package org.bogdanbuduroiu.auction.server.model;

import org.bogdanbuduroiu.auction.model.comms.message.Message;

import java.nio.channels.SocketChannel;

/**
 * Created by bogdanbuduroiu on 29.04.16.
 */
public class ServerMessageEvent {

    public SocketChannel socket;
    public Message message;

    public ServerMessageEvent(SocketChannel socket, Message message) {
        this.socket = socket;
        this.message = message;
    }
}
