package org.bogdanbuduroiu.auction.model.comms.message;

import org.bogdanbuduroiu.auction.model.User;

import java.io.Serializable;

/**
 * Created by bogdanbuduroiu on 23.04.16.
 */
public class CreateAuctionRequest extends Message implements Serializable {

    public CreateAuctionRequest(User sender) {
        super(sender, MessageType.CREATE_AUCTION_REQUEST);
    }
}
