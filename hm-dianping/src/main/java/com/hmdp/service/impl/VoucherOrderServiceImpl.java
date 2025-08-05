package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
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

    @Override
    @Transactional
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
        Long userId = UserHolder.getUser().getId();
        order.setUserId(userId);
        //代金券id
        order.setVoucherId(seckillVoucher.getVoucherId());
        save(order);

        //返回订单id
        return Result.ok(orderId);


    }
}
