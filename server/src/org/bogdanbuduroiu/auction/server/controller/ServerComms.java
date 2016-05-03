package org.bogdanbuduroiu.auction.server.controller;


import org.bogdanbuduroiu.auction.model.comms.ChangeRequest;
import org.bogdanbuduroiu.auction.model.comms.message.Message;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

/**
 * Created by bogdanbuduroiu on 28.04.16.
 */

//TODO: Add console messages
public class ServerComms implements Runnable {

    private Server server;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private final int PORT = 8080;
    private List<ChangeRequest> pendingChanges;
    private Map<Integer, SelectionKey> clients;
    private Map<SocketChannel, List<byte[]>> pendingData;
    private ByteBuffer data;

    public ServerComms(Server server) throws IOException {
        this.server = server;
        System.out.println("[SRV]\tInitiating Communication Channel...");
        serverSocketChannel = ServerSocketChannel.open();
        selector = this.initSelector();
        this.pendingChanges = new LinkedList<>();
        this.pendingData = new HashMap<>();
        this.data = ByteBuffer.allocate(1024);
        System.out.println("[SRV]\tServerComms channel initiated.");
    }

    private Selector initSelector() throws IOException{
        Selector socketSelector = SelectorProvider.provider().openSelector();

        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(this.PORT));
        serverSocketChannel.configureBlocking(false);

        serverSocketChannel.register(socketSelector, SelectionKey.OP_ACCEPT);

        return socketSelector;
    }

    private void send(SocketChannel socketChannel, byte[] data) {
        synchronized (this.pendingChanges) {
            this.pendingChanges.add(new ChangeRequest(socketChannel, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));

            synchronized (this.pendingData) {
                List queue = (List) this.pendingData.get(socketChannel);

                if (queue == null) {
                    queue = new ArrayList<>();
                    this.pendingData.put(socketChannel, queue);
                }

                queue.add(ByteBuffer.wrap(data));
            }
        }
        this.selector.wakeup();
    }

    @Override
    public void run() {
        while (true) {
            try {
                synchronized (this.pendingChanges) {
                    Iterator changes = this.pendingChanges.iterator();
                    while (changes.hasNext()) {
                        ChangeRequest changeRequest = (ChangeRequest) changes.next();
                        if (changeRequest.type == ChangeRequest.CHANGEOPS) {
                            SelectionKey key = changeRequest.socket.keyFor(this.selector);
                            key.interestOps(changeRequest.ops);
                        }
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

                    if (key.isAcceptable())
                        this.accept(key);
                    else if (key.isReadable())
                        this.receiveMessage(key);
                    else if (key.isWritable())
                        this.write(key);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        synchronized (this.pendingData) {
            List queue = (List) this.pendingData.get(socketChannel);

            while (!queue.isEmpty()) {
                ByteBuffer buf = (ByteBuffer) queue.get(0);
                socketChannel.write(buf);
                if (buf.remaining() > 0)
                    break;

                queue.remove(0);
            }

            if (queue.isEmpty())
                key.interestOps(SelectionKey.OP_READ);
        }
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);

        socketChannel.register(this.selector, SelectionKey.OP_READ);

        if (socketChannel.keyFor(this.selector).attachment() != null)
            server.newClient(socketChannel.keyFor(this.selector).attachment(), socketChannel);
    }

    /**
     * Implementation of the Object Serializer/Deserialized using ByteArrayOutputStream
     * taken from: http://stackoverflow.com/questions/5862971/java-readobject-with-nio
     */
    public void sendMessage(SocketChannel socketChannel, Message message) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for(int i=0;i<4;i++) baos.write(0);
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(message);
        oos.close();
        final ByteBuffer wrap = ByteBuffer.wrap(baos.toByteArray());
        wrap.putInt(0, baos.size()-4);
        this.send(socketChannel, wrap.array());
    }

    private final ByteBuffer lengthByteBuffer = ByteBuffer.wrap(new byte[4]);
    private ByteBuffer dataByteBuffer = null;
    private boolean readLength = true;

    public void receiveMessage(SelectionKey key) throws IOException, ClassNotFoundException{

        SocketChannel socket = (SocketChannel) key.channel();

        int numRead;
        try {
            if (readLength) {
                numRead = socket.read(lengthByteBuffer);
                if (lengthByteBuffer.remaining() == 0) {
                    readLength = false;
                    dataByteBuffer = ByteBuffer.allocate(lengthByteBuffer.getInt(0));
                    lengthByteBuffer.clear();
                }
            } else {
                numRead = socket.read(dataByteBuffer);
                if (dataByteBuffer.remaining() == 0) {
                    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(dataByteBuffer.array()));
                    final Message message = (Message) ois.readObject();

                    dataByteBuffer = null;
                    readLength = true;

                    server.processMessage(socket, message);
                }
            }
        }
        catch (IOException e) {
            key.cancel();
            socket.close();
            return;
        }

//        if (numRead == -1) {
//            key.channel().close();
//            key.cancel();
//            return;
//        }
    }
}
