package org.bogdanbuduroiu.auction.server.controller;


import org.bogdanbuduroiu.auction.model.Bid;
import org.bogdanbuduroiu.auction.model.Category;
import org.bogdanbuduroiu.auction.model.Item;
import org.bogdanbuduroiu.auction.model.User;
import org.bogdanbuduroiu.auction.model.comms.message.*;
import org.bogdanbuduroiu.auction.model.exception.InvalidBidException;
import org.bogdanbuduroiu.auction.server.security.PasswordStorage;

import java.io.*;
import java.nio.channels.SocketChannel;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.SynchronousQueue;

/**
 * Created by bogdanbuduroiu on 21.04.16.
 */


/**
 * This is the class of the server. It deals with backend procedures:
 *      1) It uses the DataPersistance class to load/store user/auction data
 *      2) It manages registrations:
 *          - creates user
 *          - creates salted hashes for passwords (using PasswordStorage class)
 *          - provides response accordingly (ACK_REGISTRATION, USER_EXISTS_ERR)
 *      3) It manages authentications:
 *          - validates Login (using PasswordStorage to retrieve hashes)
 *          - provides response accordingly (ACK_LOGIN, INVALID_LOGIN_ERR)
 *      4) It processes any messages received from the ServerComms class
 *      5) Keeps track of active users and auctions
 *      6) Creates new auctions
 */

public class Server {

    // ServerComms class is the Server implementation of the Comms class
    // ServerComms runs on a separate Thread and manages all communication
    ServerComms commsWorker;

    // Separate thread managing responses to Client requests
    ResponseWorker responseWorker;

    // Auction Undertaker buries the auctions once they have "died of old age"
    AuctionGraveWorker auctionGraveWorker;

    // Port used by the server for Client communication
    private int port;

    // Passwords are NOT stored within the User class.
    // Passwords are hashed with a salt before being added to this Map
    Map<User, String> passwords = new HashMap<>();

    // Map used to disseminate which client belongs to which user
    private Map<User, SocketChannel> clients = new HashMap<>();

    // Using set for storing active users. Reason: small time-complexity of contains()
    Set<User> activeUsers = new HashSet<>();

    Map<Integer, Item> auctions = new HashMap<>();

