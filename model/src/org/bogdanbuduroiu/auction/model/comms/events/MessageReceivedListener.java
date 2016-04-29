package org.bogdanbuduroiu.auction.model.comms.events;

import org.bogdanbuduroiu.auction.model.comms.message.Message;

/**
 * Created by bogdanbuduroiu on 28.04.16.
 */
public interface MessageReceivedListener {
    void messageReceived(MessageReceivedEvent messageReceivedEvent);
}
