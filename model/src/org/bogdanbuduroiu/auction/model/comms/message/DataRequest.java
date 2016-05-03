package org.bogdanbuduroiu.auction.model.comms.message;

import org.bogdanbuduroiu.auction.model.User;

/**
 * Created by bogdanbuduroiu on 30.04.16.
 */
public class DataRequest extends Message {

    private final DataRequestType DATA_REQUEST_TYPE;
    private User user;

    public DataRequest(DataRequestType DATA_REQUEST_TYPE) {
        super(MessageType.DATA_REQUEST);
        this.DATA_REQUEST_TYPE = DATA_REQUEST_TYPE;
    }

    public DataRequestType data_req_type() {
        return DATA_REQUEST_TYPE;
    }

    public User getUser() {
        return this.user;
    }
}
