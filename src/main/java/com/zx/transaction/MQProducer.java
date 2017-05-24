package com.zx.transaction;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.alibaba.rocketmq.client.QueryResult;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.client.impl.MQAdminImpl;
import com.alibaba.rocketmq.client.producer.LocalTransactionExecuter;
import com.alibaba.rocketmq.client.producer.LocalTransactionState;
import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.client.producer.TransactionCheckListener;
import com.alibaba.rocketmq.client.producer.TransactionMQProducer;
import com.alibaba.rocketmq.client.producer.TransactionSendResult;
import com.alibaba.rocketmq.client.producer.TransactionSendResult;
import com.alibaba.rocketmq.common.message.Message;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.alibaba.rocketmq.common.message.MessageQueue;
import com.alibaba.rocketmq.remoting.RPCHook;
import com.alibaba.rocketmq.remoting.protocol.RemotingCommand;

@Component
public class MQProducer {
	
	private final String GROUP_NAME = "transaction-pay";
	private final String NAMESRV_ADDR = "192.168.1.111:9876;192.168.1.112:9876;192.168.1.113:9876;192.168.1.114:9876";
	private TransactionMQProducer producer;
	
	public MQProducer() {
		
		this.producer = new TransactionMQProducer(GROUP_NAME);
		this.producer.setNamesrvAddr(NAMESRV_ADDR);	//nameserver服务
		this.producer.setCheckThreadPoolMinSize(5);	// 事务回查最小并发数
		this.producer.setCheckThreadPoolMaxSize(20);	// 事务回查最大并发数
		this.producer.setCheckRequestHoldMax(2000);	// 队列数
		//服务器回调Producer，检查本地事务分支成功还是失败
		this.producer.setTransactionCheckListener(new TransactionCheckListener() {
			public LocalTransactionState checkLocalTransactionState(MessageExt msg) {
				System.out.println("state -- "+ new String(msg.getBody()));
				return LocalTransactionState.COMMIT_MESSAGE;
			}
		});
		try {
			this.producer.start();
		} catch (MQClientException e) {
			e.printStackTrace();
		}	
	}
	
	public QueryResult queryMessage(String topic, String key, int maxNum, long begin, long end) throws Exception {
		return this.producer.queryMessage(topic, key, maxNum, begin, end);
	}
	
	public LocalTransactionState check(MessageExt me){
		LocalTransactionState ls = this.producer.getTransactionCheckListener().checkLocalTransactionState(me);
		return ls;
	}
	
	public void sendTransactionMessage(Message message, LocalTransactionExecuter localTransactionExecuter, Map<String, Object> transactionMapArgs) throws Exception {
		TransactionSendResult tsr = this.producer.sendMessageInTransaction(message, localTransactionExecuter, transactionMapArgs);
		System.out.println("send返回内容：" + tsr.toString());
	}
	
	public void shutdown(){
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				producer.shutdown();
			}
		}));
		System.exit(0);
	}


}