package org.bogdanbuduroiu.auction.model.comms.message;

import org.bogdanbuduroiu.auction.model.User;

import java.io.Serializable;

/**
 * Created by bogdanbuduroiu on 23.04.16.
 */
public class RegistrationRequest extends Message implements Serializable {

    private User user;
    private char[] password;

    public RegistrationRequest(User user, char[] password) {
        super(null, MessageType.REGISTRATION_REQUEST);
        this.user = user;
        this.password = password;
    }

    public User getUser() {
        return this.user;
    }

    public char[] getPassword() {
        return password;
    }

}
