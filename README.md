###RocketMQ
本身就是分布式的队列模型的消息中间件.,具有以下特点：
1. 能够严格保证消息顺序.
2. 提供丰富的的消息拉取模式.
3. 高效的订阅者水平扩展能力.
4. 实时消息订阅机制.
5. 亿级消息堆积能力.
6. 消息失败重试机制、消息可查询

参考资料见reference文件夹

相同的Consumer Group，有多个Consumer可以实现天然的负载均衡。
可以使用AllocateMessageQueueAveragely()或AllocateMessageQueueAveragelyVyCircle()
设置策略，完全平均，或者根据机器性能自动分配。

永远都是持久化的。只要消息发送到了MQ上。
RocketMQ可能会存在消息重复消费问题，需要业务上去重。
RocketMQ的Message可以设置参数keys，保证消息唯一，用来去重。
并且处理方法幂等（方法同一参数运行，每次结果相同）

---
---
####专业术语
Producer:生产者; Consumer:消费者; 
Push Consumer：启用Listener监听消息时间的消费者;
Pull Consumer:主动调用拉方法从Broker（中间件）拉消息的消费者;
Producer Group:生产者组，发送一类的消息，且关系逻辑一致(负载均衡)；
Consumer Group:消费者组，消费一类的消息，且关系逻辑一致（负载均衡）；
Broker：消息中转角色，负责存储、转发消息，也称为Server，在JMS规范中称为Provider;

广播消费(pub/sub):一个消息多个Consumer消费，则这些Consumer属于一个Consumer Group，消息也会被
Consumer Group中的每个Consumer消费一次。广播消费中的Consumer Group概念可以认为在消息
划分方面无意义。

集群消费：一个Consumer Group中的Consumer均分消息。负载均衡。
类似PTP，但一个消息可以被多个Consumer消费。

顺序消费：发送顺序一致。在RocketMQ中，主要指局部顺序，即一类消息满足顺序型。
Producer必须单线程顺序地发送到一个队列，Consumer才能顺序消费。

---
#### 集群方式
1. 单个Master，风险较大，一旦Broker宕机，整个服务不可用。
2. 多个Master。无Slave。配置简单，单个Master宕机无影响。性能最好
但单台机器宕机期间，该机器上未被消费的消息在机器恢复前不可订阅，实时性受影响。
3. 多Master，多Slave，异步复制。每个Master配置一个Slave。可以保证实时
有短暂的消息延迟，毫秒级。宕机可能会丢失少量消息。
4. 多Master，多Slave，同步双写。每个Master配置一个Slave。可以保证实时
消息无延迟，性能比异步复制稍低，宕机不丢失消息
---
#### RocketMQ环境搭建
双Master模式。

1. 配置两台Linux的hosts。vim /etc/hosts ;
    增加如下：
    
    192.168.2.104 rocketmq-nameserver1
    192.168.2.104 rocketmq-master1
    192.168.2.105 rocketmq-nameserver2
    192.168.2.105 rocketmq-master2
    
    完成后可以相互ping rocketmq-nameserver1 ,看能否ping通。
    
    192.168.2.104 rocketmq-nameserver1 2 这两个必须配。相当于zookeeper
    
