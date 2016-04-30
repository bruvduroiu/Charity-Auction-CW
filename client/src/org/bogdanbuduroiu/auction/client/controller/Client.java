package org.bogdanbuduroiu.auction.client.controller;

import org.bogdanbuduroiu.auction.client.model.event.AuctionsReceivedListener;
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
    private User currentUser;
    ClientLoginScreen clientLoginScreen;
    MainAuctionScreen mainAuctionScreen;
    private AuctionsReceivedListener auctionsReceivedListener;

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
            e.printStackTrace();
        }
    }

    public void processMessage(Message message) {
        if (message.type() == MessageType.ACK) {
            if (((AcknowledgedMessage) message).ack_type() == AckType.ACK_LOGIN) {
                clientLoginScreen.dispose();
                currentUser = message.getSender();
                mainAuctionScreen = MainAuctionScreen.initializeScreen(this);
                requestData(DataRequestType.AUCTIONS_REQ);
            }
            else if (((AcknowledgedMessage) message).ack_type() == AckType.ACK_REGISTRATION) {
                clientLoginScreen.registrationSuccessful();
            }
        }
        else if(message.type() == MessageType.ERR) {
            if (((ErrMessage) message).err_type() == ErrType.INVALID_LOGIN_ERR) {
                clientLoginScreen.invalidLogin();
            }
            //TODO: Error Handling for Registration
        }
        else if(message.type() == MessageType.DATA_RECEIVED) {
            if (((DataReceivedMessage) message).data_received_type() == DataRequestType.AUCTIONS_RECV) {
                auctionsReceivedListener.auctionDataReceived(((DataReceivedMessage) message).getData());
            }
        }
    }

    public void requestData(DataRequestType dataRequestType) {
        try {
            ResponseHandler rspHandler = new ResponseHandler();
            worker.sendMessage(new DataRequest(null, dataRequestType), rspHandler);
            rspHandler.waitForResponse(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getCurrentUsername() {
        return currentUser.getUsername();
    }

    public void addAuctionDataReceivedListener(AuctionsReceivedListener li) {
        this.auctionsReceivedListener = li;
    }

    public static void main(String[] args) {
        new Client();
    }
}




