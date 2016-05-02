package org.bogdanbuduroiu.auction.model.exception;

/**
 * Created by bogdanbuduroiu on 02.05.16.
 */
public class InvalidBidException extends Exception{

    private static final String MESSAGE = " Bid not high enough";

    public InvalidBidException() {
        super(MESSAGE);
    }
}
