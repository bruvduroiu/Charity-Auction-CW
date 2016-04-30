package org.bogdanbuduroiu.auction.model.comms.message;

import org.bogdanbuduroiu.auction.model.User;

/**
 * Created by bogdanbuduroiu on 30.04.16.
 */
public class DataReceivedMessage extends Message{

    private final DataRequestType DATA_RECEIVED_TYPE;
    private Object[][] data;

    public DataReceivedMessage(User sender, DataRequestType DATA_RECEIVED_TYPE, Object[][] data) {
        super(sender, MessageType.DATA_RECEIVED);
        this.DATA_RECEIVED_TYPE = DATA_RECEIVED_TYPE;
        this.data = data;
    }

    public Object[][] getData() {
        return this.data;
    }

    public DataRequestType data_received_type() {
        return DATA_RECEIVED_TYPE;
    }
}
