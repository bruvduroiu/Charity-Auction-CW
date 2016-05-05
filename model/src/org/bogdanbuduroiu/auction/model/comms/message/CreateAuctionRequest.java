package org.bogdanbuduroiu.auction.model.comms.message;

import org.bogdanbuduroiu.auction.model.Item;
import org.bogdanbuduroiu.auction.model.User;

import java.io.Serializable;

/**
 * Created by bogdanbuduroiu on 23.04.16.
 */
public class CreateAuctionRequest extends Message implements Serializable {

    private Item auction;

    public CreateAuctionRequest(User sender, Item auction) {
        super(sender, MessageType.CREATE_AUCTION_REQUEST);
        this.auction = auction;
    }

    public Item getAuction() {
        return auction;
    }
}
