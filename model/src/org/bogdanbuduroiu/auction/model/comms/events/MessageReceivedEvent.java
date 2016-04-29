package org.bogdanbuduroiu.auction.model.comms.events;

import org.bogdanbuduroiu.auction.model.comms.message.Message;

import java.util.EventObject;

/**
 * Created by bogdanbuduroiu on 28.04.16.
 */
public class MessageReceivedEvent extends EventObject {

    private Message message;


    public MessageReceivedEvent(Object source, Message message) {
        super(source);
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
}