2. 下载tar.gz.这个mq是我有生以来最他妈的难的一次下载。。还要编译。。
 
    git 地址https://github.com/apache/incubator-rocketmq;
    
    下载release-4.0.0-incubating版本
    
    下载到源码包后解压，进入到源码目录 ，执行maven命令编译打包（maven没有配置过自己去百度）：
    mvn -Dmaven.test.skip=true clean package install assembly:assembly   -P release-all  -U
    注意 -P  release-all 参数一定要添加
    
    编译后在target目录下，apache-rocketmq-all目录就是包含了所有内容  ，apache-rocketmq-all.tar.gz  是它的压缩包；
    然后将apache-rocketmq-all.tar.gz，拷贝到linux系统下解压运行;
    
    注意。。。上面的-p是-P。。坑了我大半年。。。
    
    解压到某个路径下
    tar -zxvf rocketmq.tar.gz -C /zx/rocketmq
    然后可以自己建几个文件夹，专门保存文件
    mkdir rocketmq/store -p
    mkdir rocketmq/store/commitlog -p
    mkdir rocketmq/store/consumequeue -p
    mkdir rocketmq/store/index -p  #索引
    
    进入rocketmq/conf/2m-noslave文件夹中，
    有broker-a.properties和broker-b.properties两个文件
    这两个文件就是Master a和b的配置文件。
    
    清空这两个文件原来的内容，复制如下内容进去：
    
    #所属集群名字
    brokerClusterName=rocketmq-cluster
    #broker名字，注意此处不同的配置文件填写的不一样,写a或者b
    brokerName=broker-a
    #0 表示 Master，>0表示Slave
    brokerId=0
    #nameServer地址，分号分割，如果配置了hosts，这里可以直接写name：端口号
    namesrvAddr=rocketmq-nameserver1:9876;rocketmq-nameserver2:9876
    #在发送消息时，自动创建服务器不存在的topic，默认创建的队列数
    defaultTopicQueueNums=4
    #是否允许 Broker 自动创建Topic，建议线下开启，线上关闭
    autoCreateTopicEnable=true
    #是否允许 Broker 自动创建订阅组，建议线下开启，线上关闭
    autoCreateSubscriptionGroup=true
    #Broker 对外服务的监听端口
    listenPort=10911
    #删除文件时间点，默认凌晨 4点
    deleteWhen=04
    #文件保留时间，默认 48 小时
    fileReservedTime=120
    #commitLog每个文件的大小默认1G
    mapedFileSizeCommitLog=1073741824
    #ConsumeQueue每个文件默认存30W条，根据业务情况调整
    mapedFileSizeConsumeQueue=300000
    #destroyMapedFileIntervalForcibly=120000
    #redeleteHangedFileInterval=120000
    #检测物理文件磁盘空间
    diskMaxUsedSpaceRatio=88
    #存储路径
    storePathRootDir=/zx/rocketmq/store
    #commitLog 存储路径.所有Message记录都存储在这
    storePathCommitLog=/zx/rocketmq/store/commitlog
    #消费队列存储路径存储路径，存储消息在commitlog中的位置信息，偏移量
    storePathConsumeQueue=/zx/rocketmq/store/consumequeue
    #消息索引存储路径
    storePathIndex=/zx/rocketmq/store/index
    #checkpoint 文件存储路径
    storeCheckpoint=/zx/rocketmq/store/checkpoint
    #abort 文件存储路径
    abortFile=/zx/rocketmq/store/abort
    #限制的消息大小
    maxMessageSize=65536
    
    #flushCommitLogLeastPages=4
    #flushConsumeQueueLeastPages=2
    #flushCommitLogThoroughInterval=10000
    #flushConsumeQueueThoroughInterval=60000
    #Broker 的角色
    #- ASYNC_MASTER 异步复制Master
    #- SYNC_MASTER 同步双写Master
    #- SLAVE 表示是从节点
    brokerRole=ASYNC_MASTER
    
    #刷盘方式
    #- ASYNC_FLUSH 异步刷盘
    #- SYNC_FLUSH 同步刷盘
    flushDiskType=ASYNC_FLUSH
    #checkTransactionMessageEnable=false
    #发消息线程池数量
    #sendMessageThreadPoolNums=128
    #拉消息线程池数量
    #pullMessageThreadPoolNums=128
    
3. 修改日志文件配置。
    新建日志文件夹
    mkdir /zx/rocketmq/logs  
    替换所有xml文件的日志配置
    cd /zx/rocketmq/conf && sed -i 's#${user.home}#/zx/rocketmq#g' *.xml

4. 修改启动脚本的jvm参数
    
    broker
    
     vim /zx/rocketmq/bin/runbroker.sh
     
    server
    
    vim /zx/rocketmq/bin/runserver.sh
    
     注意，内存至少1G
     JAVA_OPT="${JAVA_OPT} -server -Xms1g -Xmx1g -Xmn512m XX:PermSize=128m -XX:MaxPermSize=320m"
    
5. 开始启动
    首先要启动的是NameServer（两台机器）：
     cd /zx/rocketmq/bin #进入目录
     nohup sh mqnamesrv &    #启动脚本
     
     然后可以查看对应的log，看是否成功启动。
     tail -f -n 500 /zx/rocketmq/logs/rocketmqlogs/namesrv.log 

    然后启动BrokerServer,一台机器用a配置文件，一台机器用b配置文件
    nohup sh mqbroker -c /zx/rocketmq/conf/2m-noslave/broker-a.properties  >/dev/null 2>&1 & 
    nohup sh mqbroker -c /zx/rocketmq/conf/2m-noslave/broker-b.properties  >/dev/null 2>&1 & 
    
    然后也可以查看log，是否启动成功
    tail -f -n 500 /zx/rocketmq/logs/rocketmqlogs/broker.log
    
