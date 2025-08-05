package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<ShopType> queryTypeList() {
        List<String> shopTypeJson = stringRedisTemplate.opsForList().range("cache:shopType", 0, 9);
        if((shopTypeJson != null && !shopTypeJson.isEmpty())){
            List<ShopType> shopTypeList = new ArrayList<>();
            for (String item:shopTypeJson
                 ) {
                shopTypeList.add(JSONUtil.toBean(item,ShopType.class));
            }

            return shopTypeList;
        }

        shopTypeJson = new ArrayList<>();
        List<ShopType> shopTypeList = query().orderByAsc("sort").list();
        for (ShopType item:shopTypeList
        ) {
            shopTypeJson.add(JSONUtil.toJsonStr(item));
        }
        stringRedisTemplate.opsForList().rightPushAll("cache:shopType",shopTypeJson);
        return shopTypeList;
    }
}
