package debug;

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
        System.out.println("---Handling Read--- " + key.toString());
        NioConnection nc = (NioConnection) key.attachment();

        SelectionKey otherKey = nc.getSelectionKey();
        SocketChannel actualChannel = (SocketChannel) key.channel();

        ByteBuffer buf = nc.getByteBuffer();
        long bytesRead = actualChannel.read(buf);

        System.out.println(new String(buf.array()));
        if(bytesRead > 0 && nc.isConnected()){
            otherKey.interestOps(SelectionKey.OP_WRITE);
        }


        if(bytesRead == -1){
            actualChannel.close();
            if(buf.hasRemaining()){
                System.out.println("Remaining: " + buf.remaining());
                System.out.println("Position: " + buf.position());
                System.out.println("Limit: " + buf.limit());
                nc.setTerminated();
                System.out.println("CANELO KEY: " + key.toString());
                key.cancel();
                otherKey.interestOps(SelectionKey.OP_WRITE);
            }else{
                System.out.println("-*- Closing Channels (read)-*-");
                otherKey.channel().close();
                otherKey.cancel();

            }

        }

        /*
        if(bytesRead == -1){
            System.out.println("READ DONE");
            actualChannel.close();
            key.cancel();
        }*/

        /*
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
        }*/


    }

    public void handleWrite(SelectionKey key) throws IOException {
        System.out.println("---Handling Write--- " + key.toString());

        NioConnection nc = (NioConnection) key.attachment();
        SocketChannel actualChannel = (SocketChannel) key.channel();
        ByteBuffer buf = nc.getByteBuffer();
        buf.flip();
        actualChannel.write(buf);

        if (!buf.hasRemaining()) {
            System.out.println("!BUF hAS REMAINING");
            System.out.println("IS terminated: " + nc.isTerminated());
            if(nc.isTerminated()){
                System.out.println("-*- Closing Channels (write) -*-");
                key.cancel();
                actualChannel.close();
            }else{
                key.interestOps(SelectionKey.OP_READ);
            }

        }

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

        /*
        if (!buf.hasRemaining()) {
            key.interestOps(SelectionKey.OP_READ);
            //ACA IRIA LLAMADO AL POOL DE THREADS PARA QUE TRABAJE
            //Y DESPUES ÉL SETEARIA EL KEY PARA QUE SEA LEIDO
        }*/

        buf.compact();
    }
}