6. 停止服务
     cd /usr/local/rocketmq/bin 
     sh mqshutdown broker 
     sh mqshutdown namesrv 
    
2017年5月11日 23:23:01
    辞职没有几日，为了月入过完，学习到现在。望不负。
    
    
---
###安装tomcat    
http://www.cnblogs.com/hanyinglong/p/5024643.html

解压
tar -zxvf apache-tomcat-8.0.43.tar.gz  
改名
mv apache-tomcat-8.0.43 tomcat
启动
/zx/tomcat/bin/startup.sh
停止
/zx/tomcat/bin/shutdown.sh

启动java web 项目，将项目放到webapps目录下。

查看tomcat日志
tail -f -n 500 /zx/tomcat/logs/catalina.out   

---
####Console
可以在tomcat中部署rocketmq-console.war开启控制台
首先部署tomcat。

然后将该war放到webapps目录中，然后启动一次tomcat，让war自动解压；

然后修改解压文件中WEB-INF/classes中修改config.properties,
rocketmq.namesrv.addr=192.168.2.104:9876;192.168.2.105:9876   也就是修改成每个mq节点的ip和端口
---
####快速入门，简单例子，见 quickstart包
注意，使用rocketmq必须有4G的空闲磁盘空间。

---
####Consumer参数配置
启动后，从什么位置开始消费,默认从尾部(CONSUME_FROM_LAST_OFFSET)
consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET)

消费者线程池最小数量
consumer.setConsumeThreadMin(10)

线程池最大数
setConsumeThreadMax(20)

拉消息本地队列缓存消息最大数,默认1000
setPullThresholdForQueue(1000)

批量消费,一次性消费多少条，默认1条 pushConsumer,
但是即使设置成10条，也只是最大获取10条，不一定每次都是10条。
setConsumeMessageBatchMaxSize(1)

批量拉消息，一次型最多拉去多少条，默认32条 PullConsumer
setPullBatchSize(32)

消息拉取线程每隔多久拉取一次，默认为0
setPullInterval(0)

---
！！！最好都是先启动Consumer，再启动Producer。
也就是先订阅topic，在发送.
这样拉取数据的时候，一条消息到了，就能马上处理。也就是监听器的方法中，参数虽然是list，
但一次只会处理一条数据，即使上面的setConsumeMessageBatchMaxSize(1)参数设置成多条，
也只会一条。
（据我理解，如果消息并发不大，应该都是一条，但如果并发很大，应该可能会有多条的）

如果先启动Producer，再启动Consumer，那么MQ中已经堆积了很多的消息，
监听器的处理方法，一次就会处理多条数据。

！！! 推模型Push不适合批处理。
---
###消息的重试机制
Producer可以设置x ms内，消息没发送成功，就重试，还可以设置重试次数。

Consumer如果MQ消息发送到Consumer时失败的话，也会自动重试，无限循环。

如果是Consumer返回消息处理失败状态，那么会尝试几秒后消息重新发送。
（这样的话，如果是批处理时，多条数据都会重试）
然后可以获取到每条消息的重试次数(message.getReconsumeTimes())，这样就可以在
catch中，判断一条消息已经重试了多少次，如果大于2，就记录日志，直接返回成功，不再重试。

如果Consumer没有返回消息状态的话（例如consumer挂了），那么也算没有发送到达。会重试

---
#### 各种集群模式的配置
RocketMQ文件中，在conf文件中有2m-xxx的各种模式，也就是2台Master的各种模式。
例如同步复制，异步双写等。只要修改其中的properties文件。

---
### 多Master多Slave，异步复制模式集群搭建
2个Master，2个Slave

1. 配置4台Linux的hosts。vim /etc/hosts ;
    增加如下：
    
    192.168.2.104 rocketmq-nameserver1
    192.168.2.104 rocketmq-master1
    192.168.2.105 rocketmq-nameserver2
    192.168.2.105 rocketmq-master2
    192.168.2.106 rocketmq-nameserver3
    192.168.2.106 rocketmq-master1-slave
    192.168.2.107 rocketmq-nameserver4
    192.168.2.107 rocketmq-master2-slave
    
