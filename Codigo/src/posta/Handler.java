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

        if(socketChannelClient == null){
            System.out.println("NULL");
            return;
        }

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
        System.out.println("---Handling Read---");
        NioConnection nc = (NioConnection) key.attachment();

        SelectionKey otherKey = nc.getSelectionKey();
        SocketChannel actualChannel = (SocketChannel) key.channel();

        ByteBuffer buf = nc.getByteBuffer();
        long bytesRead = actualChannel.read(buf);

        System.out.println(new String(buf.array()));
        if(bytesRead > 0 && nc.isConnected()){
            otherKey.interestOps(SelectionKey.OP_WRITE);
        }

        /*
        if(bytesRead == -1){
            if(buf.hasRemaining()){
                nc.setTerminated();
                key.interestOps(0);
            }else{
                System.out.println("-*- Closing Channels (read)-*-");
                actualChannel.close();
                otherKey.channel().close();
            }
        }*/

        /*
        if(bytesRead == -1){
            System.out.println("READ DONE");
            actualChannel.close();
            key.cancel();
        }*/

        if(bytesRead == -1){
            SocketChannel otherChannel = (SocketChannel) nc.getSelectionKey().channel();
            if(buf.hasRemaining()){
                buf.flip();
                while(buf.hasRemaining())
                    otherChannel.write(buf);
            }
            System.out.println("-*- Closing Channels -*-");
            actualChannel.close();
            otherChannel.close();
            nc.getSelectionKey().cancel();
            key.cancel();
        }


    }

    public void handleWrite(SelectionKey key) throws IOException {
        System.out.println("---Handling Write---");

        NioConnection nc = (NioConnection) key.attachment();
        SocketChannel actualChannel = (SocketChannel) key.channel();
        ByteBuffer buf = nc.getByteBuffer();
        buf.flip();
        actualChannel.write(buf);
        /*if (!buf.hasRemaining()) {
            if(nc.isTerminated()){
                System.out.println("-*- Closing Channels (write) -*-");
                actualChannel.close();
                nc.getSelectionKey().channel().close();
            }else{
                key.interestOps(SelectionKey.OP_READ);
            }

        }*/

        /*
        if(!buf.hasRemaining()){
            if(!nc.prueba()){
                System.out.println("wRITE DONE");
                actualChannel.close();
                key.cancel();
            }else{
                key.interestOps(SelectionKey.OP_READ);
            }
        }*/

        if (!buf.hasRemaining()) {
            key.interestOps(SelectionKey.OP_READ);
        }
        buf.compact();

    }
}
