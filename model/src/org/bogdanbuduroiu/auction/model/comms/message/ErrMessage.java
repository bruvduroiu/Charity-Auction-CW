package org.bogdanbuduroiu.auction.model.comms.message;

import org.bogdanbuduroiu.auction.model.User;

/**
 * Created by bogdanbuduroiu on 30.04.16.
 */
public class ErrMessage extends Message {

    private static final MessageType TYPE = MessageType.ERR;
    private final ErrType ERR_TYPE;

    public ErrMessage(User sender, ErrType ERR_TYPE) {
        super(sender, TYPE);
        this.ERR_TYPE = ERR_TYPE;
    }

    public ErrType err_type() {
        return this.ERR_TYPE;
    }
}