2. 进入rocketmq/conf/2m-2s-async目录下
    有
    broker-a.properties
    broker-a-s.properties
    broker-b.properties
    broker-b-s.properties
    四个文件
    
    然后修改还是复制上面的那个参数，然后稍有不同：
    
    Master和Slave中：
    这里都要写4个
    namesrvAddr=rocketmq-nameserver1:9876;rocketmq-nameserver2:9876;rocketmq-nameserver3:9876;rocketmq-nameserver4:9876
    同一对主从节点，相同
    brokerName=broker-a
    
    Slave中：
    这里要写1（主节点个数？）， >0表示是Slave
    brokerId=1
    这个要改，
    brokerRole=SLAVE
    
3. 其他一些配置都和之前的双Master一样。

4. 启动
    cd /zx/rocketmq/bin #进入目录
    nohup sh mqnamesrv &    #启动脚本
     
    然后可以查看对应的log，看是否成功启动。
    tail -f -n 500 /zx/rocketmq/logs/rocketmqlogs/namesrv.log 

    然后启动BrokerServer,
    四台机器按照如下命令顺序启动
    nohup sh mqbroker -c /zx/rocketmq/conf/2m-2s-async/broker-a.properties  >/dev/null 2>&1 & 
    nohup sh mqbroker -c /zx/rocketmq/conf/2m-2s-async/broker-b.properties  >/dev/null 2>&1 & 
    nohup sh mqbroker -c /zx/rocketmq/conf/2m-2s-async/broker-a-s.properties  >/dev/null 2>&1 & 
    nohup sh mqbroker -c /zx/rocketmq/conf/2m-2s-async/broker-b-s.properties  >/dev/null 2>&1 & 
    
    然后也可以查看log，是否启动成功
    tail -f -n 500 /zx/rocketmq/logs/rocketmqlogs/broker.log    
    
    如果需要console,那么WEB-INF/config.properties 同样要加ip和端口号
    
这种模式，主节点挂了，从节点还是可以consume的，不过无法produce

---
---
#### 详解
RocketMQ不遵循JMS，没有PTP。只有一套订阅主题的方式去发送和接收任务，
这种方式支持集群和广播两种模式。
使用consumer.setMessageModel(xxx);设置
* 集群模式：Message.CLUSTERING
指一条消息，只会被其中一个消费者消费一次。
且生产者可以先发送消息，消费者在之后订阅也可以收到之前的数据。

* 广播模式：MessageModel.BROADCASTING
一条消息，会被所有的消费者消费一次。

Topic(主题)，订阅和发布的主题，每个主题下默认有4个queue。
topic是逻辑上的概念，4个queue才是物理存储的地方。
---
tag可以进行简单的过滤，如果需要更复杂的过滤，可以使用filtersv插件。

---

### Producer分为三种模式

* NormalProducer(普通)：
不能保证消息的顺序一致性
---
* OrderProducer（顺序）：
可以保证严格的消息顺序进行消费。
只有前一条消息被消费完成，下一条消息才能进行消费。
因为topic下有多个queue，如果进入不同队列是无法保证顺序一致性的。

这么写可以保证顺序消费：
Producer：
            /**
             * 如果要顺序消费，需要使用MessageQueueSelector，保证消息进入同一个队列
             */
            producer.send(message, new MessageQueueSelector() {
                public MessageQueue select(List<MessageQueue> list, Message message, Object o) {
                    Integer id =(Integer)o;
                    return list.get(id);
                }
            },0);//0是队列的下标,这个是上面select()方法中的Object o
Consumer：
         MessageListenerOrderly使用这个消息监听器。
上面这个只能保证消息过来的顺序，如果在Consumer程序中，使用多线程处理消息，那么无法保证
先过来的消息一定会被先处理完成。
RocketMQ中，Consumer想要使用多线程，不用自己写个threadPool，直接修改参数中的消费者线程池线程数就可以了。

---
* TransactionProducer（事务）：
分布式事务的需求，例如进行支付宝转账1W到余额宝，就需要考虑，如果支付宝扣完钱后，余额宝那个系统
宕机了怎么办。

两阶段提交协议可以实现分布式事务，也就是顺序执行，先修改A模块，然后回调后修改B模块。但是效率很低。

可以使用MQ实现分布式事务。
例如上面的支付宝转账例子，只要在支付宝扣款后，生成一个消息，那么就算此时余额宝挂了，只要消息还在，
也还是可以在余额宝中加上金额的，也就是保证最终的一致性。

