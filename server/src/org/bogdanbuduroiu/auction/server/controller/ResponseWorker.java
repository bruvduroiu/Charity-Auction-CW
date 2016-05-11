package org.bogdanbuduroiu.auction.server.controller;

import org.bogdanbuduroiu.auction.model.comms.message.*;
import org.bogdanbuduroiu.auction.server.model.ServerMessageEvent;

import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by bogdanbuduroiu on 29.04.16.
 */
public class ResponseWorker implements Runnable {
    private List<ServerMessageEvent> queue = new LinkedList<>();
    private Server server;

    public void queueResponse(Server server, SocketChannel socketChannel, Message message) {
        this.server = server;
        synchronized (queue) {
            queue.add(new ServerMessageEvent(socketChannel, message));
            queue.notify();
        }
    }


    @Override
    public void run() {
        ServerMessageEvent messageEvent;

        while(true) {
            synchronized (queue) {
                while (queue.isEmpty()) {
                    try {
                        queue.wait();
                    }
                    catch (InterruptedException e) {}
                }
                messageEvent = queue.remove(0);
                try {
                    server.commsWorker.sendMessage(messageEvent.socket, messageEvent.message);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
