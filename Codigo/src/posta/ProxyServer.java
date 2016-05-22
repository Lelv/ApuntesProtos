package posta;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by lelv on 5/21/16.
 */
public class ProxyServer implements Runnable{
    private final Selector selector;
    private final ServerSocketChannel serverSocketChannel;

    private static final long TIMEOUT = 5000;

    public ProxyServer(int port) throws IOException {
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void run(){

        Handler handler = new Handler();
        try{
            while(true){

                if(selector.select(TIMEOUT) == 0){
                    //hacer algo piola
                    continue;
                }
                Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();

                while (keyIter.hasNext()) {
                    SelectionKey key = keyIter.next();
                    keyIter.remove();

                    if(!key.isValid()){
                        System.out.println("NO VALIDO");
                        continue;
                    }

                    if(key.isAcceptable()){
                        handler.handleAccept(key);
                    }else if(key.isConnectable()){
                        handler.handleConnect(key);
                    }else if (key.isReadable()){
                        handler.handleRead(key);
                    }else if (key.isValid() && key.isWritable()){
                        handler.handleWrite(key);
                    }

                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try{
            new Thread(new ProxyServer(7070)).run();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
