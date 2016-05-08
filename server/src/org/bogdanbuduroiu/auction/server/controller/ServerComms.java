package org.bogdanbuduroiu.auction.server.controller;


import org.bogdanbuduroiu.auction.model.comms.ChangeRequest;
import org.bogdanbuduroiu.auction.model.comms.message.Message;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Created by bogdanbuduroiu on 28.04.16.
 */

//TODO: Add console messages
public class ServerComms implements Runnable {

    private Server server;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private final String host;
    private final int PORT = 8080;
    private List<ChangeRequest> pendingChanges;
    private Map<Integer, SelectionKey> clients;
    private Map<SocketChannel, List<Message>> pendingData;
    private Map<Socket, SSLSocket> sslSocketMap;
    private Map<Socket, SSLSession> sslSessionMap;
    private ByteBuffer data;
    private static final int sslHandshakeTimeout = 30000;


    public ServerComms(Server server) throws IOException {
        this.server = server;
        System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][SRV]\tInitiating Communication Channel...");
        selector = this.initSelector();

        this.host = InetAddress.getByName("localhost").toString();

        this.sslSocketMap = new HashMap<>();
        this.sslSessionMap = new HashMap<>();

        this.pendingChanges = new LinkedList<>();
        this.pendingData = new HashMap<>();
        this.data = ByteBuffer.allocate(1024);
        System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][SRV]\tServerComms channel initiated.");
    }

    private Selector initSelector() throws IOException{
        Selector socketSelector = SelectorProvider.provider().openSelector();

        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(this.PORT));
        serverSocketChannel.configureBlocking(false);

        serverSocketChannel.register(socketSelector, SelectionKey.OP_ACCEPT);

        return socketSelector;
    }

    private void send(SocketChannel socketChannel, Message message) {
        synchronized (this.pendingChanges) {
            this.pendingChanges.add(new ChangeRequest(socketChannel, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));

            synchronized (this.pendingData) {
                List queue = (List) this.pendingData.get(socketChannel);

                if (queue == null) {
                    queue = new ArrayList<>();
                    this.pendingData.put(socketChannel, queue);
                }

                queue.add(message);
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
        Socket socket = socketChannel.socket();

        try {
            SSLSocket sslSocket = this.sslSocketMap.get(socket);
            key.cancel();
            key.channel().configureBlocking(true);

            this.configureSSLSocket(socket, sslSocket);

            OutputStream os = sslSocket.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(os);
            oos.flush();

            synchronized (this.pendingData) {
                List queue = this.pendingData.get(socketChannel);

                while (!queue.isEmpty()) {
                    oos.writeObject(queue.get(0));
                    queue.remove(0);
                }

                oos.close();

            }
            key.channel().configureBlocking(false);
            this.queueRegistration(socketChannel);
        }
        catch (SSLException e) {
            throw e;
        }
        catch (IOException e) {
            this.deregisterSocket(socket);
            key.cancel();
            socketChannel.close();
        }

    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);

        Socket socket = socketChannel.socket();

        this.registerSocket(socket, this.host, this.PORT, false);

        //TODO: Document SSL please
        socketChannel.register(this.selector, SelectionKey.OP_READ);
    }

    /**
     * Implementation of the Object Serializer/Deserialized using ByteArrayOutputStream
     * taken from: http://stackoverflow.com/questions/5862971/java-readobject-with-nio
     */
    public void sendMessage(SocketChannel socketChannel, Message message) throws IOException {
        this.send(socketChannel, message);
    }

    private ByteBuffer dataByteBuffer = null;

    public void receiveMessage(SelectionKey key) throws IOException, ClassNotFoundException{


        SocketChannel socketChannel = (SocketChannel) key.channel();
        Socket socket = socketChannel.socket();

        SSLSocket sslSocket = this.sslSocketMap.get(socket);
        key.cancel();
        key.channel().configureBlocking(true);

        this.configureSSLSocket(socket, sslSocket);

        InputStream is = sslSocket.getInputStream();
        ObjectInputStream ois = new ObjectInputStream(is);
        Message message = null;
        try {
            message = (Message) ois.readObject();
            ois.close();
        }
        catch (SocketTimeoutException e) {
            message = null;
        }
        catch (IOException e) {
            this.deregisterSocket(socket);
            return;
        }

        if (message == null) {
            System.out.println("[CON]\tServer has closed connection.");
            this.deregisterSocket(socket);
            sslSocket.close();
        }

        try {
            if (message != null)
                server.processMessage(socketChannel, message);
        }
        finally {
            key.channel().configureBlocking(false);
            this.queueRegistration(socketChannel);
        }

    }

    private void queueRegistration(SocketChannel socketChannel) {
        synchronized (this.pendingChanges) {
            this.pendingChanges.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
        }
    }

    private void configureSSLSocket(Socket socket, SSLSocket sslSocket) throws IOException {
        if (!this.sslSessionMap.containsKey(socket)) {
            sslSocket.setSoTimeout(this.sslHandshakeTimeout);

            SSLSession session = sslSocket.getSession();
            this.sslSessionMap.put(socket, session);

            if (session.isValid())
                System.out.println("[CON]\tSSL session details: " + session);

            else
            if (sslSocket.getUseClientMode())
                throw new SSLException("[CON]\tSSL Handshake failed!");
        }

        sslSocket.setSoTimeout(1);
    }

    protected void registerSocket(Socket socket, String host, int port, boolean client) throws IOException {
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket) factory.createSocket(socket, host, port, true);

        sslSocket.setUseClientMode(client);

        this.sslSocketMap.put(socket, sslSocket);
    }

    protected void deregisterSocket(Socket socket) {
        this.sslSocketMap.remove(socket);
        this.sslSessionMap.remove(socket);
    }

}
