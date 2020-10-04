# im_sys
聊天系统：后台基于springboot，前台基于mui
# 聊天实现

websocket+netty

## http

http请求是客户端发起请求，服务端响应，然后断开连接，客户端发起，服务端响应的一种循环。

## websocket

### 特点

websocket是客户端发起连接后，就会一直保持连接，期间客户端和服务端都能向对方发送数据，直到连接关闭。

### 其他特点

1、建立于TCP协议之上

2、与http协议有良好的兼容性，默认端口也是80和443，并且握手阶段采用http协议，因此握手时不容易屏蔽，能通过各种http代理服务器。

3、数据格式比较轻量，性能开销小，通信高效。

4、可以发送文本，也可以发送二进制数据。

5、没有同源限制，可以与任意服务器通信。

6、协议标识符为'ws'(加密为'wss')，服务网址就是URL

### 应用场景

将http这种无状态的协议升级为有状态的ws协议，让聊天保持长连接。

## netty

一个异步的事件驱动的网络框架

异步：客户端向服务端发送请求之后不需等待服务端的返回结果

事件驱动：将事件抽取出来映射到一个个方法上，当事件发生后对应的回调函数将立刻被调用并执行。

### 实现

#### 1、创建WebScoketServer类并注入到spring容器中（单例）

```java
@Component
public class WebSocketServer {
    private static class SigletionWSServer{
        static final WebSocketServer instance = new WebSocketServer();
    }
    public static WebSocketServer getInstance(){
        return SigletionWSServer.instance;
    }
```

采用主从线程池模型

```java
private EventLoopGroup mainGroup;//监听客户/服务端连接
private EventLoopGroup subGroup;//处理连接
```

服务启动器

```java
private ServerBootstrap server;
```

netty是异步的，一个操作可能不会立刻返回，我们需要一种用于在之后某个时间点确定结果的方法ChannelFuture

```java
private ChannelFuture future;
```

无参构造创建各个功能

需要添加子处理器WSServerInit

```java
public WebSocketServer(){
        mainGroup = new NioEventLoopGroup();
        subGroup = new NioEventLoopGroup();
        server = new ServerBootstrap();
        server.group(subGroup, subGroup)
              .channel(NioServerSocketChannel.class)
                .childHandler(new WSServerInit());
    }
```

#### 2、WSServerInit

继承ChannelInitializer<SocketChannel>并重写initChannel方法

```java
public class WSServerInit extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel){

```



获取管道，保存ChannelHandler的list，用于处理或拦截channel的入站事件和出站操作

```java
ChannelPipeline pipeline = socketChannel.pipeline();
```

websocket基于http协议所需要的http编解码器

```java
pipeline.addLast(new HttpServerCodec());
```

在http上有一些数据流产生，有大有小，我们对其处理，既然如此，我们需要使用netty对数据流写 提供支持

```java
pipeline.addLast(new ChunkedWriteHandler());
```

对httpmessage进行聚合处理，聚合成request和response

```java
pipeline.addLast(new HttpObjectAggregator(1024*64));
```

心跳支持

针对客户端在一分钟时间内没有向服务端发送读写心跳（ALL）主动断开连接

有读有写空闲则不做处理

```java
/**
     * Creates a new instance firing {@link IdleStateEvent}s.
     *
     * @param readerIdleTimeSeconds
     *        an {@link IdleStateEvent} whose state is {@link IdleState#READER_IDLE}
     *        will be triggered when no read was performed for the specified
     *        period of time.  Specify {@code 0} to disable.
     * @param writerIdleTimeSeconds
     *        an {@link IdleStateEvent} whose state is {@link IdleState#WRITER_IDLE}
     *        will be triggered when no write was performed for the specified
     *        period of time.  Specify {@code 0} to disable.
     * @param allIdleTimeSeconds
     *        an {@link IdleStateEvent} whose state is {@link IdleState#ALL_IDLE}
     *        will be triggered when neither read nor write was performed for
     *        the specified period of time.  Specify {@code 0} to disable.
     */
/ ** 
    *创建一个触发{@link IdleStateEvent}的新实例。 
    * 
    * @param readerIdleTimeSeconds 
    *状态为{@link IdleState＃READER_IDLE}的{@link IdleStateEvent} 
	*在指定的时间段内未执行任何读取时将被触发。指定{@code 0}禁用。 
    * @param writerIdleTimeSeconds 
    *状态为{@link IdleState＃WRITER_IDLE}的{@link IdleStateEvent} 
	*在指定的时间段内未执行任何写操作时将被触发。指定{@code 0}禁用。 
    * @param allIdleTimeSeconds *状态为{@link IdleState＃ALL_IDLE}的{@link IdleStateEvent} 
	*如果在指定的时间内未执行读写操作，将被触发。指定{@code 0}禁用。 
    * /
pipeline.addLast(new IdleStateHandler(8,10,12));
//自定义空闲状态检测的handler
pipeline.addLast(new HeartBeatHandler());
```

