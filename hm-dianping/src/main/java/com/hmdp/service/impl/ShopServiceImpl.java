package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
//        缓存穿透
//        Shop shop = queryWithPassThrough(id);
//        Shop shop = cacheClient.getWithPassThrough(RedisConstants.CACHE_SHOP_KEY, id, Shop.class,
//                this::getById, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);


        //用互斥锁解决缓存击穿
        //Shop shop = queryWithPMutex(id);

        //逻辑过期解决缓存击穿
        //Shop shop = queryWithLogicalExpire(id);
        Shop shop = cacheClient.getWithLogicalExpire(RedisConstants.CACHE_SHOP_KEY, id, Shop.class,this::getById,
                RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        if(null  == shop){
            return Result.fail("店铺不存在！");
        }
        return Result.ok(shop);

    }

    //缓存重建的执行线程
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    //逻辑过期解决缓存击穿
    public Shop queryWithLogicalExpire(Long id){

        //1.从redis里查询店铺缓存
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在
        if (StrUtil.isBlank(json)) {
            //3.不存在，直接返回
            return null;
        }

        //4。命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        JSONObject data =(JSONObject) redisData.getData();
        Shop shop = JSONUtil.toBean(data, Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        //5.判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            //5.1未过期，直接返回店铺信息
            return shop;
        }

        //5.2已过期，需要缓存重建
        //6缓存重建
        //6.1获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY+id;
        boolean isLock = tryLock(lockKey);
        //6.2判断是否获取锁成功
        if(isLock){
            //6.3成功,开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    //重建缓存
                    saveShop2Redis(id,20L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                finally {
                    //释放锁
                    unlock(lockKey);
                }
            });
        }

        //6.4失败，返回过期数据
        return shop;
    }

    //互斥锁解决缓存击穿
    public Shop queryWithPMutex(Long id){
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        //1.从redis里查询店铺缓存

        String json = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在
        if (StrUtil.isNotBlank(json)) {
            //3.存在，直接返回
            return JSONUtil.toBean(json, Shop.class);
        }

        //穿透：判断命中的是否是空值 ps: ""空字符串不是null
        if("".equals(json)|| json != null){
            return null;
        }

        //4.实现缓存重建
        //4.1获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY+id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            //4.2判断是否成功
            if (!isLock) {
                //4.3失败，则休眠并重试
                Thread.sleep(50);
                //递归重试
                return queryWithPMutex(id);
            }

            //4.4成功，根据id查询数据库
            shop = getById(id);
            //模拟重建的延时
            Thread.sleep(200);

            //穿透:5不存在,返回错误
            if(null ==  shop){
                //将空值写入redis
                stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }

            //6.存在，写入Redis
            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            //7.释放互斥锁
            unlock(lockKey);
        }

        return shop;
    }


    //解决缓存穿透
    public Shop queryWithPassThrough(Long id){

        //1.从redis里查询店铺缓存
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在
        if (StrUtil.isNotBlank(json)) {
            //3.存在，直接返回
            Shop shop = JSONUtil.toBean(json, Shop.class);
            return shop;
        }

        //穿透：判断命中的是否是空值 ps: ""空字符串不是null
        if("".equals(json)|| json != null){
            return null;
        }

        //4.不存在，根据id查询数据库
        Shop shop = getById(id);

        //穿透:5不存在,返回错误
        if(null == shop){
            //将空值写入redis
            stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }

        //6.存在，写入Redis
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);

        return shop;
    }


    //尝试加锁
    private boolean tryLock(String key){
        //如果不存在
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    //释放锁
    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }

    public void saveShop2Redis(Long id,Long expireSeconds) throws InterruptedException {
        //查询店铺数据
        Shop shop = getById(id);
        Thread.sleep(200);
        //封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        redisData.setData(shop);

        //写入Redis
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }

    @Override
    @Transactional(rollbackFor= Exception.class)
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(null == id){
            return Result.fail("店铺id不能为空");
        }
        //1.更新数据库
        updateById(shop);

        //2.删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY+id);

        return Result.ok();
    }
}
