package org.bogdanbuduroiu.auction.model.comms.message;

import org.bogdanbuduroiu.auction.model.User;

import java.io.Serializable;

/**
 * Created by bogdanbuduroiu on 23.04.16.
 */
public class LoginRequest extends Message implements Serializable {

    private User user;
    private char[] password;

    public LoginRequest(User user, char[] password) {
        //TODO: Add encryption to the username & password
        //TODO: Fix this band aid "null"
        super(null, MessageType.LOGIN_REQUEST);
        this.user = user;
        this.password = password;
    }

    public User getUser() {
        return this.user;
    }

    public char[] getPassword() {
        return this.password;
    }
}
