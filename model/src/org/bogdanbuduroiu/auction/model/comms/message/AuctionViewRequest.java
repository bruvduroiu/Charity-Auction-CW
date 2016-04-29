package org.bogdanbuduroiu.auction.model.comms.message;

import org.bogdanbuduroiu.auction.model.User;

import java.io.Serializable;

/**
 * Created by bogdanbuduroiu on 23.04.16.
 */
public class AuctionViewRequest extends Message implements Serializable {
    public AuctionViewRequest(User sender) {
        super(sender, MessageType.AUCTION_VIEW_REQUEST);
    }
}
