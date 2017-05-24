package com.zx.quickstart;

import com.alibaba.rocketmq.client.consumer.DefaultMQPushConsumer;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import com.alibaba.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.common.consumer.ConsumeFromWhere;
import com.alibaba.rocketmq.common.message.Message;
import com.alibaba.rocketmq.common.message.MessageExt;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * 消费者
 */
public class Consumer {

    public static void main(String[] args) throws Exception {
        //创建消费者，并指定 consumer group
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("zx_consumer");
        //设置地址
        consumer.setNamesrvAddr("192.168.2.104:9876;192.168.2.105:9876");

        /**
         * 设置Consumer第一次启动是从队列头部开始消费， 还是尾部开始消费
         * 如果不是第一次启动，那么按照上次消费的位置消费
         */
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);

        //订阅topic ，获取所有标签
        /**
         * " a || b || c" 这么写，表示接收tag是a或b或c的
         */
        consumer.subscribe("zx","*");

        //注册消息监听器,并发的
        //是批量的消费，所以消息是list

        consumer.registerMessageListener(new MessageListenerConcurrently() {
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list,
                                                            ConsumeConcurrentlyContext consumeConcurrentlyContext) {
//                System.out.println(Thread.currentThread().getName() + "接收到新的消息:" + list);

                try {
                    for (MessageExt message : list) {
                        String topic = message.getTopic();
                        String body = new String(message.getBody(),"UTF-8");
                        String tags = message.getTags();
                        System.out.println("收到消息："  + " topic:" + topic
                        + "  body:" + body + " tags:" + tags);

                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    //如果失败了，稍后处理
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;

                }


                //返回信息，消费处理状态
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        //启动
        consumer.start();
        System.out.println("consumer started.");
    }
}
