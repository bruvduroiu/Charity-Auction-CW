package org.bogdanbuduroiu.auction.client.model.event;

import org.bogdanbuduroiu.auction.model.Item;

import java.util.Map;
import java.util.Set;

/**
 * Created by bogdanbuduroiu on 30.04.16.
 */
public interface AuctionsReceivedListener {
    void auctionDataReceived(Map<Integer, Item> data);
}
