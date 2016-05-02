package org.bogdanbuduroiu.auction.model;

import java.io.Serializable;

/**
 * Created by bogdanbuduroiu on 21.04.16.
 */
public class Bid implements Serializable {

    private User user;
    private Double bidAmmount;

    public Bid(User user, Double bidAmmount) {
        this.user = user;
        this.bidAmmount = bidAmmount;
    }

    public User getUser() {
        return user;
    }

    public Double getBidAmmount() {
        return bidAmmount;
    }
}
