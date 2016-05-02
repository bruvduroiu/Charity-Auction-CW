package org.bogdanbuduroiu.auction.model.comms.message;

import org.bogdanbuduroiu.auction.model.Item;
import org.bogdanbuduroiu.auction.model.User;

/**
 * Created by bogdanbuduroiu on 02.05.16.
 */
public class BidAcknowledgedMessage extends AcknowledgedMessage {

    private final Item ITEM;

    public BidAcknowledgedMessage(Item item) {
        super(null, AckType.ACK_NEW_BID);
        this.ITEM = item;
    }

    public Item getItem() {
        return ITEM;
    }
}
