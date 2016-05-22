package original;

import com.sun.org.apache.xpath.internal.operations.Bool;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Created by lelv on 5/20/16.
 */
public class NioConnection {

    public SelectionKey selectionKey;
    public ByteBuffer byteBuffer;
    public boolean connected;


    public NioConnection(ByteBuffer bb){
        this.selectionKey = null;
        this.byteBuffer = bb;
        this.connected = true;
    }

    public NioConnection(SelectionKey sk, ByteBuffer bb){
        this.selectionKey = sk;
        this.byteBuffer = bb;
        this.connected = false;
    }

    public NioConnection(SelectionKey sk, ByteBuffer bb, boolean connected){
        this.selectionKey = sk;
        this.byteBuffer = bb;
        this.connected = connected;
    }

    public SelectionKey getSelectionKey(){
        return selectionKey;
    }

    public void setSelectionKey(SelectionKey sk){
        this.selectionKey = sk;
    }

    public ByteBuffer getByteBuffer(){
        return this.byteBuffer;
    }

    public boolean isConnected(){
        return connected;
    }

    public void setConnected(boolean connected){
        this.connected = connected;
    }
}
