package org.bogdanbuduroiu.auction.model.comms.message;

import org.bogdanbuduroiu.auction.model.User;

/**
 * Created by bogdanbuduroiu on 30.04.16.
 */
public class DataRequest extends Message {

    private final DataRequestType DATA_REQUEST_TYPE;

    public DataRequest(User sender, DataRequestType dataRequestType) {
        super(sender, MessageType.DATA_REQUEST);
        this.DATA_REQUEST_TYPE = dataRequestType;
    }

    public DataRequestType data_req_type() {
        return DATA_REQUEST_TYPE;
    }
}
