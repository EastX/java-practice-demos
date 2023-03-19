--- Redis 加锁脚本

local lock_key = KEYS[1];
local lock_flag = ARGV[1];
--- 锁定时长，单位：毫秒
local lock_ttl = tonumber(ARGV[2]);

--- HASH 支持可重入
--- lock_flag 保存加锁唯一标识
--- lock_num 保存加锁次数
local info = redis.call("HMGET", lock_key, "lock_flag", "lock_num");
local h_flag = info[1];
local h_num = tonumber(info[2]);
if (h_num == nil or h_num < 0) then
    h_num = 0;
end

--- 返回加锁次数，未加锁成功返回 -1
if (not h_flag or h_flag == lock_flag) then
    local res_num = h_num + 1;
    redis.call("HMSET", lock_key, "lock_flag", lock_flag, "lock_num", res_num);
    redis.call("PEXPIRE", lock_key, lock_ttl);
    return res_num;
else
    return -1;
end


