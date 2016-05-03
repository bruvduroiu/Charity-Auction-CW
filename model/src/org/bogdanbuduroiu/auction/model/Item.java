package org.bogdanbuduroiu.auction.model;



import org.bogdanbuduroiu.auction.model.exception.InvalidBidException;

import java.io.Serializable;
import java.util.*;

/**
 * Created by bogdanbuduroiu on 21.04.16.
 */
public class Item implements Serializable {

    private int itemID;
    private String title;
    private String description;
    private Category category;
    private User vendor;
    private long startTime;
    private long endTime;
    private long expiryTime;
    private boolean closed;
    private final Double RESERVE_PRICE;

    private PriorityQueue<Bid> bids = new PriorityQueue<>(
            (Comparator<Bid> & Serializable)(Bid o1, Bid o2) -> o2.getBidAmmount().compareTo(o1.getBidAmmount())
    );

    public Item(String title, String description, Category category, User vendor, long expiryTime, Double reservePrice) {
        this.itemID = this.hashCode();
        this.title = title;
        this.description = description;
        this.category = category;
        this.vendor = vendor;
        this.expiryTime = expiryTime;
        this.RESERVE_PRICE = reservePrice;
        this.bids.add(new Bid(vendor, RESERVE_PRICE));
    }

    public void setVendor(User vendor) {
        this.vendor = vendor;
    }

    public void addBid(Bid bid) throws InvalidBidException {
        if (bid.getBidAmmount() < bids.peek().getBidAmmount())
            throw new InvalidBidException();

        bids.offer(bid);
    }

    public int getItemID() {
        return itemID;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public User getVendor() {
        return vendor;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public boolean isClosed() {
        return closed;
    }

    public Double getReservePrice() {
        return RESERVE_PRICE;
    }

    public PriorityQueue<Bid> getBids() {
        return bids;
    }

    public Date getTimeRemaining() {
        return new Date((this.expiryTime - System.currentTimeMillis()));
    }

    public void startAuction() {
        startTime = System.currentTimeMillis();
        closed = false;
    }

    public void closeAuction() {
        endTime = System.currentTimeMillis();
        closed = true;
    }
}
