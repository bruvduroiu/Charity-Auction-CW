package org.bogdanbuduroiu.auction.client.controller;



import org.bogdanbuduroiu.auction.model.comms.ChangeRequest;
import org.bogdanbuduroiu.auction.model.comms.message.*;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.*;
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
    private InetSocketAddress host;
    private Selector selector;
    private ByteBuffer data;
    private List<ChangeRequest> pendingChanges;
    private Map<SocketChannel, List<byte[]>> pendingData;
    private Map<SocketChannel, ResponseHandler> rspHandlers;
    private Map<Socket, SSLSocket> sslSocketMap;
    private Map<Socket, SSLSession> sslSessionMap;
    private static final int sslHandshakeTimeout = 30000;

    public Comms(Client client, int port) throws IOException {
        this(client, new InetSocketAddress(InetAddress.getByName("localhost"), 8080));
    }

    public Comms(Client client, InetSocketAddress host) throws IOException {
        this.client = client;
        this.host = host;
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

        SocketChannel socketChannel = SocketChannel.open();
        Socket socket = socketChannel.socket();
        System.out.println("[CON]\tAttempting to connect to server...");
        socketChannel.configureBlocking(false);
        socketChannel.connect(this.host);

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
        Socket socket = socketChannel.socket();

        try {
            SSLSocket sslSocket = this.sslSocketMap.get(socket);
            key.cancel();
            key.channel().configureBlocking(true);

            this.configureSSLSocket(socket, sslSocket);

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

    private final ByteBuffer lengthByteBuffer = ByteBuffer.wrap(new byte[4]);
    private ByteBuffer dataByteBuffer = null;
    private boolean readLength = true;

    private void read(SelectionKey key) throws IOException, ClassNotFoundException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        Socket socket = socketChannel.socket();

        SSLSocket sslSocket = this.sslSocketMap.get(socket);
        key.cancel();
        socketChannel.configureBlocking(true);

        this.configureSSLSocket(socket, sslSocket);
        InputStream is = sslSocket.getInputStream();
        int numRead;
        try {
            numRead = is.read(data.array(), 0, data.array().length);
        } catch (SocketTimeoutException e) {
            // The read timed out so we're done.
            numRead = 0;
        } catch (IOException e) {
            this.deregisterSocket(socket);
            // The remote entity probably forcibly closed the connection.
            // Nothing to see here. Move on.
            // No need to cancel, already done
            return;
        }

        if (numRead == -1) {
            // Don't queue a cancellation since we have alread cancelled the
            // channel's registration. Just close the socket.
            this.deregisterSocket(socket);
            sslSocket.close();
            // The caller needs to be notifed. Although this is
            // a "clean" close from the caller's perspective this
            // is unexpected. So we manufacture an exception.
        }

        try {
            if (numRead > 0) {
                // Hand the data off to our worker thread
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data.array()));
                Message message = (Message) ois.readObject();
                ois.close();
                this.handleResponse(socketChannel, message);
            }
        } finally {
            key.channel().configureBlocking(false);
            // Queue a channel reregistration
            synchronized(this.pendingChanges) {
                this.pendingChanges.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
            }
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

        sslSocket.addHandshakeCompletedListener((e) -> {
            try {
                sslSocket.setSoTimeout(1);
            } catch (SocketException e1) {
                e1.printStackTrace();
            }
        });
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