HeartBeatHandler

```java
/**
 * 用于检测channel的心跳handler
 * 继承ChannelInboundHandlerAdapter
 */
public class HeartBeatHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent){
            IdleStateEvent event = (IdleStateEvent) evt;//强转
            if (event.state()== IdleState.READER_IDLE){
                System.out.println("进入读空闲...");
            } else if (event.state() == IdleState.WRITER_IDLE){
                System.out.println("进入写空闲...");
            } else if (event.state() == IdleState.ALL_IDLE){
                System.out.println("channel 关闭之前：users的数量为："+ChatHandler.users.size());
                Channel channel = ctx.channel();
                //资源释放
                channel.close();
                System.out.println("channel 关闭之后：users的数量为："+ChatHandler.users.size());
            }
        }
    }
}
```

将协议升级为ws协议并添加访问路径

```java
pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
//自定义handler
pipeline.addLast(new ChatHandler());
```

ChatHandler用于处理消息的handler

由于它的传输数据的载体是frame在netty中，是用与为websocket专门处理文本对象的，frame是消息载体，此类叫TextWebSocketFrame

TextWebSocketFrame：表示一个文本帧

继承SimpleChannelInboundHandler<TextWebSocketFrame> 

```java
public class ChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> 
```

记录和管理所有客户端的channel

```java
public static ChannelGroup users = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
```

当客户端与服务端建立连接自动执行channelRead0方法

```java
protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        //1、获取客户端所传输的消息
        String content = msg.text();
        DataContent dataContent = JsonUtils.jsonToPojo(content, DataContent.class);
        Integer action = dataContent.getAction();
        Channel channel = ctx.channel();
        //2、判断消息类型根据不同类型来处理不同业务
        if (action == MsgActionEnum.CONNECT.type){
//            2.1当websocket第一次open的时候初始化channel，把channel和userid关联起来
            String senderId = dataContent.getChatMsg().getSenderId();
            UserChannerlRel.put(senderId,channel);
            for (Channel c:users
                 ) {
                System.out.println(c.id().asLongText());
            }
            UserChannerlRel.output();
        }else if (action==MsgActionEnum.CHAT.type){
//            2.2聊天类型的消息，把聊天记录保存到数据库中，同时标记消息的签收状态是[未签收]
            ChatMsg chatMsg = dataContent.getChatMsg();
            String msgContent = chatMsg.getMsg();
            String senderId = chatMsg.getSenderId();
            String receiverId = chatMsg.getReceiverId();
            //保存消息到数据库，并且标记为未签收
            UserService userService = (UserService) SpringUtil.getBean("userService");
            String msgId = userService.saveMsg(chatMsg);
            chatMsg.setMsgId(msgId);
            DataContent dataContentMsg = new DataContent();
            dataContentMsg.setChatMsg(chatMsg);
            //发送消息
            Channel receiverChannel = UserChannerlRel.get(receiverId);
            if (receiverChannel == null){
                //离线用户

            } else {
                //当receiverChannel不为空的时候，从ChannelGroup去查找对应的channel是否存在
                Channel findChannel = users.find(receiverChannel.id());
                if (findChannel != null){
                    //用户在线
                    receiverChannel.writeAndFlush(new TextWebSocketFrame(
                            JsonUtils.objectToJson(dataContentMsg)
                    ));
                } else {
                    //离线
                }
            }

        }else if (action==MsgActionEnum.SIGNED.type){
//            2.3签收消息类型，针对具体消息进行签收，修改数据库中对应的消息的签收状态[已签收]
            UserService userService = (UserService) SpringUtil.getBean("userService");
            //扩展字段signed 类型消息中，代表需要去签收的消息id，逗号间隔
            String msgIdsStr = dataContent.getExtand();
            String[] msgsId = msgIdsStr.split(",");

            List<String> msgList = new ArrayList<>();
            for (String mid:msgsId) {
                if (StringUtils.isNotBlank(mid)){
                    msgList.add(mid);
                }
            }
            System.out.println(msgList.toString());
            if (msgList!=null&&!msgList.isEmpty()&&msgList.size()>0){
                //批量签收
                userService.updateMsgSigned(msgList);
            }

        }else if (action==MsgActionEnum.KEEPALIVE.type){
//            2.4心跳类型的消息
            System.out.println("收到来自channel为：["+channel+"]的心跳包");
        }
```
