package org.bogdanbuduroiu.auction.model;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.bogdanbuduroiu.auction.model.comms.message.Message;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by bogdanbuduroiu on 21.04.16.
 */
public class User implements Serializable{

    private String firstName;
    private String lastName;
    private String username;
    private int userID;

    public User(String username) {
        this(null, null, username);
    }

    public User(String firstName, String lastName, String username) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.userID = this.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (!(obj instanceof User)) return false;

        return username.equals(((User) obj).username);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17,31)
                .append(username)
                .toHashCode();
    }

    public String getUsername() {
        return username;
    }

    public int getUserID() {
        return userID;
    }
}
