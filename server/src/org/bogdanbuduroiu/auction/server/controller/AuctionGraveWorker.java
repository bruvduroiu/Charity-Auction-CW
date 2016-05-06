package org.bogdanbuduroiu.auction.server.controller;

import org.bogdanbuduroiu.auction.model.Item;

import java.util.Iterator;

/**
 * Created by bogdanbuduroiu on 05.05.16.
 */
public class AuctionGraveWorker implements Runnable {

    private Server server;

    public AuctionGraveWorker(Server server) {
        this.server = server;
    }

    public synchronized void closeExpiredAuctions() {
        this.notify();
    }

    @Override
    public synchronized void run() {
        while (true) {
            try {
                this.wait();
            } catch (InterruptedException e) {
            }

            synchronized (server.auctions) {
                Iterator<Item> auctionIter = server.auctions.values().iterator();
                while (auctionIter.hasNext()) {
                    Item nextAuct = auctionIter.next();

                    if (nextAuct.isClosed()) continue;

                    if (nextAuct.isExpired())
                        server.closeAuction(nextAuct);
                }
            }
        }
    }
}
