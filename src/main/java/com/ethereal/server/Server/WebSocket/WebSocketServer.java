package com.ethereal.server.Server.WebSocket;

import com.ethereal.server.Core.Model.TrackException;
import com.ethereal.server.Server.Abstract.Server;
import com.ethereal.server.Server.Delegate.CreateInstanceDelegate;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebSocketServer extends Server {
    protected boolean isClose = false;
    protected ExecutorService es;
    private Channel channel;
    public WebSocketServer(List<String> prefixes) {
        super(prefixes);
        config = new WebSocketServerConfig();
        this.es= Executors.newFixedThreadPool(getConfig().threadCount);
    }

    public ExecutorService getEs() {
        return es;
    }

    public void setEs(ExecutorService es) {
        this.es = es;
    }


    public WebSocketServerConfig getConfig() {
        return (WebSocketServerConfig)config;
    }

    @Override
    public void start() {
        try {
            NioEventLoopGroup boss=new NioEventLoopGroup();
            NioEventLoopGroup work=new NioEventLoopGroup();
            try {
                URI uri = new URI(prefixes.get(0).replace("ethereal://","ws://"));
                WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(uri.toString(),null, false);
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(boss,work)                                //2
                        .channel(NioServerSocketChannel.class)            //3
                        .childHandler(new ChannelInitializer<SocketChannel>() {    //5
                            @Override
                            public void initChannel(SocketChannel ch) {
                                //????????????
                                ch.pipeline().addLast(new HttpServerCodec());
                                ch.pipeline().addLast(new HttpObjectAggregator(getConfig().getMaxBufferSize()));
                                ch.pipeline().addLast(new ChunkedWriteHandler());
                                ch.pipeline().addLast(new CustomWebSocketHandler(net.getName(),es, wsFactory));
                            }
                        });
                channel = bootstrap.bind(uri.getPort()).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()){
                            onListenerSuccess();
                        }
                        else {
                            onListenerFailEvent();
                        }
                    }
                }).channel();
                channel.closeFuture().sync();
            }
            catch (Exception e){
                onException(new TrackException(e));
            }
            finally {
                boss.shutdownGracefully();
                work.shutdownGracefully();
            }
        } catch (Exception e){
            onException(new TrackException(e));
        }
    }

    @Override
    public void close() {
        if(!isClose){
            channel.close();
        }
    }
}
