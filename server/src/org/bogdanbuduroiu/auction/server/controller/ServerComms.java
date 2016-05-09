package org.bogdanbuduroiu.auction.server.controller;


import org.bogdanbuduroiu.auction.model.comms.ChangeRequest;
import org.bogdanbuduroiu.auction.model.comms.message.Message;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.*;
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
    private Map<SocketChannel, List<byte[]>> pendingData;
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
                        else if (changeRequest.type == ChangeRequest.REGISTER)
                            changeRequest.socket.register(this.selector, SelectionKey.OP_CONNECT);
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
            try (ObjectOutputStream oos = new ObjectOutputStream(os)) {
                oos.flush();

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
            key.channel().configureBlocking(false);
            synchronized (this.pendingChanges) {
                this.pendingChanges.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_READ));
            }
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
        Socket socket = socketChannel.socket();
        socketChannel.configureBlocking(false);

        this.registerSocket(socket, this.host, this.PORT, false);

        //TODO: Document SSL please
        socketChannel.register(this.selector, SelectionKey.OP_READ);
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

    public void receiveMessage(SelectionKey key) throws IOException, ClassNotFoundException{


        SocketChannel socketChannel = (SocketChannel) key.channel();
        Socket socket = socketChannel.socket();

        SSLSocket sslSocket = this.sslSocketMap.get(socket);
        key.cancel();
        key.channel().configureBlocking(true);

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
                server.processMessage(socketChannel, message);
            }
        } finally {
            key.channel().configureBlocking(false);
            // Queue a channel reregistration
            synchronized(this.pendingChanges) {
                this.pendingChanges.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
            }
        }
    }

    private void configureSSLSocket(Socket socket, SSLSocket sslSocket) throws IOException {
        if (!this.sslSessionMap.containsKey(socket)) {
            sslSocket.setSoTimeout(this.sslHandshakeTimeout);

            SSLSession session = sslSocket.getSession();
            this.sslSessionMap.put(socket, session);

            if (session.isValid())
                System.out.println("[CON]\tSSL session details: " + session);

            else if (sslSocket.getUseClientMode())
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
