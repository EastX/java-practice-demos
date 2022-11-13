-- 漏桶限流

-- 限流 key ， hash 保存限流相关信息
local key = KEYS[1];
-- 最大访问量
local capacity = tonumber(ARGV[1]);
-- 限流时长（毫秒）
local ttl = tonumber(ARGV[2]);
-- 当前时间戳（毫秒）
local now = tonumber(ARGV[3]);
-- 水流出速率（每毫秒）
local rate = tonumber(ARGV[4]);

-- 限流信息
local info = redis.call("HMGET", key, "last_time", "stored_water");
-- 上次处理时间
local last_time = tonumber(info[1]);
-- 当前存储的水量，默认为0，存在保存值使用保存值
local stored_water = tonumber(info[2]);
if (stored_water == nil) then
    stored_water = 0;
end

if (last_time ~= nil) then
    -- 根据上次处理时间和当前时间差，计算流出后的水量
    local leaked_water = math.floor((now - last_time) * rate);
    stored_water = math.max(stored_water - leaked_water, 0);
    if (leaked_water > 0) then
        last_time = nil;
    end
end

-- 首次访问、泄露了水 设置上次处理时间
if (last_time == nil) then
    redis.call("HSET", key, "last_time", now);
end

-- 被限流返回0，未被限流返回1
local res = 0;
if (capacity > stored_water) then
    redis.call("HSET", key, "stored_water", stored_water + 1);
    res = 1;
end

redis.call("PEXPIRE", key, ttl);
return res;
