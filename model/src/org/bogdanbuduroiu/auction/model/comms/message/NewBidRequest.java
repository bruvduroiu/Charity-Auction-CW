package org.bogdanbuduroiu.auction.model.comms.message;

import org.bogdanbuduroiu.auction.model.User;

import java.io.Serializable;

/**
 * Created by bogdanbuduroiu on 23.04.16.
 */
public class NewBidRequest extends Message implements Serializable {
    public NewBidRequest(User sender) {
        super(sender, MessageType.NEW_BID_REQUEST);
    }
}
