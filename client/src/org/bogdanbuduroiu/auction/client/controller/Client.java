package org.bogdanbuduroiu.auction.client.controller;

import org.bogdanbuduroiu.auction.client.model.event.AuctionsReceivedListener;
import org.bogdanbuduroiu.auction.client.view.ClientLoginScreen;
import org.bogdanbuduroiu.auction.client.view.MainAuctionScreen;
import org.bogdanbuduroiu.auction.model.Item;
import org.bogdanbuduroiu.auction.model.comms.message.*;
import org.bogdanbuduroiu.auction.model.User;

import javax.xml.crypto.Data;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by bogdanbuduroiu on 21.04.16.
 */
public class Client {

    protected Comms worker;
    private User currentUser;
    ClientLoginScreen clientLoginScreen;
    MainAuctionScreen mainAuctionScreen;
    List<AuctionsReceivedListener> auctionsReceivedListeners= new ArrayList<>();

    public Client() {
        try {
            worker = new Comms(8080);
            Thread t = new Thread(worker);
            configureClient();
            t.start();
            clientLoginScreen = new ClientLoginScreen(this);
        } catch (IOException e) {
            System.out.println("[ERR]\tError occurred when initializing the client. " + e.getMessage());
        }
    }

    public void validateDetails(User user, char[] password){
        try {
            ResponseHandler rspHandler = new ResponseHandler();
            worker.sendMessage(new LoginRequest(user, password), rspHandler);
            rspHandler.waitForResponse(this);
        } catch (IOException e) {
            System.out.println("[ERR]\tError occurred when attempting logging in. " + e.getMessage());
        }
    }

    public void newRegistration(User user, char[] password) {
        try {
            ResponseHandler rspHandler = new ResponseHandler();
            worker.sendMessage(new RegistrationRequest(user, password), rspHandler);
            rspHandler.waitForResponse(this);
        }
        catch (IOException e) {
            System.out.println("[ERR]\tError occurred registering. " + e.getMessage());
        }
    }

    public void newAuction(Item auction) throws IOException {
        ResponseHandler rspHandler = new ResponseHandler();
        auction.setVendorID(currentUser.getUserID());
        worker.sendMessage(new CreateAuctionRequest(currentUser, auction), rspHandler);
        rspHandler.waitForResponse(this);
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
            DataReceivedMessage dataReceivedMessage = ((DataReceivedMessage) message);
            System.out.print("[DAT]\tData received from server. Data type: ");
            if (dataReceivedMessage.data_received_type() == DataRequestType.AUCTIONS_RECV) {
                System.out.println("AUCTIONS_RECV");
                for (AuctionsReceivedListener listener : auctionsReceivedListeners)
                    listener.auctionDataReceived(dataReceivedMessage.getData());
            }
        }
    }


    public void requestData(DataRequestType dataRequestType) {
        try {
            System.out.println("[REQ]\tRequesting data from server...");
            ResponseHandler rspHandler = new ResponseHandler();
            worker.sendMessage(new DataRequest(null, dataRequestType), rspHandler);
            rspHandler.waitForResponse(this);
        } catch (IOException e) {
            System.out.println("[ERR]\tError occurred while requesting data. " + e.getMessage());
        }
    }

    public void configureClient() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        final Runnable requester = () -> requestData(DataRequestType.AUCTIONS_REQ);

        scheduler.scheduleAtFixedRate(requester, 10, 10, TimeUnit.SECONDS);

    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void addAuctionDataReceivedListener(AuctionsReceivedListener li) {
        auctionsReceivedListeners.add(li);
    }

    public static void main(String[] args) {
        new Client();
    }
}



