package org.bogdanbuduroiu.auction.model.comms.message;

import org.bogdanbuduroiu.auction.model.Item;
import org.bogdanbuduroiu.auction.model.User;

/**
 * Created by bogdanbuduroiu on 02.05.16.
 */
public class BidAcknowledgedMessage extends AcknowledgedMessage {

    private final Item ITEM;

    public BidAcknowledgedMessage(User sender, Item item) {
        super(sender, AckType.ACK_NEW_BID);
        this.ITEM = item;
    }

    public Item getItem() {
        return ITEM;
    }
}
