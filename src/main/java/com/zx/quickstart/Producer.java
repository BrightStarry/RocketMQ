package com.zx.quickstart;

import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.alibaba.rocketmq.client.producer.MessageQueueSelector;
import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.common.message.Message;
import com.alibaba.rocketmq.common.message.MessageQueue;

import java.util.List;

/**
 * 生产者
 */
public class Producer {

    public static void main(String[] args) throws Exception {
        //创建生产者,参数字符就是 Producer Group 名。
        DefaultMQProducer producer = new DefaultMQProducer("zx_producer");
        //设置地址
        producer.setNamesrvAddr("192.168.2.104:9876;192.168.2.105:9876");

        //启动
        producer.start();

        Message message = null;
        for (int i = 0; i < 5; i++) {
            //新建消息
            message = new Message(
                    "zx",//topic
                    "zxA",//tag,可以用来过滤
                    "key" + i,//key 去重
                    ("helloworld" + i).getBytes() //body
            );
//            SendResult result = producer.send(message);
//            System.out.println(result);

            /**
             * 如果要顺序消费，需要使用MessageQueueSelector，保证消息进入同一个队列
             */
            producer.send(message, new MessageQueueSelector() {
                public MessageQueue select(List<MessageQueue> list, Message message, Object o) {
                    Integer id =(Integer)o;
                    return list.get(id);
                }
            },0);//0是队列的下标,这个是上面select()方法中的Object o

        }

        producer.shutdown();

    }
}
