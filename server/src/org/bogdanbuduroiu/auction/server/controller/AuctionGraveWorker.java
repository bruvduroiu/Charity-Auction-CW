package org.bogdanbuduroiu.auction.server.controller;

import org.bogdanbuduroiu.auction.model.Item;

import java.util.Iterator;

/**
 * Created by bogdanbuduroiu on 05.05.16.
 */

/**
 * Handles the closure of auctions on a separate thread.
 */
public class AuctionGraveWorker implements Runnable {

    // Reference to the server the GraveWorker is a slave of.
    private Server server;

    public AuctionGraveWorker(Server server) {
        this.server = server;
    }

    /**
     * Wake up the GraveWorker.
     */
    public synchronized void closeExpiredAuctions() {
        this.notify();
    }

    @Override
    public synchronized void run() {
        while (true) {
            try {

                // Waits until it is notified that it should check for expired auctions
                this.wait();
            } catch (InterruptedException e) {
            }

            // Iterate through all the auctions.
            // Check expired auctions.
            // Close them
            synchronized (server.auctions) {
                Iterator<Item> auctionIter = server.auctions.values().iterator();
                while (auctionIter.hasNext()) {
                    Item nextAuct = auctionIter.next();

                    // Ignore already closed auctions
                    if (nextAuct.isClosed()) continue;

                    if (nextAuct.isExpired())
                        server.closeAuction(nextAuct);
                }
            }
        }
    }
}
