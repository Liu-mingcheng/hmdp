package com.hmdp.service.impl;

import cn.hutool.db.ds.simple.AbstractDataSource;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisIdWorker redisIdWorker;

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result seckillVoucher(Long voucherId) {

        //1.查询优惠券
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);

        //2.判断秒杀是否开始
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀还未开始");

        }

        //3.判断秒杀是否已经结束
        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已经结束");
        }

        //4.判断库存是否充足
        if(seckillVoucher.getStock() <1){
            return Result.fail("库存不足!");
        }

        Long userId = UserHolder.getUser().getId();
        //由于toString的源码是new String，所以如果我们只用userId.toString()拿到的也不是同一个对象地址虽然值相同，
        // 需要使用intern()把字符串加入常量池，如果字符串常量池中已经包含了一个等于这个string对象的字符串（由equals（object）方法确定），
        // 那么将返回池中的字符串。否则，将此String对象添加到池中，并返回对此String对象的引用。
//        synchronized (userId.toString().intern()){
        //获取代理对象（事务）
//            IVoucherOrderService proxy =(IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId, seckillVoucher);
//        }

        //创建锁对象，避免集群下同一个用户秒杀了两单优惠券
        SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        //获取锁
        boolean isLock = lock.tryLock(1200);
        //判断是否获取锁成功
        if(!isLock){
            //获取锁失败，返回错误或重试
            return Result.fail("一个用户只允许下一单");
        }

        try {
            //获取代理对象（事务）
            IVoucherOrderService proxy =(IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId, seckillVoucher);
        } finally {
            //释放锁
            lock.unlock();
        }

    }

    @Transactional
    public Result createVoucherOrder(Long voucherId, SeckillVoucher seckillVoucher) {

        //5.一人一单
        Long userId = UserHolder.getUser().getId();

        //5.1查询订单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        //5.2判断是否存在
        if (count > 0) {
            //用户已经购买过了
            return Result.fail("用户已经购买过一次!");
        }

        //5.扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock -1")  //set stock = stock -1
                .eq("voucher_id", voucherId).gt("stock",0) //where voucher_id = ? and stock > 0
                .update();

        if(!success){
            return Result.fail("库存不足!");
        }


        //6.创建订单
        VoucherOrder order = new VoucherOrder();
        //订单id
        long orderId = redisIdWorker.nextId("order");
        order.setId(orderId);
        //用户id

        order.setUserId(userId);
        //代金券id
        order.setVoucherId(seckillVoucher.getVoucherId());
        save(order);

        //返回订单id
        return Result.ok(orderId);
    }
}
