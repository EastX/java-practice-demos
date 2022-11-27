package cn.eastx.practice.demo.lock.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metric;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis工具类
 *  参考：https://github.com/iyayu/RedisUtil
 *
 * @author EastX
 * @date 2022/10/20
 */
@Slf4j
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisUtil(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /* ------------------------- key 相关 ------------------------- */
    /**
     * 删除 key
     *
     * @param key 缓存 key
     * @return 是否删除成功
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除 key
     *
     * @param keys 缓存 key 集合
     * @return 删除的 key 数量，使用 管道/事务 返回null
     */
    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }

    /**
     * 是否存在 key
     *
     * @param key 缓存 key
     * @return 是否存在 key
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     *
     * @param key 缓存 key
     * @param duration 过期时间
     * @return 是否设置成功，使用 管道/事务 返回null
     */
    public Boolean expire(String key, Duration duration) {
        return redisTemplate.expire(key, duration);
    }

    /**
     * 返回 key 的剩余的过期时间，单位秒
     *
     * @param key 缓存 key
     * @return 剩余过期时间，使用 管道/事务 返回null
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 移除 key 的过期时间，key 将持久保持
     *
     * @param key 缓存 key
     * @return 是否设置成功，使用 管道/事务 返回null
     */
    public Boolean persist(String key) {
        return redisTemplate.persist(key);
    }

    /* ------------------------- string 相关 ------------------------- */
    /**
     * 设置 key 的值及过期时间
     *
     * @param key 缓存 key
     * @param value 缓存值
     * @param timeout 缓存时长
     */
    public void setEx(String key, Object value, Duration timeout) {
        redisTemplate.opsForValue().set(key, value, timeout);
    }

    /**
     * 设置 key 的值及过期时间（key 不存在时）
     *
     * @param key 缓存 key
     * @param value 缓存值
     * @param timeout 缓存时长
     * @return 是否设置成功，使用 管道/事务 返回null
     */
    public Boolean setIfAbsent(String key, Object value, Duration timeout) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout);
    }

    /**
     * 获取指定 key 的值
     *
     * @param key 缓存 key
     * @return 值对象，使用 管道/事务 返回null
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 批量获取
     *
     * @param keys 缓存 key 集合
     * @return 值集合，使用 管道/事务 返回null
     */
    public List<Object> multiGet(Collection<String> keys) {
        return redisTemplate.opsForValue().multiGet(keys);
    }

    /**
     * 对 key 所储存的字符串值，设置指定偏移量上的位(bit)
     *
     * @param key 缓存 key
     * @param offset 偏移量
     * @param value bit值，1=true，0=false
     * @return 是否设置成功，使用 管道/事务 返回null
     */
    public Boolean setBit(String key, long offset, boolean value) {
        return redisTemplate.opsForValue().setBit(key, offset, value);
    }

    /**
     * 对 key 所储存的字符串值，获取指定偏移量上的位(bit)
     *
     * @param key 缓存 key
     * @param offset 偏移量
     * @return bit值，1=true，0=false，使用 管道/事务 返回null
     */
    public Boolean getBit(String key, long offset) {
        return redisTemplate.opsForValue().getBit(key, offset);
    }

    /**
     * 增加(自增长), 负数则为自减
     *
     * @param key 缓存 key
     * @param delta 增量
     * @return 增长后的数量，使用 管道/事务 返回null
     */
    public Long incrBy(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 追加到末尾
     *
     * @param key 缓存 key
     * @param value 值
     * @return 追加指定值之后，key 中字符串的长度，使用 管道/事务 返回null
     */
    public Integer append(String key, String value) {
        return redisTemplate.opsForValue().append(key, value);
    }

    /* ------------------------- hash 相关 ------------------------- */
    /**
     * 获取存储在哈希表中指定字段的值
     *
     * @param key 缓存 key
     * @param field 字段
     * @return 值，使用 管道/事务 返回null
     */
    public Object hGet(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    /**
     * 获取所有给定字段的值
     *
     * @param key 缓存 key
     * @return 所有字段与值对应Map，使用 管道/事务 返回null
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 设置指定字段的值
     *
     * @param key 缓存 key
     * @param hashKey 字段
     * @param value 值
     */
    public void hPut(String key, String hashKey, String value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * 设置指定字段的值（hashKey 不存在时）
     *
     * @param key 缓存 key
     * @param hashKey 字段
     * @param value 值
     * @return 是否设置成功，使用 管道/事务 返回null
     */
    public Boolean hPutIfAbsent(String key, String hashKey, String value) {
        return redisTemplate.opsForHash().putIfAbsent(key, hashKey, value);
    }

    /**
     * 设置多个字段的值
     *
     * @param key 缓存 key
     * @param hMap 字段与值对应Map
     */
    public void hPutAll(String key, Map<String, Object> hMap) {
        redisTemplate.opsForHash().putAll(key, hMap);
    }

    /**
     * 删除一个或多个哈希表字段
     *
     * @param key 缓存 key
     * @param fields 字段数组
     * @return 是否删除成功，使用 管道/事务 返回null
     */
    public Long hDelete(String key, Object... fields) {
        return redisTemplate.opsForHash().delete(key, fields);
    }

    /**
     * 查看哈希表 key 中，指定的字段是否存在
     *
     * @param key 缓存 key
     * @param field 字段
     * @return 是否存在，使用 管道/事务 返回null
     */
    public Boolean hHasKey(String key, String field) {
        return redisTemplate.opsForHash().hasKey(key, field);
    }

    /**
     * 为哈希表 key 中的指定字段的整数值加上增量，负数为减
     *
     * @param key 缓存 key
     * @param field 字段
     * @param delta 增量
     * @return 增长后的数量，使用 管道/事务 返回null
     */
    public Long hIncrBy(String key, Object field, long delta) {
        return redisTemplate.opsForHash().increment(key, field, delta);
    }

    /**
     * 获取哈希表中字段的数量
     *
     * @param key 缓存 key
     * @return 字段数量，使用 管道/事务 返回null
     */
    public Long hSize(String key) {
        return redisTemplate.opsForHash().size(key);
    }

    /* ------------------------- list 相关 ------------------------- */
    /**
     * 通过索引获取列表中的元素
     *
     * @param key 缓存 key
     * @param index 索引
     * @return 元素，使用 管道/事务 返回null
     */
    public Object lIndex(String key, long index) {
        return redisTemplate.opsForList().index(key, index);
    }

    /**
     * 获取列表指定范围内的元素
     *
     * @param key 缓存 key
     * @param start 开始位置, 0是开始位置
     * @param end 结束位置, -1返回所有
     * @return 元素集合，使用 管道/事务 返回null
     */
    public List<Object> lRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 存储在 list 头部
     *
     * @param key 缓存 key
     * @param value 元素值
     * @return 存储后列表的长度，使用 管道/事务 返回null
     */
    public Long lLeftPush(String key, String value) {
        return redisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * 存储在 list 头部（多个元素）
     *
     * @param key 缓存 key
     * @param values 元素值数组
     * @return 存储后列表的长度，使用 管道/事务 返回null
     */
    public Long lLeftPushAll(String key, String... values) {
        return redisTemplate.opsForList().leftPushAll(key, values);
    }

    /**
     * 存储在 list 头部（list 存在时）
     *
     * @param key 缓存 key
     * @param value 元素值
     * @return 存储后列表的长度，使用 管道/事务 返回null
     */
    public Long lLeftPushIfPresent(String key, String value) {
        return redisTemplate.opsForList().leftPushIfPresent(key, value);
    }

    /**
     * 如果 pivot 存在，在 pivot 前面添加
     *
     * @param key 缓存 key
     * @param pivot 支点
     * @param value 元素值
     * @return 存储后列表的长度，使用 管道/事务 返回null
     */
    public Long lLeftPush(String key, String pivot, String value) {
        return redisTemplate.opsForList().leftPush(key, pivot, value);
    }

    /**
     * 存储在 list 尾部
     *
     * @param key 缓存 key
     * @param value 元素值
     * @return 存储后列表的长度，使用 管道/事务 返回null
     */
    public Long lRightPush(String key, String value) {
        return redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * 存储在 list 尾部（多个元素）
     *
     * @param key 缓存 key
     * @param values 元素值数组
     * @return 存储后列表的长度，使用 管道/事务 返回null
     */
    public Long lRightPushAll(String key, String... values) {
        return redisTemplate.opsForList().rightPushAll(key, values);
    }

    /**
     * 存储在 list 头部（list 存在时）
     *
     * @param key 缓存 key
     * @param value 元素值
     * @return 存储后列表的长度，使用 管道/事务 返回null
     */
    public Long lRightPushIfPresent(String key, String value) {
        return redisTemplate.opsForList().rightPushIfPresent(key, value);
    }

    /**
     * 如果 pivot 存在，在 pivot 后面添加
     *
     * @param key 缓存 key
     * @param pivot 支点
     * @param value 元素值
     * @return 存储后列表的长度，使用 管道/事务 返回null
     */
    public Long lRightPush(String key, String pivot, String value) {
        return redisTemplate.opsForList().rightPush(key, pivot, value);
    }

    /**
     * 通过索引设置列表元素的值
     *
     * @param key 缓存 key
     * @param index 索引
     * @param value 元素值
     */
    public void lSet(String key, long index, String value) {
        redisTemplate.opsForList().set(key, index, value);
    }

    /**
     * 移出并获取列表的第一个元素
     *
     * @param key 缓存 key
     * @return 删除的元素
     */
    public Object lLeftPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    /**
     * 移除并获取列表最后一个元素
     *
     * @param key 缓存 key
     * @return 移除的元素
     */
    public Object lRightPop(String key) {
        return redisTemplate.opsForList().rightPop(key);
    }

    /**
     * 移出并获取列表的第一个元素，如果列表没有元素会阻塞列表直到等待超时或发现可弹出元素为止
     *
     * @param key 缓存 key
     * @param timeout 等待时长
     * @return 移除的元素
     */
    public Object lLeftPop(String key, Duration timeout) {
        return redisTemplate.opsForList().leftPop(key, timeout);
    }

    /**
     * 移出并获取列表的最后一个元素， 如果列表没有元素会阻塞列表直到等待超时或发现可弹出元素为止
     *
     * @param key 缓存 key
     * @param timeout 等待时长
     * @return
     */
    public Object lRightPop(String key, Duration timeout) {
        return redisTemplate.opsForList().rightPop(key, timeout);
    }

    /**
     * 删除集合中值等于 value 得元素
     *
     * @param key 缓存 key
     * @param index 索引
     *              index=0, 删除所有值等于value的元素; index>0, 从头部开始删除第一个值等于value的元素;
     *              index<0, 从尾部开始删除第一个值等于value的元素;
     * @param value 元素值
     * @return 被移除元素的数量，列表不存在时返回 0 ，使用 管道/事务 返回null
     */
    public Long lRemove(String key, long index, String value) {
        return redisTemplate.opsForList().remove(key, index, value);
    }

    /**
     * 裁剪list
     *
     * @param key 缓存 key
     * @param start 开始索引
     * @param end 结束索引
     */
    public void lTrim(String key, long start, long end) {
        redisTemplate.opsForList().trim(key, start, end);
    }

    /**
     * 获取列表长度
     *
     * @param key 缓存 key
     * @return 列表长度，使用 管道/事务 返回null
     */
    public Long lLen(String key) {
        return redisTemplate.opsForList().size(key);
    }

    /* ------------------------- set 相关 ------------------------- */
    /**
     * set添加元素
     *
     * @param key 缓存 key
     * @param values 元素值数组
     * @return 被添加到集合中的新元素的数量，不包括被忽略的元素，使用 管道/事务 返回null
     */
    public Long sAdd(String key, String... values) {
        return redisTemplate.opsForSet().add(key, values);
    }

    /**
     * set移除元素
     *
     * @param key 缓存 key
     * @param values 元素值数组
     * @return 被成功移除的元素的数量，不包括被忽略的元素，使用 管道/事务 返回null
     */
    public Long sRemove(String key, Object... values) {
        return redisTemplate.opsForSet().remove(key, values);
    }

    /**
     * 移除并返回集合的一个随机元素
     *
     * @param key 缓存 key
     * @return 元素值，使用 管道/事务 返回null
     */
    public Object sPop(String key) {
        return redisTemplate.opsForSet().pop(key);
    }

    /**
     * 获取集合的大小
     *
     * @param key 缓存 key
     * @return 集合大小，使用 管道/事务 返回null
     */
    public Long sSize(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    /**
     * 判断集合是否包含元素
     *
     * @param key 缓存 key
     * @param value 元素值
     * @return 是否包含元素，使用 管道/事务 返回null
     */
    public Boolean sIsMember(String key, Object value) {
        return redisTemplate.opsForSet().isMember(key, value);
    }

    /**
     * 获取多个 key 的交集
     *
     * @param keys 缓存 key 集合
     * @return 交集成员的列表，使用 管道/事务 返回null
     */
    public Set<Object> sIntersect(Collection<String> keys) {
        return redisTemplate.opsForSet().intersect(keys);
    }

    /**
     * 多个 key 的交集存储到 destKey 中
     *
     * @param keys 缓存 key 集合
     * @param destKey 存储交集的缓存 key
     * @return 存储交集的集合的元素数量，使用 管道/事务 返回null
     */
    public Long sIntersectAndStore(Collection<String> keys, String destKey) {
        return redisTemplate.opsForSet().intersectAndStore(keys, destKey);
    }

    /**
     * 获取多个 key 的并集
     *
     * @param keys 缓存 key 集合
     * @return 并集成员的列表，使用 管道/事务 返回null
     */
    public Set<Object> sUnion(Collection<String> keys) {
        return redisTemplate.opsForSet().union(keys);
    }

    /**
     * 多个 key 的并集存储到 destKey 中
     *
     * @param keys 缓存 key 集合
     * @param destKey 存储并集的缓存 key
     * @return 存储并集的集合的元素数量，使用 管道/事务 返回null
     */
    public Long sUnionAndStore(Collection<String> keys, String destKey) {
        return redisTemplate.opsForSet().unionAndStore(keys, destKey);
    }

    /**
     * 获取多个 key 的差集
     *
     * @param keys 缓存 key 集合
     * @return 差集成员的列表，使用 管道/事务 返回null
     */
    public Set<Object> sDifference(Collection<String> keys) {
        return redisTemplate.opsForSet().difference(keys);
    }

    /**
     * 多个 key 的差集存储到 destKey 中
     *
     * @param keys 缓存 key 集合
     * @param destKey 存储差集的缓存 key
     * @return 存储差集的集合的元素数量，使用 管道/事务 返回null
     */
    public Long sDifferenceAndStore(Collection<String> keys, String destKey) {
        return redisTemplate.opsForSet().differenceAndStore(keys, destKey);
    }

    /**
     * 获取集合所有元素
     *
     * @param key 缓存 key
     * @return 元素集合，使用 管道/事务 返回null
     */
    public Set<Object> setMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 随机获取集合中的一个元素
     *
     * @param key 缓存 key
     * @return 元素，使用 管道/事务 返回null
     */
    public Object sRandomMember(String key) {
        return redisTemplate.opsForSet().randomMember(key);
    }

    /**
     * 随机获取集合中多个元素
     *
     * @param key 缓存 key
     * @param count 元素数量
     * @return 元素列表，set 不存在返回 null，使用 管道/事务 返回null
     */
    public List<Object> sRandomMembers(String key, long count) {
        return redisTemplate.opsForSet().randomMembers(key, count);
    }

    /* ------------------------- sorted set 相关 ------------------------- */
    /**
     * 添加元素
     *
     * @param key 缓存 key
     * @param value 元素值
     * @param score 分数
     * @return 是否添加成功，使用 管道/事务 返回null
     */
    public Boolean zAdd(String key, String value, double score) {
        return redisTemplate.opsForZSet().add(key, value, score);
    }

    /**
     * 删除元素
     *
     * @param key 缓存 key
     * @param values 元素值数组
     * @return 被成功移除的成员的数量，不包括被忽略的成员，使用 管道/事务 返回null
     */
    public Long zRemove(String key, Object... values) {
        return redisTemplate.opsForZSet().remove(key, values);
    }

    /**
     * 增加元素的分数
     *
     * @param key 缓存 key
     * @param value 元素值
     * @param delta 增量
     * @return 增加后的值，使用 管道/事务 返回null
     */
    public Double zIncrementScore(String key, String value, double delta) {
        return redisTemplate.opsForZSet().incrementScore(key, value, delta);
    }

    /**
     * 返回元素在集合的排名，按元素的 score 值由小到大排列
     *
     * @param key 缓存 key
     * @param value 元素值
     * @return 元素排名，0表示第一位，使用 管道/事务 返回null
     */
    public Long zRank(String key, Object value) {
        return redisTemplate.opsForZSet().rank(key, value);
    }

    /**
     * 返回元素在集合的排名，按元素的 score 值由大到小排列
     *
     * @param key 缓存 key
     * @param value 元素值
     * @return 元素排名，0表示第一位，使用 管道/事务 返回null
     */
    public Long zReverseRank(String key, Object value) {
        return redisTemplate.opsForZSet().reverseRank(key, value);
    }

    /**
     * 获取指定索引区间的元素集合，从小到大排序
     *
     * @param key 缓存 key
     * @param start 开始位置
     * @param end 结束位置, -1查询所有
     * @return 指定索引区间的元素集合，使用 管道/事务 返回null
     */
    public Set<Object> zRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().range(key, start, end);
    }

    /**
     * 获取指定索引区间的元素+分数集合，从小到大排序
     *
     * @param key 缓存 key
     * @param start 开始位置
     * @param end 结束位置, -1查询所有
     * @return 指定索引区间的元素+分数集合，使用 管道/事务 返回null
     */
    public Set<ZSetOperations.TypedTuple<Object>> zRangeWithScores(String key, long start,
                                                                   long end) {
        return redisTemplate.opsForZSet().rangeWithScores(key, start, end);
    }

    /**
     * 获取指定分数区间的元素集合，从小到大排序
     *
     * @param key 缓存 key
     * @param min 开始分数
     * @param max 结束分数
     * @return 指定分数区间的元素集合，使用 管道/事务 返回null
     */
    public Set<Object> zRangeByScore(String key, double min, double max) {
        return redisTemplate.opsForZSet().rangeByScore(key, min, max);
    }

    /**
     * 获取指定分数区间的元素+分数集合，从小到大排序
     *
     * @param key 缓存 key
     * @param min 开始分数
     * @param max 结束分数
     * @return 指定分数区间的元素+分数集合，使用 管道/事务 返回null
     */
    public Set<ZSetOperations.TypedTuple<Object>> zRangeByScoreWithScores(String key, double min,
                                                                          double max) {
        return redisTemplate.opsForZSet().rangeByScoreWithScores(key, min, max);
    }

    /**
     * 获取指定索引区间的元素集合，从大到小排序
     *
     * @param key 缓存 key
     * @param start 开始位置
     * @param end 结束位置, -1查询所有
     * @return 指定索引区间的元素集合，使用 管道/事务 返回null
     */
    public Set<Object> zReverseRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    /**
     * 获取指定索引区间的元素+分数集合，从大到小排序
     *
     * @param key 缓存 key
     * @param start 开始位置
     * @param end 结束位置, -1查询所有
     * @return 指定索引区间的元素+分数集合，使用 管道/事务 返回null
     */
    public Set<ZSetOperations.TypedTuple<Object>> zReverseRangeWithScores(String key, long start,
                                                                          long end) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
    }

    /**
     * 获取指定分数区间的元素集合，从大到小排序
     *
     * @param key 缓存 key
     * @param min 开始分数
     * @param max 结束分数
     * @return 指定分数区间的元素集合，使用 管道/事务 返回null
     */
    public Set<Object> zReverseRangeByScore(String key, double min, double max) {
        return redisTemplate.opsForZSet().reverseRangeByScore(key, min, max);
    }

    /**
     * 获取指定分数区间的元素+分数集合，从大到小排序
     *
     * @param key 缓存 key
     * @param min 开始分数
     * @param max 结束分数
     * @return 指定分数区间的元素+分数集合，使用 管道/事务 返回null
     */
    public Set<ZSetOperations.TypedTuple<Object>> zReverseRangeByScoreWithScores(String key,
                                                                                 double min,
                                                                                 double max) {
        return redisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, min, max);
    }

    /**
     * 根据分数值区间获取集合元素数量
     *
     * @param key 缓存 key
     * @param min 开始分数
     * @param max 结束分数
     * @return 元素数量，使用 管道/事务 返回null
     */
    public Long zCount(String key, double min, double max) {
        return redisTemplate.opsForZSet().count(key, min, max);
    }

    /**
     * 获取集合大小
     *
     * @param key 缓存 key
     * @return 集合大小，使用 管道/事务 返回null
     */
    public Long zCard(String key) {
        return redisTemplate.opsForZSet().zCard(key);
    }

    /**
     * 获取集合中元素的分数值
     *
     * @param key 缓存 key
     * @param value 元素值
     * @return 分数值，使用 管道/事务 返回null
     */
    public Double zScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    /**
     * 移除指定索引区间的成员
     *
     * @param key 缓存 key
     * @param start 开始位置
     * @param end 结束位置, -1=所有
     * @return 被成功移除的成员的数量，不包括被忽略的成员，使用 管道/事务 返回null
     */
    public Long zRemoveRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().removeRange(key, start, end);
    }

    /**
     * 根据指定的score值的范围来移除成员
     *
     * @param key 缓存 key
     * @param min 开始分数
     * @param max 结束分数
     * @return 被成功移除的成员的数量，不包括被忽略的成员，使用 管道/事务 返回null
     */
    public Long zRemoveRangeByScore(String key, double min, double max) {
        return redisTemplate.opsForZSet().removeRangeByScore(key, min, max);
    }

    /**
     * 获取两个集合的并集并存储到另一个 key 中
     *
     * @param key 缓存 key
     * @param otherKey 另一个缓存 key
     * @param destKey 存储 key
     * @return 存储的结果集合大小，使用 管道/事务 返回null
     */
    public Long zUnionAndStore(String key, String otherKey, String destKey) {
        return redisTemplate.opsForZSet().unionAndStore(key, otherKey, destKey);
    }

    /**
     * 获取多个集合的并集并存储到另一个 key 中
     *
     * @param key 缓存 key
     * @param otherKeys 其它缓存 key 集合
     * @param destKey 存储 key
     * @return 存储的结果集合大小，使用 管道/事务 返回null
     */
    public Long zUnionAndStore(String key, Collection<String> otherKeys, String destKey) {
        return redisTemplate.opsForZSet().unionAndStore(key, otherKeys, destKey);
    }

    /**
     * 获取两个集合的交集并存储到另一个 key 中
     *
     * @param key 缓存 key
     * @param otherKey 另一个缓存 key
     * @param destKey 存储 key
     * @return 存储的结果集合大小，使用 管道/事务 返回null
     */
    public Long zIntersectAndStore(String key, String otherKey, String destKey) {
        return redisTemplate.opsForZSet().intersectAndStore(key, otherKey, destKey);
    }

    /**
     * 获取多个集合的交集并存储到另一个 key 中
     *
     * @param key 缓存 key
     * @param otherKeys 其它缓存 key 集合
     * @param destKey 存储 key
     * @return 存储的结果集合大小，使用 管道/事务 返回null
     */
    public Long zIntersectAndStore(String key, Collection<String> otherKeys, String destKey) {
        return redisTemplate.opsForZSet().intersectAndStore(key, otherKeys, destKey);
    }

    /* ------------------------- HyperLogLog 相关 ------------------------- */
    /**
     * 添加指定元素到 HyperLogLog 中
     *
     * @param key 缓存 key
     * @param values 元素值数组
     * @return 如果至少有个元素被添加返回 1， 否则返回 0，使用 管道/事务 返回null
     */
    public Long pfAdd(String key, Object... values) {
        return redisTemplate.opsForHyperLogLog().add(key, values);
    }

    /**
     * 获取 HyperLogLog 基数估算值
     *
     * @param keys 缓存 key 数组
     * @return 基数估算值，使用 管道/事务 返回null
     */
    public Long pfCount(String... keys) {
        return redisTemplate.opsForHyperLogLog().size(keys);
    }

    /**
     * HyperLogLog 合并
     *
     * @param destKey 目标 key
     * @return 合并后的基数估算值，使用 管道/事务 返回null
     */
    public Long pfUnion(String destKey, String... sourceKeys) {
        return redisTemplate.opsForHyperLogLog().union(destKey, sourceKeys);
    }

    /* ------------------------- GEO 相关 ------------------------- */
    /**
     * 存储指定的地理空间位置
     *
     * @param key 缓存 key
     * @param point 地理空间位置
     * @param member 地点名称
     * @return 如果至少有个元素被添加返回 1， 否则返回 0，使用 管道/事务 返回null
     */
    public Long geoAdd(String key, Point point, String member) {
        return redisTemplate.opsForGeo().add(key, point, member);
    }

    /**
     * 存储多个地理空间位置
     *
     * @param key 缓存 key
     * @param memberMap 多个地理空间位置
     * @return 如果至少有个元素被添加返回 1， 否则返回 0，使用 管道/事务 返回null
     */
    public Long geoAdd(String key, Map<Object, Point> memberMap) {
        return redisTemplate.opsForGeo().add(key, memberMap);
    }

    /**
     * 获取多个地点的地理空间位置
     *
     * @param key 缓存 key
     * @param members 地点名称数组
     * @return 地理空间位置集合，使用 管道/事务 返回null
     */
    public List<Point> geoPosition(String key, String members) {
        return redisTemplate.opsForGeo().position(key, members);
    }

    /**
     * 获取两个给定位置之间的距离
     *
     * @param key 缓存 key
     * @param member1 地点1
     * @param member2 地点2
     * @return 两个位置之间的距离
     */
    public Distance geoDistance(String key, String member1, String member2) {
        return redisTemplate.opsForGeo().distance(key, member1, member2);
    }

    /**
     * 获取两个给定位置之间的距离
     *
     * @param key 缓存 key
     * @param member1 地点1
     * @param member2 地点2
     * @param metric 度量单位
     * @return 两个位置之间的距离
     */
    public Distance getDistance(String key, String member1, String member2, Metric metric) {
        return redisTemplate.opsForGeo().distance(key, member1, member2, metric);
    }

    /**
     * 给定的经纬度为中心，返回键包含的位置元素当中，与中心的距离不超过给定最大距离的所有位置元素。
     *
     * @param key 缓存 key
     * @param member 地点标识
     * @param radius 半径，单位米
     * @return 地点列表，使用 管道/事务 返回null
     */
    public GeoResults<RedisGeoCommands.GeoLocation<Object>> geoRadius(String key, String member,
                                                                      double radius) {
        return redisTemplate.opsForGeo().radius(key, member, radius);
    }

    /**
     * 获取一个或多个位置元素的 geohash 值
     *
     * @param key 缓存 key
     * @param members 地点名称数组
     * @return geohash 值集合，使用 管道/事务 返回null
     */
    public List<String> geoHash(String key, String... members) {
        return redisTemplate.opsForGeo().hash(key, members);
    }

    /* ------------------------- 脚本相关 ------------------------- */
    /**
     * 执行 Redis 脚本
     *
     * @param script Redis 脚本
     * @param keys 缓存 key 集合
     * @param args 参数集合
     * @param <T> 返回值类型
     * @return 执行结果
     */
    public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
        return redisTemplate.execute(script, keys, args);
    }

}