    public Server(int port) throws IOException
    {
        this.port = port;

        commsWorker = new ServerComms(this);

        responseWorker = new ResponseWorker();

        auctionGraveWorker = new AuctionGraveWorker(this);

        System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][SRV]\tStarting server...");
        System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][SRV]\tStarting server...");

        configureServer();

        new Thread(commsWorker).start();
        new Thread(responseWorker).start();
        new Thread(auctionGraveWorker).start();

        System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][SRV]\tServer initialized on port " + this.port + ".");
    }

    /**
     * Called by the ServerComms class to process every message received by the ServerSocketChannel
     *
     *
     * @param socket Sender's (client) SocketChannel used for responding to request (ResponseWorker)
     * @param message Sender's (client) Message
     * @see ResponseWorker
     */
    public void processMessage(SocketChannel socket, Message message)
    {
        try {

            auctionGraveWorker.closeExpiredAuctions();

            if (message.type() == MessageType.LOGIN_REQUEST) {

                LoginRequest lr = (LoginRequest) message;
                User tmpUser = lr.getUser();

                // Hashing password provided at Login Screen and comparing to stored hash.
                // NOTE: Hashing passwords drastically decreases Server performance
                if (!validCredentials(lr.getUser(), new String(lr.getPassword()))) {
                    // Inform the client of the failed login attempt
                    responseWorker.queueResponse(this, socket, new ErrMessage(null, ErrType.INVALID_LOGIN_ERR));

                    // Log the Failed login attempt, together with the attempted username and client's host address.
                    System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][USR]\t" +
                            "Failed login attempt; User: " + tmpUser.getUsername() + ". Host: " + socket.socket().getInetAddress());
                    return;
                }

                // User is now active
                this.activeUsers.add(tmpUser);

                // Store SocketChannel belonging to tmpUsers's Client
                this.clients.put(tmpUser, socket);

                // Inform client of successful login.
                responseWorker.queueResponse(this, socket, new AcknowledgedMessage(null, AckType.ACK_LOGIN));

                // Log the successful login.
                System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][USR]\t" +
                        "New login from user " + tmpUser.getUsername() + "; Host: " + socket.socket().getInetAddress());


            } else if (message.type() == MessageType.REGISTRATION_REQUEST) {

                RegistrationRequest registrationRequest = (RegistrationRequest) message;

                User tmpUser = registrationRequest.getUser();
                String password = new String(registrationRequest.getPassword());

                // Hashing password using a salted hashing algorithm (see PasswordStorage class)
                // NOTE: Hashing passwords drastically decreases Server performance
                String hashedPassword = PasswordStorage.createHash(password);

                // Check if a user is not already registered under the same alias
                if (passwords.keySet().contains(tmpUser)) {

                    // Inform client of already existing alias
                    responseWorker.queueResponse(this, socket, new ErrMessage(null, ErrType.USER_EXISTS_ERR));
                    return;
                }

                // Add the new user, together with their password
                this.passwords.put(tmpUser, hashedPassword);

                // Inform client of the successful registration
                this.responseWorker.queueResponse(this, socket, new AcknowledgedMessage(null, AckType.ACK_REGISTRATION));

                System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][USR]\t" +
                        "New registration. Username: " + tmpUser.getUsername() +"; Host: " + socket.socket().getInetAddress());

            } else if (message.type() == MessageType.NEW_BID_REQUEST) {

                NewBidRequest newBidRequest = (NewBidRequest) message;
                Bid bid = newBidRequest.getBid();
                Item item;
                synchronized (auctions) {
                    item = auctions.get(newBidRequest.getItem().getItemID());
                }

                try {
                    item.addBid(bid);

                    this.responseWorker.queueResponse(this, socket, new BidAcknowledgedMessage(null, item));

                    System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][BID]\t" +
                            "New bid on item "
                            + item.getTitle() + ". Value: " + newBidRequest.getBid().getBidAmmount());

                } catch (InvalidBidException e) {
                    this.responseWorker.queueResponse(this, socket, new BidFailedMessage(null, item));
                }

            } else if (message.type() == MessageType.DATA_REQUEST) {
                DataRequest dataRequest = (DataRequest) message;

                if (dataRequest.data_req_type() == DataRequestType.AUCTIONS_REQ) {
                    responseWorker.queueResponse(this, socket, new DataReceivedMessage(null, DataRequestType.AUCTIONS_RECV, auctions));
                } else if (dataRequest.data_req_type() == DataRequestType.BIDS_REQ) {

                }
            } else if (message.type() == MessageType.CREATE_AUCTION_REQUEST) {
                CreateAuctionRequest auctionRequest = (CreateAuctionRequest) message;

                if (!this.newAuction(auctionRequest.getAuction())) {
                    System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][ERR]\tError adding new auction:");
                    return;
                }
                responseWorker.queueResponse(this, socket, new DataReceivedMessage(null, DataRequestType.AUCTIONS_RECV, auctions));
                System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][AUC]\t" +
                        "New auction: " + auctionRequest.getAuction().getTitle() + ". Price: " + auctionRequest.getAuction().getReservePrice());
            }
        }
        catch (PasswordStorage.CannotPerformOperationException e)
        {
            System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][ERR]\tPassword hasher error: " + e.getMessage());
        }
        catch (PasswordStorage.InvalidHashException e)
        {
            System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][ERR]\tInvalid hash: " + e.getMessage());
        }
        finally {
            this.clients.put(message.getSender(), socket);
        }
    }


    private boolean newAuction(Item auction)
    {
        try {
            auction.startAuction();
            auctions.put(auction.getItemID(), auction);
            return true;
        }
        catch (Exception e) {
            System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][ERR]\tError adding new auction: " + e.getMessage());
        }
        return false;
    }

    protected boolean validCredentials(User user, String password)
            throws PasswordStorage.InvalidHashException, PasswordStorage.CannotPerformOperationException
    {

        if (!passwords.keySet().contains(user)) return false;

        if (PasswordStorage.verifyPassword(password, passwords.get(user)))
            return true;

        return false;
    }

    void newClient(Object user, SocketChannel socketChannel)
    {
        this.clients.put((User) user, socketChannel);
    }

    @SuppressWarnings("unchecked")
    private void configureServer()
    {
        try {
            passwords = (HashMap<User, String>) DataPersistance.loadData(DataPersistance.LOAD_USERS);
            auctions = (HashMap<Integer, Item>) DataPersistance.loadData(DataPersistance.LOAD_AUCTIONS);
        }
        catch (ClassNotFoundException e) {
            System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][ERR]\tCorrupted file store.");
        }
        catch (IOException e) {
            System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][ERR]\tStored user data could not be loaded.");
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() ->{
            try {
                System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][SRV]\tServer shutting down...");
                DataPersistance.storeData(passwords, auctions);
                System.out.println("Bye.");
            }
            catch (IOException e) {
                System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][ERR]\tFATAL_ERR: Unable to store session data. Reason: " + e.getMessage());
            }
        }));
    }

    public static void main(String[] args)
    {
        try {
            new Server(8080);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
