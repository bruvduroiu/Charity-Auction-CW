package org.bogdanbuduroiu.auction.model;


import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.*;

/**
 * Created by bogdanbuduroiu on 21.04.16.
 */
public class Item implements Serializable {

    private long itemID;
    private String title;
    private String description;
    private Category category;
    private int vendorID;
    private long startTime;
    private long endTime;
    private long expiryTime;
    private boolean closed;
    private Double reservePrice;
    private BufferedImage itemImage;

    private PriorityQueue<Bid> bids = new PriorityQueue<>(
            (Bid o1, Bid o2) -> o2.getBidAmmount().compareTo(o1.getBidAmmount())
    );

    public Item(String title, String description, Category category, int vendorID, long expiryTime, Double reservePrice, BufferedImage itemImage) {
        this.itemID = this.hashCode();
        this.title = title;
        this.description = description;
        this.category = category;
        this.vendorID = vendorID;
        this.expiryTime = expiryTime;
        this.reservePrice = reservePrice;
        this.itemImage = itemImage;
    }

    public long getItemID() {
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

    public int getVendorID() {
        return vendorID;
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
        return reservePrice;
    }

    public BufferedImage getItemImage() {
        return itemImage;
    }

    public PriorityQueue<Bid> getBids() {
        return bids;
    }

    public Date getTimeRemaining() {
        return new Date((this.expiryTime - System.currentTimeMillis() * 1000));
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
