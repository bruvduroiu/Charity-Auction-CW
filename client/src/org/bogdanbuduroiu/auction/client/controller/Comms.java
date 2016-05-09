package org.bogdanbuduroiu.auction.client.controller;



import org.bogdanbuduroiu.auction.model.comms.ChangeRequest;
import org.bogdanbuduroiu.auction.model.comms.message.*;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

/**
 * Created by bogdanbuduroiu on 21.04.16.
 */
public class Comms implements Runnable {

    private Client client;
    private InetAddress host;
    private int port;
    private Selector selector;
    private ByteBuffer data;
    private SocketChannel socketChannel;
    private List<ChangeRequest> pendingChanges;
    private Map<SocketChannel, List<byte[]>> pendingData;
    private Map<SocketChannel, ResponseHandler> rspHandlers;

    public Comms(Client client, int port) throws IOException {
        this(client, InetAddress.getLocalHost(), port);
    }

    public Comms(Client client, InetAddress host, int port) throws IOException {
        this.client = client;
        this.host = host;
        this.port = port;
        this.selector = this.initSelector();
        this.data = ByteBuffer.allocate(8192);
        this.pendingChanges = new LinkedList<>();
        this.pendingData = new HashMap<>();
        this.rspHandlers = Collections.synchronizedMap(new HashMap<>());

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
                            changeRequest.socket.register(this.selector, changeRequest.ops, client.getCurrentUser());
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

    private SocketChannel initiateConnection() throws IOException {
        if (this.socketChannel != null)
            return this.socketChannel;

        SocketChannel socketChannel = SocketChannel.open();
        System.out.println("[CON]\tAttempting to connect to server...");
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress(this.host, this.port));

        while (!socketChannel.finishConnect()) {

        }

        synchronized (this.pendingChanges) {
            this.pendingChanges.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
        }

        return socketChannel;
    }


    private void finishConnection(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        Socket socket = socketChannel.socket();

        try {
            key.attach(client.getCurrentUser());
            socketChannel.finishConnect();
        }
        catch (IOException e) {
            System.out.println(e.getStackTrace());
            key.cancel();
            return;
        }

        key.interestOps(SelectionKey.OP_WRITE);
    }

    private void send(byte[] data, ResponseHandler handler) throws IOException {

        SocketChannel socketChannel = this.initiateConnection();

        this.rspHandlers.put(socketChannel, handler);

        synchronized (this.pendingData) {
            List queue = (List) this.pendingData.get(socketChannel);
            if (queue == null) {
                queue = new ArrayList();
                this.pendingData.put(socketChannel, queue);
            }
            queue.add(ByteBuffer.wrap(data));
        }
        synchronized (this.pendingChanges){
            pendingChanges.add(new ChangeRequest(socketChannel, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));
        }

        this.selector.wakeup();
    }

    protected void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        synchronized (this.pendingData) {
            List queue = this.pendingData.get(socketChannel);

            while (!queue.isEmpty()) {
                ByteBuffer buffer = (ByteBuffer) queue.get(0);
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
    private final ByteBuffer lengthByteBuffer = ByteBuffer.wrap(new byte[4]);
    private ByteBuffer dataByteBuffer = null;
    private boolean readLength = true;

    private void read(SelectionKey key) throws IOException, ClassNotFoundException {

        SocketChannel socketChannel = (SocketChannel) key.channel();
        key.channel().configureBlocking(false);


        int numRead;
        try {
            if (readLength) {
                numRead = socketChannel.read(lengthByteBuffer);
                if (lengthByteBuffer.remaining() == 0) {
                    readLength = false;
                    dataByteBuffer = ByteBuffer.allocate(lengthByteBuffer.getInt(0));
                    lengthByteBuffer.clear();
                }
            } else {
                numRead = socketChannel.read(dataByteBuffer);
                if (dataByteBuffer.remaining() == 0) {
                    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(dataByteBuffer.array()));
                    final Message message = (Message) ois.readObject();

                    dataByteBuffer = null;
                    readLength = true;

                    this.handleResponse(socketChannel, message);
                }
            }
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
    }

    private void handleResponse(SocketChannel socketChannel, Message message) throws IOException{
        ResponseHandler handler = this.rspHandlers.get(socketChannel);

        if (handler.handleResponse(message)) {
            socketChannel.close();
            socketChannel.keyFor(this.selector).cancel();
        }
    }

    public void sendMessage(Message message, ResponseHandler handler) throws  IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for(int i=0;i<4;i++) baos.write(0);
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(message);
        oos.close();
        final ByteBuffer wrap = ByteBuffer.wrap(baos.toByteArray());
        wrap.putInt(0, baos.size()-4);
        this.send(wrap.array(), handler);
    }
}

