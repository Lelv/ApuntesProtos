package original;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * Created by lelv on 5/20/16.
 */
public class PopTest {

    public static void main(String[] args) {
        //testTwo();



        try{
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.socket().bind(new InetSocketAddress(7070));
            ssc.configureBlocking(false);

            Selector selector = Selector.open();
            ssc.register(selector, SelectionKey.OP_ACCEPT);


            while (true) { // Run forever, processing available I/O operations
                // Wait for some channel to be ready (or timeout)
                //System.out.println("manzana");
                int cant = selector.select();
                if (cant == 0) { // returns # of ready chans
                    //System.out.print(".");
                    continue;
                }

                System.out.println("Selecciono: " + cant);
                // Get iterator on set of keys with I/O to process
                Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
                while (keyIter.hasNext()) {
                    SelectionKey key = keyIter.next(); // Key is bit mask
                    // Server socket channel has pending connection requests?
                    if (key.isAcceptable()) {
                        System.out.println("ACCEPT");

                        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

                        SocketChannel socketChannelClient = ((ServerSocketChannel) key.channel()).accept();
                        socketChannelClient.configureBlocking(false);

                        NioConnection ncClient = new NioConnection(byteBuffer);
                        SelectionKey clientKey = socketChannelClient.register(selector, SelectionKey.OP_READ, ncClient);

                        SocketChannel sc = SocketChannel.open();
                        sc.configureBlocking(false);
                        sc.connect(new InetSocketAddress("localhost", 110));

                        SelectionKey otherKey = sc.register(selector, SelectionKey.OP_CONNECT, new NioConnection(clientKey, byteBuffer));

                        ncClient.setSelectionKey(otherKey);

                    }

                    if(key.isConnectable()){
                        System.out.println("CONNECT");
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        if(socketChannel.finishConnect()){
                            ((NioConnection)key.attachment()).setConnected(true);
                            key.interestOps(SelectionKey.OP_READ);
                            //key.interestOps(SelectionKey.OP_WRITE);
                        }
                    }
                    // Client socket channel has pending data?
                    if (key.isReadable()) {
                        System.out.println("READ");
                        NioConnection nc = (NioConnection) key.attachment();

                        SelectionKey otherKey = nc.getSelectionKey();
                        SocketChannel actualChannel = (SocketChannel) key.channel();

                        ByteBuffer buf = nc.getByteBuffer();
                        long bytesRead = actualChannel.read(buf);

                        //if (bytesRead == -1) { // Did the other end close?
                        //    actualChannel.close();
                        //}/* else if (bytesRead > 0) {
                            // Indicate via key that reading/writing are both of interest now.
                         //   key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        //}*/

                        System.out.println(new String(buf.array()));
                        if(bytesRead > 0 && ((NioConnection)otherKey.attachment()).isConnected()){
                            //otherChannel.write(buf);
                            otherKey.interestOps(SelectionKey.OP_WRITE);

                        }
                        if(bytesRead == -1){
                            System.out.println("CERRAR");
                            actualChannel.close();
                            otherKey.channel().close();
                        }
                    }
                    // Client socket channel is available for writing and
                    // key is valid (i.e., channel not closed)?
                    if (key.isValid() && key.isWritable()) {
                        System.out.println("WRITE");
                        //buf.flip();
                        NioConnection nc = (NioConnection) key.attachment();
                        SocketChannel actualChannel = (SocketChannel) key.channel();
                        ByteBuffer buf = nc.getByteBuffer();
                        buf.flip();
                        SelectionKey otherKey = nc.getSelectionKey();
                        actualChannel.write(buf);
                        if (!buf.hasRemaining()) { // Buffer completely written?
                            // Nothing left, so no longer interested in writes
                            key.interestOps(SelectionKey.OP_READ);
                        }
                        buf.compact(); // Make room for more data to be read in
                    }
                    keyIter.remove(); // remove from set of selected keys
                }
            }


        }catch(Exception e){
            e.printStackTrace();
        }
    }

        public static void testTwo(){
        try{
            SocketChannel sc = SocketChannel.open();
            sc.connect(new InetSocketAddress("localhost", 8000));
            String x = "piola";
            ByteBuffer bf = ByteBuffer.wrap(x.getBytes());
            sc.write(bf);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
