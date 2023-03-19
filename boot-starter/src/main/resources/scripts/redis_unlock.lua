--- Redis 解锁脚本

local lock_key = KEYS[1];
local lock_flag = ARGV[1];

--- HASH 支持可重入
--- lock_flag 保存加锁唯一标识
--- lock_num 保存加锁次数
local info = redis.call("HMGET", lock_key, "lock_flag", "lock_num");
local h_flag = info[1];
local h_num = tonumber(info[2]);
if (h_num == nil) then
    h_num = 0;
end

--- 返回剩余加锁次数，未被加锁或解锁完返回 0，非自己加锁返回 -1
if (not h_flag) then
    return 0;
elseif (h_flag == lock_flag) then
    if (h_num <= 0) then
        redis.call("DEL", lock_key);
        return 0;
    else
        local res_num = h_num - 1;
        redis.call("HMSET", lock_key, "lock_flag", lock_flag, "lock_num", res_num);
        return res_num;
    end
else
    return -1;
end
