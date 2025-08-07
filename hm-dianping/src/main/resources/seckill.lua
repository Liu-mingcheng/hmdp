--1.参数列表
--1.1优惠券id
local voucherId = ARGV[1]
--1.2用户id
local userId = ARGV[2]

--2.数据key
--2.1库存key
local stockKey = "seckill:stock:"..voucherId
--2.2订单key
local orderKey = "seckill:order:"..voucherId

--3.脚本业务
local stock = redis.call("get",stockKey)
--3.1判断库存是否充足，由于get到的是一个字符串，所以要转数字
if(tonumber(stock) <=0) then
    --3.2库存不足，返回1
    return 1
end

--3.2判断用户是否下单 也就是判断redis集合里是否有这个用户的id
if(redis.call("sismember",orderKey,userId) == 1) then
    --3.3 存在，说明重复下单
    return 2
end

--3.4扣库存
redis.call("incrby",stockKey,-1)
--3.5下单(保存用户) sadd
redis.call("sadd",orderKey,userId)
return 0



