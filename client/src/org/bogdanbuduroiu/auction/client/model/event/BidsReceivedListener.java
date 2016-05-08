package org.bogdanbuduroiu.auction.client.model.event;

import org.bogdanbuduroiu.auction.model.Item;
import org.bogdanbuduroiu.auction.model.User;

import java.util.Map;

/**
 * Created by bogdanbuduroiu on 07.05.16.
 */
public interface BidsReceivedListener {
    void bidsReceived(Map<Integer, Item> auctionData, User user);
}
