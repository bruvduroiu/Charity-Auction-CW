package org.bogdanbuduroiu.auction.model.comms.message;

import org.bogdanbuduroiu.auction.model.Item;
import org.bogdanbuduroiu.auction.model.User;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by bogdanbuduroiu on 30.04.16.
 */
public class DataReceivedMessage extends Message{

    private final DataReceivedType DATA_RECEIVED_TYPE;
    private Map<Integer, Item> data;
    private Set<Item> wonItems;

    public DataReceivedMessage(User sender, DataReceivedType DATA_RECEIVED_TYPE, Map<Integer, Item> data) {
        this(sender, DATA_RECEIVED_TYPE, data, null);
    }

    public DataReceivedMessage(User sender, DataReceivedType DATA_RECEIVED_TYPE, Map<Integer, Item> data, Set<Item> wonItems) {
        super(sender, MessageType.DATA_RECEIVED);
        this.DATA_RECEIVED_TYPE = DATA_RECEIVED_TYPE;
        this.data = data;
        this.wonItems = wonItems;
    }

    public Map<Integer, Item> getData() {
        return this.data;
    }

    public DataReceivedType data_received_type() {
        return DATA_RECEIVED_TYPE;
    }

    public Set<Item> getWonAuctions() {
        return this.wonItems;
    }
}
