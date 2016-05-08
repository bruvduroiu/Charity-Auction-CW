package org.bogdanbuduroiu.auction.client.controller;



import org.bogdanbuduroiu.auction.model.comms.ChangeRequest;
import org.bogdanbuduroiu.auction.model.comms.message.*;

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
    private Map<SocketChannel, List<Message>> pendingData;
    private Map<SocketChannel, ResponseHandler> rspHandlers;
    private Map<Socket, SSLSocket> sslSocketMap;
    private Map<Socket, SSLSession> sslSessionMap;
    private static final int sslHandshakeTimeout = 30000;

    public Comms(Client client, int port) throws IOException {
        this(client, InetAddress.getByName("localhost"), port);
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

        this.sslSocketMap = new HashMap<>();
        this.sslSessionMap = new HashMap<>();
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

    private SocketChannel initiateConnection() throws IOException {
        if (this.socketChannel != null)
            return this.socketChannel;

        SocketChannel socketChannel = SocketChannel.open();
        Socket socket = socketChannel.socket();
        System.out.println("[CON]\tAttempting to connect to server...");
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress(this.host, this.port));

        while (!socketChannel.finishConnect()) {

        }

        synchronized (this.pendingChanges) {
            this.pendingChanges.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
        }

        this.registerSocket(socket, this.host.toString(), this.port, true);

        return socketChannel;
    }


    private void finishConnection(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        Socket socket = socketChannel.socket();

        try {
            socketChannel.finishConnect();
        }
        catch (IOException e) {
            System.out.println(e.getStackTrace());
            key.cancel();
            return;
        }

        this.registerSocket(socket, this.host.toString(), this.port, false);

        key.interestOps(SelectionKey.OP_WRITE);
    }

    private void send(Message message, ResponseHandler handler) throws IOException {

        SocketChannel socketChannel = this.initiateConnection();

        this.rspHandlers.put(socketChannel, handler);

        synchronized (this.pendingData) {
            List queue = (List) this.pendingData.get(socketChannel);
            if (queue == null) {
                queue = new ArrayList<>();
                this.pendingData.put(socketChannel, queue);
            }
            queue.add(message);
        }
        synchronized (this.pendingChanges){
            pendingChanges.add(new ChangeRequest(socketChannel, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));
        }

        this.selector.wakeup();
    }

    protected void write(SelectionKey key) throws IOException {
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

                oos.flush();
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

    private ByteBuffer dataByteBuffer = null;

    private void read(SelectionKey key) throws IOException, ClassNotFoundException {
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
                this.handleResponse(socketChannel, message);
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

    private void handleResponse(SocketChannel socketChannel, Message message) throws IOException{
        ResponseHandler handler = this.rspHandlers.get(socketChannel);

        if (handler.handleResponse(message)) {
            socketChannel.close();
            socketChannel.keyFor(this.selector).cancel();
        }
    }

    public void sendMessage(Message message, ResponseHandler handler) throws  IOException {
        this.send(message, handler);
    }
}

