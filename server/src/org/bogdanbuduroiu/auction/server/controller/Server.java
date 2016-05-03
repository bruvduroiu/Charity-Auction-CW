package org.bogdanbuduroiu.auction.server.controller;


import org.bogdanbuduroiu.auction.model.Bid;
import org.bogdanbuduroiu.auction.model.Item;
import org.bogdanbuduroiu.auction.model.User;
import org.bogdanbuduroiu.auction.model.comms.message.*;
import org.bogdanbuduroiu.auction.model.exception.InvalidBidException;

import java.io.*;
import java.nio.channels.SocketChannel;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.SynchronousQueue;

/**
 * Created by bogdanbuduroiu on 21.04.16.
 */


public class Server {

    ServerComms commsWorker;
    ResponseWorker responseWorker;
    private int port;
    Map<User, char[]> passwords = new HashMap<>();
    Map<User, SocketChannel> clients = new HashMap<>();
    Set<User> activeUsers = new HashSet<>();
    Map<Integer, Item> auctions = new HashMap<>();
    private static final String DIR_PATH = "../server/data";
    private static final String USERS_REL_PATH = "users.dat";
    private static final String AUCTIONS_REL_PATH = "auctions.dat";

    public Server(int port) throws IOException{
        this.port = port;
        commsWorker = new ServerComms(this);
        responseWorker = new ResponseWorker();
        System.out.println("[SRV]\tStarting server...");
        configureServer();

        new Thread(commsWorker).start();
        new Thread(responseWorker).start();
        System.out.println("[SRV]\tServer initialized on port " + this.port + ".");
    }

    public void processMessage(SocketChannel socket, Message message) {
            if (message.type() == MessageType.LOGIN_REQUEST) {
                LoginRequest lr = (LoginRequest) message;
                User tmpUser = lr.getUser();

                if (!validCredentials(tmpUser, lr.getPassword())) {
                    responseWorker.queueResponse(this, socket, new ErrMessage(tmpUser, ErrType.INVALID_LOGIN_ERR));
                    System.out.println("[USR]\tFailed login attempt at " + Date.from(ZonedDateTime.now().toInstant())
                            + ". User: " + tmpUser.getUsername() + ". Host: " + socket.socket().getInetAddress());
                    return;
                }

                this.activeUsers.add(tmpUser);
                this.clients.put(tmpUser,socket);

                responseWorker.queueResponse(this, socket, new AcknowledgedMessage(tmpUser, AckType.ACK_LOGIN));

                System.out.println("[USR]\tNew login from user " + tmpUser.getUsername() + " at "
                        + Date.from(ZonedDateTime.now().toInstant()) + ". Host: " + socket.socket().getInetAddress());
            }

            else if (message.type() == MessageType.REGISTRATION_REQUEST) {
                RegistrationRequest registrationRequest = (RegistrationRequest) message;
                User tmpUser = registrationRequest.getUser();
                char[] password = registrationRequest.getPassword();

                if (passwords.keySet().contains(tmpUser)) {
                    responseWorker.queueResponse(this, socket, new ErrMessage(tmpUser, ErrType.USER_EXISTS_ERR));
                    return;
                }

                this.passwords.put(tmpUser, password);
                this.responseWorker.queueResponse(this, socket, new AcknowledgedMessage(tmpUser, AckType.ACK_REGISTRATION));

                System.out.println("[USR]\tNew registration. Username: " + tmpUser.getUsername()
                        + " at " + Date.from(ZonedDateTime.now().toInstant()) + ". Host: " + socket.socket().getInetAddress());
            }

            else if (message.type() == MessageType.NEW_BID_REQUEST) {
                NewBidRequest newBidRequest = (NewBidRequest) message;
                Bid bid = newBidRequest.getBid();
                Item item = auctions.get(newBidRequest.getItem().getItemID());
                try {
                    item.addBid(bid);
                    this.responseWorker.queueResponse(this, socket, new BidAcknowledgedMessage(item));
                    System.out.println("[BID]\tNew bid on item "
                            + item.getTitle() + " at " + Date.from(ZonedDateTime.now().toInstant())
                            + ". Value: " + newBidRequest.getBid().getBidAmmount());
                }
                catch (InvalidBidException e) {
                    this.responseWorker.queueResponse(this, socket, new BidFailedMessage(item));
                }
            }

            else if (message.type() == MessageType.DATA_REQUEST) {
                DataRequest dataRequest = (DataRequest) message;

                if (dataRequest.data_req_type() == DataRequestType.AUCTIONS_REQ) {
                    responseWorker.queueResponse(this, socket, new DataReceivedMessage(null, DataRequestType.AUCTIONS_RECV, auctions));
                }

                else if (dataRequest.data_req_type() == DataRequestType.BIDS_REQ) {

                }
            }

            else if (message.type() == MessageType.CREATE_AUCTION_REQUEST) {
                CreateAuctionRequest auctionRequest = (CreateAuctionRequest) message;

                if (!this.newAuction(auctionRequest.getAuction())) {
                    System.out.println("[ERR]\tError adding new auction:");
                    return;
                }
                responseWorker.queueResponse(this, socket, new DataReceivedMessage(null, DataRequestType.AUCTIONS_RECV, auctions));
                System.out.println("[AUC]\tNew auction: " + auctionRequest.getAuction().getTitle()
                        + ". Price: " + auctionRequest.getAuction().getReservePrice());
            }
    }


