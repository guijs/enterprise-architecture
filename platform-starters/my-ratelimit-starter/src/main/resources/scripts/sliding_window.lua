-- 滑动窗口限流：member 必须唯一，否则同一毫秒多个请求会因 member 相同互相覆盖
-- KEYS[1]=限流key  ARGV[1]=now(ms)  ARGV[2]=window(ms)  ARGV[3]=limit  ARGV[4]=member
local key = KEYS[1]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])
local member = ARGV[4]
redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
local count = redis.call('ZCARD', key)
if count < limit then
    redis.call('ZADD', key, now, member)
    redis.call('PEXPIRE', key, window)
    return 1
end
return 0
