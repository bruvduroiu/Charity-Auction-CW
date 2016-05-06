package org.bogdanbuduroiu.auction.model.comms.message;

import org.bogdanbuduroiu.auction.model.Item;
import org.bogdanbuduroiu.auction.model.User;

/**
 * Created by bogdanbuduroiu on 06.05.16.
 */
public class AuctionFailedNotification extends Message {

    private Item auction;

    public AuctionFailedNotification(User sender, Item auction) {
        super(sender, MessageType.AUCTION_FAIL_NOTIFICATION);
        this.auction = auction;
    }

    public Item getAuction() {
        return auction;
    }
}
