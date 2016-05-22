package posta;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

/**
 * Created by lelv on 5/20/16.
 */
public class NioConnection {

    private SelectionKey selectionKey;
    private ByteBuffer byteBuffer;
    private boolean connected;
    private boolean terminated;


    public NioConnection(ByteBuffer bb){
        this.selectionKey = null;
        this.byteBuffer = bb;
        this.connected = true;
        this.terminated = false;
    }

    public NioConnection(SelectionKey sk, ByteBuffer bb){
        this.selectionKey = sk;
        this.byteBuffer = bb;
        this.connected = false;
        this.terminated = false;
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

    public void setTerminated(){
        ((NioConnection)this.selectionKey.attachment()).terminate();
    }

    private void terminate(){
        System.out.println("Seteo terminated");
        this.terminated = true;
    }

    public boolean isTerminated(){
        return this.terminated;
    }

    public boolean prueba(){
        return selectionKey.channel().isOpen();
    }
}
