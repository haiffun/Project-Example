package netty.googleprotobuf;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.util.CharsetUtil;

import java.util.Random;

public class NettyClient {
	public static void main(String[] args) {
		// 客户端需要一个事件循环组
		EventLoopGroup group = new NioEventLoopGroup();

		// 创建客户端启动对象
		// 注意客户端使用的不是ServerBootstrap 而是Bootstrap
		Bootstrap bootstrap = new Bootstrap();
		// 设置相关参数
		bootstrap.group(group) //设置线程组
				.channel(NioSocketChannel.class) // 设置客户端通道的实现类(反射)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						//在pipeline中加入 ProtoBufEncoder
						pipeline.addLast("encoder", new ProtobufEncoder());
						pipeline.addLast(new NettyClientHandler()); //加入自己的处理器
					}
				});

		System.out.println("客户端准备就绪");

		try {
			// 启动客户端去连接服务器端
			ChannelFuture channelFuture = bootstrap.connect("127.0.0.1", 8080).sync();
			// 监听关闭通道
			channelFuture.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			group.shutdownGracefully();
		}
	}
}

class NettyClientHandler extends ChannelInboundHandlerAdapter {

	/**
	 * 当通道就绪就会触发该方法
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		//随机发送Student 或者 Worker 对象
		int random = new Random().nextInt(3);
		DataInfo.Message message;

		if(random < 2) {

			message = DataInfo.Message.newBuilder().setDataType(DataInfo.Message.DataType.StudentType).setStudent(DataInfo.Student.newBuilder().setId(5).setName("张三").build()).build();
		} else {

			message = DataInfo.Message.newBuilder().setDataType(DataInfo.Message.DataType.WorkerType).setWorker(DataInfo.Worker.newBuilder().setAge(20).setName("李四").build()).build();
		}

		ctx.writeAndFlush(message);
	}

	/**
	 * 当通道有读取事件时触发
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf buf = (ByteBuf) msg;
		System.out.println("服务器消息: " + buf.toString(CharsetUtil.UTF_8));
		System.out.println("服务器地址: "+ ctx.channel().remoteAddress());
	}

	/**
	 * 异常事件
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}
}