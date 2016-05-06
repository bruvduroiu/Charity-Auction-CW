package org.bogdanbuduroiu.auction.model.comms.message;

import org.bogdanbuduroiu.auction.model.Item;
import org.bogdanbuduroiu.auction.model.User;

/**
 * Created by bogdanbuduroiu on 06.05.16.
 */
public class AuctionWonNotification extends Message {

    private Item auction;

    public AuctionWonNotification(User sender, Item auction) {
        super(sender, MessageType.AUCTION_WON_NOTIFICATION);
        this.auction = auction;
    }

    public Item getAuction() {
        return this.auction;
    }
}
