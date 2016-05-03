package org.bogdanbuduroiu.auction.model.comms.message;

import org.bogdanbuduroiu.auction.model.User;

/**
 * Created by bogdanbuduroiu on 28.04.16.
 */
public class AcknowledgedMessage extends Message{

    private final AckType ACK_TYPE;

    public AcknowledgedMessage(AckType ackType) {
        super(MessageType.ACK);
        this.ACK_TYPE = ackType;
    }

    public AckType ack_type() {
        return ACK_TYPE;
    }
}
