package org.bogdanbuduroiu.auction.model;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;

/**
 * Created by bogdanbuduroiu on 21.04.16.
 */
public class User implements Serializable{

    private String firstName;
    private String lastName;
    private String username;
    private transient int userID;

    public User(String username) {
        this(null, null, username);
    }

    public User(String firstName, String lastName, String username) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        userID = new HashCodeBuilder(17,37).append(username).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (!(obj instanceof User)) return false;

        return username.equals(((User) obj).username);
    }

    @Override
    public int hashCode() {
        return userID;
    }

    public String getUsername() {
        return username;
    }

    public int getUserID() {
        return userID;
    }
}
