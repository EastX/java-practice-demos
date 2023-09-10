package cn.eastx.practice.middleware.cache;

import org.springframework.data.redis.connection.MessageListener;

/**
 * Redis 发布订阅模式 - 订阅者监听
 * 实现此接口支持发布订阅，注意中文传输可能存在问题
 *
 * 参考：https://mp.weixin.qq.com/s/bIUwmsAv1ALPiNs2AcxFPg
 *
 * @author EastX
 * @date 2023/08/19
 */
public interface RedisSubscriber extends MessageListener {

    /**
     * 通道名称
     */
    String getTopic();

}
