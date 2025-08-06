-- --锁的key
-- local key = KEYS[1]
-- --当前线程标示
-- loacl threadId = ARGV[1]

--获取锁中的线程标示 get key
local id = redis.call("get",KEYS[1])

--比较
if(ARGV[1] == id)
then
    --一致删除锁
    return redis.call("del",KEYS[1])
end
return 0
