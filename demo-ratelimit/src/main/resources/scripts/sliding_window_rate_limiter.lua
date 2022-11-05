-- 滑动窗口限流

-- 限流 key ， zset 保存未被限流的 id 与时间戳
local key = KEYS[1];
-- 最大访问量
local capacity = tonumber(ARGV[1]);
-- 限流时长（毫秒）
local ttl = tonumber(ARGV[2]);
-- 当前时间戳（毫秒）
local now = tonumber(ARGV[3]);
-- 唯一ID
local ukid = ARGV[4];

-- 清除过期的数据
redis.call('ZREMRANGEBYSCORE', key, 0, now - ttl);

local count = redis.call('ZCARD', key);
local res = 0;
if (count < capacity) then
    -- 往 zset 中添加一个值、得分均为当前时间戳的元素，[value,score]
    redis.call("ZADD", key, now, ukid);
    -- 重置 zset 的过期时间，单位毫秒
    redis.call("PEXPIRE", key, ttl);
    res = 1;
end

-- 被限流返回0，未被限流返回1
return res;
