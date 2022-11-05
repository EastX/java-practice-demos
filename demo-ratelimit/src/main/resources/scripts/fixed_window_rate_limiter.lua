-- 固定窗口限流

-- 限流key ，string 保存调用限流的次数
local key = KEYS[1];
-- 最大访问量
local capacity = tonumber(ARGV[1]);
-- 限流时长（毫秒）
local ttl = tonumber(ARGV[2]);

local count = redis.call('INCR', key);
if (count == 1) then
    -- 首次访问设置过期时间
    redis.call('PEXPIRE', key, ttl);
end

local res = 0;
if (count <= capacity) then
    res = 1;
end

-- 被限流返回0，未被限流返回1
return res;
