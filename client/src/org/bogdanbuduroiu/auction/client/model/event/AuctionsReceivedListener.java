package org.bogdanbuduroiu.auction.client.model.event;

/**
 * Created by bogdanbuduroiu on 30.04.16.
 */
public interface AuctionsReceivedListener {
    void auctionDataReceived(Object[][] data);
}
