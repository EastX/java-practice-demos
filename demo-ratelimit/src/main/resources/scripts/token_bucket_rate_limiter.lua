-- 令牌桶限流

-- 限流 key ， hash 保存限流相关信息
local key = KEYS[1];
-- 最大访问量
local capacity = tonumber(ARGV[1]);
-- 限流时长（毫秒）
local ttl = tonumber(ARGV[2]);
-- 当前时间戳（毫秒）
local now = tonumber(ARGV[3]);
-- 生成令牌速率（每毫秒）
local rate = tonumber(ARGV[4]);

-- 限流信息
local info = redis.call("HMGET", key, "last_time", "stored_tokens");
-- 上次处理时间
local last_time = tonumber(info[1]);
-- 令牌数量，默认为最大访问量，存在保存值使用保存值
local stored_tokens = tonumber(info[2]);
if (stored_tokens == nil) then
    stored_tokens = capacity;
end

if (last_time ~= nil) then
    -- 根据上次处理时间和当前时间差，触发式往桶里添加令牌
    local add_tokens = math.floor((now - last_time) * rate);
    stored_tokens = math.min(add_tokens + stored_tokens, capacity);
    if (add_tokens > 0) then
        last_time = nil;
    end
end

-- 首次访问、添加了令牌 设置上次处理时间
if (last_time == nil) then
    redis.call("HSET", key, "last_time", now);
end

-- 被限流返回0，未被限流返回1
local res = 0;
if (stored_tokens > 0) then
    redis.call("HSET", key, "stored_tokens", stored_tokens - 1);
    res = 1;
end

redis.call("PEXPIRE", key, ttl);
return res;
