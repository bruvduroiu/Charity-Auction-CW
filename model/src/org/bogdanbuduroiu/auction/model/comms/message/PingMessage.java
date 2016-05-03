package org.bogdanbuduroiu.auction.model.comms.message;

import org.bogdanbuduroiu.auction.model.User;

/**
 * Created by bogdanbuduroiu on 30.04.16.
 */
public class PingMessage extends Message {

    private static final MessageType TYPE = MessageType.PING;

    public PingMessage() {
        super(TYPE);
    }
}
