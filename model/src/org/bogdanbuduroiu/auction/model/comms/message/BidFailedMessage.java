package org.bogdanbuduroiu.auction.model.comms.message;

import org.bogdanbuduroiu.auction.model.Item;
import org.bogdanbuduroiu.auction.model.User;

/**
 * Created by bogdanbuduroiu on 02.05.16.
 */
public class BidFailedMessage extends ErrMessage {

    private final Item item;

    public BidFailedMessage(User sender, Item item) {
        super(sender, ErrType.INVALID_BID_ERR);
        this.item = item;
    }

    public Item getItem() {
        return item;
    }
}
