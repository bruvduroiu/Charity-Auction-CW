package org.bogdanbuduroiu.auction.client.controller;

import org.bogdanbuduroiu.auction.client.view.ClientLoginScreen;
import org.bogdanbuduroiu.auction.model.comms.message.LoginRequest;
import org.bogdanbuduroiu.auction.model.comms.Comms;
import org.bogdanbuduroiu.auction.model.comms.message.RegistrationRequest;
import org.bogdanbuduroiu.auction.model.User;

import java.io.*;

/**
 * Created by bogdanbuduroiu on 21.04.16.
 */
public class Client {

    protected Comms worker;

    public Client() {
        try {
            worker = new Comms(8080);
            Thread t = new Thread(worker);
            t.start();
            ClientLoginScreen clientLoginScreen = new ClientLoginScreen(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void validateDetails(User user, char[] password){
        try {
            worker.sendMessage(new LoginRequest(user, password));
        } catch (IOException e) {
            //TODO: Error Handling
            e.printStackTrace();
        }
    }

    public void newRegistration(User user, char[] password) {
        try {
            worker.sendMessage(new RegistrationRequest(user, password));
        }
        catch (IOException e) {
            //TODO: Error Handling
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Client();
    }




}




