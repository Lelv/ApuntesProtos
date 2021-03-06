package posta;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by lelv on 5/21/16.
 */
public class Handler {

    private static ExecutorService pool;
    private static final int WORKER_POOL = 100;

    public Handler(){
        pool = Executors.newFixedThreadPool(WORKER_POOL);
    }

    public void handleAccept(SelectionKey key) throws IOException {
        System.out.println("---Handling Accept---");

        SocketChannel socketChannelClient = ((ServerSocketChannel) key.channel()).accept();

        if(socketChannelClient == null) return;

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        socketChannelClient.configureBlocking(false);

        NioConnection ncClient = new NioConnection(buffer);
        SelectionKey clientKey = socketChannelClient.register(key.selector(), SelectionKey.OP_READ, ncClient);

        SocketChannel sc = SocketChannel.open();
        sc.configureBlocking(false);
        sc.connect(new InetSocketAddress("localhost", 110));

        SelectionKey otherKey = sc.register(key.selector(), SelectionKey.OP_CONNECT, new NioConnection(clientKey, buffer));

        ncClient.setSelectionKey(otherKey);
    }

    public void handleConnect(SelectionKey key) throws IOException {
        System.out.println("---Handling Connect---");
        SocketChannel socketChannel = (SocketChannel) key.channel();
        if(socketChannel.finishConnect()){
            ((NioConnection)key.attachment()).setConnected(true);
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    public void handleRead(SelectionKey key) throws IOException {
        System.out.println("---Handling Read--- " + key.toString());
        NioConnection nc = (NioConnection) key.attachment();

        if(!nc.isConnected()) return;

        SelectionKey otherKey = nc.getSelectionKey();
        SocketChannel actualChannel = (SocketChannel) key.channel();

        ByteBuffer buf = nc.getByteBuffer();
        long bytesRead = actualChannel.read(buf);

        System.out.println(new String(buf.array()));
        if(bytesRead > 0){
            otherKey.interestOps(SelectionKey.OP_WRITE);
        }

        if(bytesRead == -1){
            if(buf.hasRemaining()){
                System.out.println("-** Setting terminated **-");
                nc.setTerminated();
                otherKey.interestOps(SelectionKey.OP_WRITE);
            }else{
                System.out.println("-*- Closing Channels (read)-*-");
                otherKey.channel().close();
                otherKey.cancel();

            }
            actualChannel.close();
            key.cancel();
        }

    }

    public void handleWrite(SelectionKey key) throws IOException {
        System.out.println("---Handling Write--- " + key.toString());

        NioConnection nc = (NioConnection) key.attachment();
        SocketChannel actualChannel = (SocketChannel) key.channel();
        ByteBuffer buf = nc.getByteBuffer();
        buf.flip();
        actualChannel.write(buf);

        if (!buf.hasRemaining()) {
            if(nc.isTerminated()){
                System.out.println("-*- Closing Channels (write) -*-");
                actualChannel.close();
                key.cancel();
            }else{
                key.interestOps(SelectionKey.OP_READ);
            }

        }
        buf.compact();
    }
}
