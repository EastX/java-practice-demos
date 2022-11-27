--- Redis 解锁脚本（简单实现）

local lock_key = KEYS[1];
local lock_flag = ARGV[1];

--- 判断锁定的唯一标识与参数一致删除锁
--- 返回值：1=解锁成功（删除成功），0=锁已失效或删除失败，-1=非自己的锁不支持解锁
local val = redis.call('GET', lock_key);
if (not val) then
    return 0;
elseif (val == lock_flag) then
    return redis.call('DEL', lock_key);
else
    return -1;
end
