package org.bogdanbuduroiu.auction.client.controller;

import org.bogdanbuduroiu.auction.client.model.event.AuctionsReceivedListener;
import org.bogdanbuduroiu.auction.client.model.event.BidsReceivedListener;
import org.bogdanbuduroiu.auction.client.view.ClientLoginScreen;
import org.bogdanbuduroiu.auction.client.view.MainAuctionScreen;
import org.bogdanbuduroiu.auction.model.Bid;
import org.bogdanbuduroiu.auction.model.Item;
import org.bogdanbuduroiu.auction.model.comms.message.*;
import org.bogdanbuduroiu.auction.model.User;

import javax.swing.*;
import javax.xml.crypto.Data;
import java.io.*;
import java.net.InetAddress;
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
    BidsReceivedListener bidsReceivedListener;

    public Client() {
        try {
            worker = new Comms(this, 8080);
//            worker = new Comms(this, InetAddress.getByName("188.166.169.154"), 8080);
            Thread t = new Thread(worker);
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
            requestData(DataRequestType.BIDS_REQ);
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
        auction.setVendor(currentUser);
        worker.sendMessage(new CreateAuctionRequest(currentUser, auction), rspHandler);
        rspHandler.waitForResponse(this);
    }

    public void newBid(Item item, double bidAmmount) throws IOException {
        ResponseHandler rspHandler = new ResponseHandler();
        worker.sendMessage(new NewBidRequest(currentUser, item, new Bid(currentUser, bidAmmount)), rspHandler);
        rspHandler.waitForResponse(this);
        requestData(DataRequestType.BIDS_REQ);
    }

    public void processMessage(Message message) {
        if (message.type() == MessageType.ACK) {
            AcknowledgedMessage ack_message = (AcknowledgedMessage) message;
            if (ack_message.ack_type() == AckType.ACK_LOGIN) {
                clientLoginScreen.dispose();
                mainAuctionScreen = MainAuctionScreen.initializeScreen(this);
                requestData(DataRequestType.AUCTIONS_REQ);
                requestData(DataRequestType.BIDS_REQ);
                configureClient();
            }
            else if (ack_message.ack_type() == AckType.ACK_REGISTRATION)
                clientLoginScreen.registrationSuccessful();

            else if (ack_message.ack_type() == AckType.ACK_NEW_BID)
                mainAuctionScreen.bidSuccess(((BidAcknowledgedMessage) ack_message).getItem());
        }
        else if(message.type() == MessageType.ERR) {
            ErrMessage errMessage = (ErrMessage) message;
            if (errMessage.err_type() == ErrType.INVALID_LOGIN_ERR) {
                clientLoginScreen.invalidLogin();
            }

            else if (errMessage.err_type() == ErrType.INVALID_BID_ERR) {
                mainAuctionScreen.bidFail(((BidFailedMessage) message).getItem());
            }
            //TODO: Error Handling for Registration
        }
        else if(message.type() == MessageType.DATA_RECEIVED) {
            DataReceivedMessage dataReceivedMessage = ((DataReceivedMessage) message);
            System.out.print("[DAT]\tData received from server. Data type: ");
            if (dataReceivedMessage.data_received_type() == DataReceivedType.AUCTIONS_RECV) {
                System.out.println("AUCTIONS_RECV");
                for (AuctionsReceivedListener listener : auctionsReceivedListeners)
                    listener.auctionDataReceived(dataReceivedMessage.getData());
                if (dataReceivedMessage.getWonAuctions() == null)
                    return;
                for (Item item : dataReceivedMessage.getWonAuctions())
                    JOptionPane.showMessageDialog(null, "Congratulations! You have won " + item.getTitle() +
                            " for " + item.getBids().peek().getBidAmmount());
            }
            else if (dataReceivedMessage.data_received_type() == DataReceivedType.BIDS_RECV) {
                System.out.println("BIDS_RECV");
                bidsReceivedListener.bidsReceived(dataReceivedMessage.getData(), currentUser);
            }
        }
        else if (message.type() == MessageType.AUCTION_WON_NOTIFICATION) {
            AuctionWonNotification auctionWonNotif = ((AuctionWonNotification) message);
            JOptionPane.showConfirmDialog(null, "Congratulations! You have won " + auctionWonNotif.getAuction().getTitle() +
                    " for " + auctionWonNotif.getAuction().getBids().peek().getBidAmmount());
        }
    }


    public void requestData(DataRequestType dataRequestType) {
        try {
            System.out.println("[REQ]\tRequesting data from server...");
            ResponseHandler rspHandler = new ResponseHandler();
            worker.sendMessage(new DataRequest(currentUser, dataRequestType), rspHandler);
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

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public void addAuctionDataReceivedListener(AuctionsReceivedListener li) {
        auctionsReceivedListeners.add(li);
    }

    public void addBidsReceivedListener(BidsReceivedListener li) {
        this.bidsReceivedListener = li;
    }

    public static void main(String[] args) {
        new Client();
    }
}