具体实现有两种。
1. 业务与消息耦合： 
在支付宝系统，将扣款（修改记录）和添加消息（增加记录）放置到一个事务。确保只要扣款了，消息就一定生成；
2. 业务与消息解耦：
将给余额宝增加金额这个业务封装成一个消息，把这个消息发到MQ上。在这个消息发送的回调方法（CallBack）中
处理本地事务：扣款。
如果本地事务成功后，在回调方法中Commit消息。
如果本地事务失败，在回调方法中rollback消息。

需要创建TransactionProducer对象。
然后使用如下方法发送消息，第一个参数是消息，
第二个是发送时本地执行的业务方法，也就是自定义一个类，实现localTransactionExecuter接口，
在这接口的方法中，可以选择提交或回滚之前发送的消息。还有一种是未知，也就是不知道成功或失败。
第三个参数可以传给localTransactionExecuter的方法中作为参数
producer.sendMessageInTransaction(message, localTransactionExecuter, transactionMapArgs)

会有一个问题，就是如果回调的方法中，让消息回滚或提交的这个发送未成功（也就是提交或回滚这个指令发送到MQ的时候失败）。
所以RocketMQ有一个机制，定时检查所有TransactionProducer，看是不是有消息是未知状态的（就是没有回滚也没有提交的）。
MQ就会调用Producer的回调检查方法（不过3.2.6版本后，这个分布式事务的回调方法闭源了），如下：
    可以在producer创建后，设置对应的回调检查方法。
    //服务器回调Producer，检查本地事务分支成功还是失败
		producer.setTransactionCheckListener(new TransactionCheckListener() {
			public LocalTransactionState checkLocalTransactionState(MessageExt msg) {
				System.out.println("state -- "+ new String(msg.getBody()));
				return LocalTransactionState.COMMIT_MESSAGE;
			}
		});

RocketMQ，事务消息的实现很简单，
因为所有消息是存在commitlog文件中的，但获取消息需要从comsumequeue文件中获取消息在commitlog的位置，
那么只要消息没有回滚或提交，就只把消息存在commitlog中，却不存到consumequeue中，那么消息就永远不会被消费了。
---
---
#### 阉割分布式事务后的解决方案
当时用TransactionProducer的时候，消息一边发送到MQ，程序自己也会执行自己的业务方法，在这个方法中，
可以根据业务是否执行成功等状况选择提交或回滚事务。
但是可能有一种情况，那就是，这个业务方法执行完毕后，向MQ发送该消息的提交或回滚指令时，发送失败了。
那么此时这条消息存在于MQ中，且不会被任何消费者消费，这条消息就丢失了。
本来RocketMQ提供了一种定时检查TransactionMessage消息的机制，如果发现消息未回滚、未提交，就调用Producer
设置好的回调方法进行处理，但是在3.0.8版本后已经阉割。

那么为了处理这种消息可能丢失的情况。可以使用如下方式：
1. 生产端发送事务消息，然后执行本地方法（进行一整个分布式事务的第一阶段操作）的时候要记录这个更新或创建时间。
2. 消费端将每条消费了的消息存入数据库。
3. 消费端开启一个定时线程，每隔一段时间，扫描这个数据库。把最新的记录再通过MQ或者什么发回给Producer端。
4. 生产端收到消费端的数据后，就将数据库中对应的记录标识为已完成（事务执行完毕）
5. 同时生产端也有一个定时扫描数据库的线程，扫描状态为未完成，且更新/创建时间距离当前时间已经超过一定时间
的记录。将该记录时间更新，并重发消息。
---
---
### Consumer 
分为PullConsumer和PushConsumer。
其本质都是拉模式（Pull），即consumer轮询从broker拉取消息
* PushConsumer，推模型消费者(常用)：把轮训过程封装了。并注册了监听器，拉取到消息后，
唤醒监听器的consumeMessage()来消费。

* PullConsumer，拉模式消费者：拉取消息的过程需要用户自己写。首先通过Topic获取到
MessageQueue的集合，遍历MessageQueue集合，然后针对每个MessageQueue批量获取消息，
一次获取完成后，记录该队列下一次要取的offset（偏移量），知道取完了，换一个MessageQueue.
---
---
##＃ Filter 过滤器 filtersrv插件

http://lifestack.cn/archives/371.html

---
---
RocketMQ还有许多运维命令，详见reference中的运维命令整理。