    private boolean newAuction(Item auction) {
        try {
            auction.startAuction();
            auctions.put(auction.getItemID(), auction);
            return true;
        }
        catch (Exception e) {
            System.out.println("[ERR]\tError adding new auction: " + e.getMessage());
        }
        return false;
    }

    protected boolean validCredentials(User user, char[] password) {
        String passwd = new String(password);

        if (!passwords.keySet().contains(user)) return false;

        if (passwd.equals(new String(passwords.get(user)))) return true;

        return false;
    }

    private void configureServer() {
        try {
            loadData();
        }
        catch (ClassNotFoundException e) {
            System.out.println("[ERR]\tCorrupted file store.");
        }
        catch (IOException e) {
            System.out.println("[ERR]\tStored user data could not be loaded.");
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() ->{
            try {
                System.out.println("[SRV]\tServer shutting down...");
                storeData();
                System.out.println("Bye.");
            }
            catch (IOException e) {
                System.out.println("[ERR]\tFATAL_ERR: Unable to store session data. Reason: " + e.getMessage());
            }
        }));
    }


    private void loadData() throws IOException, ClassNotFoundException {
        File file = new File(DIR_PATH, USERS_REL_PATH);
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
        passwords = (HashMap<User, char[]>) ois.readObject();
        ois.close();

        file = new File(DIR_PATH, AUCTIONS_REL_PATH);
        ois = new ObjectInputStream(new FileInputStream(file));
        auctions = (HashMap<Integer, Item>) ois.readObject();
        ois.close();
    }

    private void storeData() throws IOException {
        File file = new File(DIR_PATH, USERS_REL_PATH);
        if (!(new File(DIR_PATH)).exists())
            new File(DIR_PATH).mkdir();
        if (!(file.exists())) {
            file = new File(DIR_PATH, USERS_REL_PATH);
            file.createNewFile();
        }
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
        oos.writeObject(passwords);
        oos.close();

        file = new File(DIR_PATH, AUCTIONS_REL_PATH);
        if (!file.exists()) {
            file = new File(DIR_PATH, AUCTIONS_REL_PATH);
            file.createNewFile();
        }

        oos = new ObjectOutputStream(new FileOutputStream(file));
        oos.writeObject(auctions);
        oos.close();
    }


    public static void main(String[] args) {
        try {
            new Server(8080);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
