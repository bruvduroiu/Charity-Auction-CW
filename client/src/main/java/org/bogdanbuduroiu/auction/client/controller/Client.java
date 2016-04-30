package org.bogdanbuduroiu.auction.client.controller;

import org.bogdanbuduroiu.auction.client.view.ClientLoginScreen;
import org.bogdanbuduroiu.auction.client.view.MainAuctionScreen;
import org.bogdanbuduroiu.auction.model.comms.message.*;
import org.bogdanbuduroiu.auction.model.User;

import java.io.*;

/**
 * Created by bogdanbuduroiu on 21.04.16.
 */
public class Client {

    protected Comms worker;
    ClientLoginScreen clientLoginScreen;
    MainAuctionScreen mainAuctionScreen;

    public Client() {
        try {
            worker = new Comms(8080);
            Thread t = new Thread(worker);
            t.start();
            clientLoginScreen = new ClientLoginScreen(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void validateDetails(User user, char[] password){
        try {
            ResponseHandler rspHandler = new ResponseHandler();
            worker.sendMessage(new LoginRequest(user, password), rspHandler);
            rspHandler.waitForResponse(this);
        } catch (IOException e) {
            //TODO: Error Handling
            e.printStackTrace();
        }
    }

    public void newRegistration(User user, char[] password) {
        try {
            ResponseHandler rspHandler = new ResponseHandler();
            worker.sendMessage(new RegistrationRequest(user, password), rspHandler);
            rspHandler.waitForResponse(this);
        }
        catch (IOException e) {
            //TODO: Error Handling
            e.printStackTrace();
        }
    }

    public void processMessage(Message message) {
        if (message.type() == MessageType.ACK) {
            if (((AcknowledgedMessage) message).ack_type() == AckType.ACK_LOGIN) {
                clientLoginScreen.dispose();
                mainAuctionScreen = MainAuctionScreen.initializeScreen(message.getSender());
            }
            else if (((AcknowledgedMessage) message).ack_type() == AckType.ACK_REGISTRATION) {
                clientLoginScreen.registrationSuccessful();
            }
        }
        else if(message.type() == MessageType.ERR) {
            if (((ErrMessage) message).err_type() == ErrType.INVALID_LOGIN_ERR) {
                clientLoginScreen.invalidLogin();
            }
        }
    }

    public static void main(String[] args) {
        new Client();
    }
}




