package org.bogdanbuduroiu.auction.model.comms.message;

import org.bogdanbuduroiu.auction.model.User;

import java.io.Serializable;

/**
 * Created by bogdanbuduroiu on 21.04.16.
 */
public abstract class Message implements Serializable {

    private final MessageType TYPE;

    public Message(MessageType TYPE) {
        this.TYPE = TYPE;
    }

    public MessageType type() {
        return TYPE;
    }
}
