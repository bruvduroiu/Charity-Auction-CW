package org.bogdanbuduroiu.auction.model.comms;



import org.bogdanbuduroiu.auction.model.comms.events.MessageReceivedEvent;
import org.bogdanbuduroiu.auction.model.comms.message.Message;
import org.bogdanbuduroiu.auction.model.comms.events.MessageReceivedListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

/**
 * Created by bogdanbuduroiu on 21.04.16.
 */
//TODO: Add console messages
public class Comms implements Runnable {

    private InetAddress host;
    private int port;
    private Selector selector;
    private ByteBuffer data;
    private List<ChangeRequest> pendingChanges;
    private Map<SocketChannel, List<byte[]>> pendingData;
    private SocketChannel socketChannel;
    //TODO: Implement RspHandlers
    private MessageReceivedListener messageReceivedListener = null;

    public Comms(int port) throws IOException {
        this(InetAddress.getLocalHost(), port);
    }

    public Comms(InetAddress host, int port) throws IOException {
        this.host = host;
        this.port = port;
        this.selector = this.initSelector();
        this.data = ByteBuffer.allocate(8192);
        this.pendingChanges = new LinkedList<>();
        this.pendingData = new HashMap<>();
    }

    private Selector initSelector() throws IOException {
        return SelectorProvider.provider().openSelector();
    }

    @Override
    public void run() {
        while (true) {
            try {

                synchronized (this.pendingChanges) {
                    Iterator changes = this.pendingChanges.iterator();

                    while(changes.hasNext()) {
                        ChangeRequest changeRequest = (ChangeRequest) changes.next();

                        if (changeRequest.type == ChangeRequest.CHANGEOPS) {
                            SelectionKey key = changeRequest.socket.keyFor(this.selector);
                            key.interestOps(changeRequest.ops);
                        }
                        else if (changeRequest.type == ChangeRequest.REGISTER)
                            changeRequest.socket.register(this.selector, changeRequest.ops);
                    }
                    this.pendingChanges.clear();
                }
                int num = this.selector.select();

                if (num == 0) continue;

                Iterator selectedKeys = this.selector.selectedKeys().iterator();

                while (selectedKeys.hasNext()) {
                    SelectionKey key = (SelectionKey) selectedKeys.next();
                    selectedKeys.remove();

                    if (!key.isValid()) continue;

                    if (key.isConnectable())
                        this.finishConnection(key);
                    else if (key.isReadable())
                        this.read(key);
                    else if (key.isWritable())
                        this.write(key);

                }

            }
            catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void initiateConnection() throws IOException {
        if (this.socketChannel != null)
            return;
        this.socketChannel = SocketChannel.open();
        System.out.println("[CON]\tAttempting to connect to server...");
        socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress(this.host, this.port));

        while (!socketChannel.finishConnect()) {}

        synchronized (this.pendingChanges) {
            this.pendingChanges.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
        }
    }


    private void finishConnection(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        try {
            socketChannel.finishConnect();
        }
        catch (IOException e) {
            System.out.println(e.getStackTrace());
            key.cancel();
            return;
        }

        key.interestOps(SelectionKey.OP_WRITE);
    }

    private void send(byte[] data) throws IOException {
        initiateConnection();

        synchronized (this.pendingData) {
            List queue = (List) this.pendingData.get(this.socketChannel);
            if (queue == null) {
                queue = new ArrayList();
                this.pendingData.put(this.socketChannel, queue);
            }
            queue.add(ByteBuffer.wrap(data));
        }
        synchronized (this.pendingChanges){
            pendingChanges.add(new ChangeRequest(this.socketChannel, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));
        }

        this.selector.wakeup();
    }

    protected void read(SelectionKey key) throws IOException{
        SocketChannel socketChannel = (SocketChannel) key.channel();

        this.data.clear();

        int numRead;
        try {
            numRead = socketChannel.read(this.data);
            System.out.println("[I/O]\tReceived bytes from: " + socketChannel.socket().getInetAddress() + ". Printing:");
            data.flip();
        }
        catch (IOException e) {
            key.cancel();
            socketChannel.close();
            return;
        }

        if (numRead == -1) {
            System.out.println("[CON]\tServer has closed connection.");
            key.channel().close();
            key.cancel();
            return;
        }

        System.out.println("[MSG]\t");
        while (data.hasRemaining())
            System.out.print((char) data.get());
        System.out.println("\n");
    }

    protected void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        synchronized (this.pendingData) {
            List queue = (List) this.pendingData.get(socketChannel);

            while (!queue.isEmpty()) {
                ByteBuffer buffer = (ByteBuffer) queue.get(0);
                System.out.println("[MSG]\tSending: ");
                while (buffer.hasRemaining())
                    System.out.print((char) buffer.get());
                buffer.rewind();
                socketChannel.write(buffer);
                if (buffer.remaining() > 0)
                    break;
                queue.remove(0);
            }

            if (queue.isEmpty()) {
                key.interestOps(SelectionKey.OP_READ);
                this.data.flip();
            }
        }
    }

    public void sendMessage(Message message) throws  IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for(int i=0;i<4;i++) baos.write(0);
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(message);
        oos.close();
        final ByteBuffer wrap = ByteBuffer.wrap(baos.toByteArray());
        wrap.putInt(0, baos.size()-4);
        this.send(wrap.array());
    }

    private void receiveMessage(Message message) {
        //TODO: Implement object deserialization
    }

    public void addMessageReceivedListener(MessageReceivedListener li) {
        messageReceivedListener = li;
    }

    public void removeMessageReceivedListener() {
        messageReceivedListener = null;
    }

}

