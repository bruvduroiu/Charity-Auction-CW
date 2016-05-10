package org.bogdanbuduroiu.auction.model;



import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bogdanbuduroiu.auction.model.exception.InvalidBidException;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
        this.title = title;
        this.description = description;
        this.category = category;
        this.vendor = vendor;
        this.expiryTime = expiryTime;
        this.RESERVE_PRICE = reservePrice;
        this.bids.add(new Bid(vendor, RESERVE_PRICE));
        this.itemID = this.hashCode();
    }

    public void setVendor(User vendor) {
        this.vendor = vendor;
    }

    public void addBid(Bid bid) throws InvalidBidException {
        if (bid.getBidAmmount() <= bids.peek().getBidAmmount())
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

    public int getVendorId() {
        return this.vendor.getUserID();
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

    public String getTimeRemainingString() {
        DateTime currentDate = new DateTime();
        DateTime expiryDate = new DateTime(expiryTime);

        Duration duration = new Duration(currentDate, expiryDate);
        String timeRemaining = "N/A";
        if (duration.getStandardDays() > 0)
            timeRemaining = duration.getStandardDays() +
                    "d:" + (duration.getStandardHours() - duration.getStandardDays() * 24) +
                    "h:" + (duration.getStandardMinutes() - duration.getStandardHours() * 60) + "m";

        else
            timeRemaining = duration.getStandardHours() +
                    "h:" + (duration.getStandardMinutes() - duration.getStandardHours() * 60) +
                    "m:" + (duration.getStandardSeconds() - duration.getStandardMinutes() * 60) + "s";
        return timeRemaining;
    }

    public long getMillisRemaining() {
        return (this.expiryTime - System.currentTimeMillis());
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() >= this.expiryTime);
    }

    public void startAuction() {
        startTime = System.currentTimeMillis();
        closed = false;
    }

    public void closeAuction() {
        endTime = System.currentTimeMillis();
        closed = true;
    }

    public User getAuctionWinner() {
        if (bids.size() > 1)
            return this.bids.peek().getUser();
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (!(obj instanceof Item)) return false;

        final Item other = (Item) obj;
        return new EqualsBuilder()
                .append(title, other.title)
                .append(vendor, other.vendor)
                .append(description, other.description)
                .append(RESERVE_PRICE, other.RESERVE_PRICE)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(title)
                .append(vendor)
                .append(description)
                .append(RESERVE_PRICE)
                .toHashCode();
    }
}
