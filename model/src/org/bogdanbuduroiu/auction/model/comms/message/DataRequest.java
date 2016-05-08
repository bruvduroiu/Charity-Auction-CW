package org.bogdanbuduroiu.auction.model.comms.message;

import org.bogdanbuduroiu.auction.model.Category;
import org.bogdanbuduroiu.auction.model.User;

/**
 * Created by bogdanbuduroiu on 30.04.16.
 */
public class DataRequest extends Message {

    private final DataRequestType DATA_REQUEST_TYPE;
    private final User USER;
    private final Category CATEGORY;
    private final String KEYWORD;

    public DataRequest(User sender, DataRequestType DATA_REQUEST_TYPE) {
        this(sender, DATA_REQUEST_TYPE, Category.ALL);
    }

    public DataRequest(User sender, DataRequestType DATA_REQUEST_TYPE, Category category) {
        super(sender, MessageType.DATA_REQUEST);
        this.DATA_REQUEST_TYPE = DATA_REQUEST_TYPE;
        this.CATEGORY = category;
        this.USER = sender;
        this.KEYWORD = "";
    }

    public DataRequest(User sender, DataRequestType DATA_REQUEST_TYPE, String keyword) {
        super(sender, MessageType.DATA_REQUEST);
        this.DATA_REQUEST_TYPE = DATA_REQUEST_TYPE;
        this.CATEGORY = Category.ALL;
        this.USER = sender;
        this.KEYWORD = keyword;
    }

    public DataRequestType data_req_type() {
        return DATA_REQUEST_TYPE;
    }

    public Category getCategory() {
        return CATEGORY;
    }

    public String getKeyword() {
        return KEYWORD;
    }

    public User getUser() {
        return this.USER;
    }
}
