package org.bogdanbuduroiu.auction.model.comms.message;

import org.bogdanbuduroiu.auction.model.User;

import java.io.Serializable;

/**
 * Created by bogdanbuduroiu on 21.04.16.
 */
public abstract class Message implements Serializable {

    private User sender;
    private final MessageType TYPE;

    public Message(User sender, MessageType TYPE) {
        this.sender = sender;
        this.TYPE = TYPE;
    }

    public User getSender() {
        return sender;
    }

    public MessageType type() {
        return TYPE;
    }
}
