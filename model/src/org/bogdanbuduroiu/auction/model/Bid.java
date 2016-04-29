package org.bogdanbuduroiu.auction.model;

import java.io.Serializable;

/**
 * Created by bogdanbuduroiu on 21.04.16.
 */
public class Bid implements Serializable {

    private String username;
    private Double bidAmmount;

    public Bid(String username, Double bidAmmount) {
        this.username = username;
        this.bidAmmount = bidAmmount;
    }

    public String getUsername() {
        return username;
    }

    public Double getBidAmmount() {
        return bidAmmount;
    }
}
