package org.bogdanbuduroiu.auction.server.controller;


import org.bogdanbuduroiu.auction.model.Bid;
import org.bogdanbuduroiu.auction.model.Category;
import org.bogdanbuduroiu.auction.model.Item;
import org.bogdanbuduroiu.auction.model.User;
import org.bogdanbuduroiu.auction.model.comms.message.*;
import org.bogdanbuduroiu.auction.model.exception.InvalidBidException;
import org.bogdanbuduroiu.auction.server.security.PasswordStorage;
import org.bogdanbuduroiu.auction.server.view.ServerGUI;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.SocketChannel;
import java.time.ZonedDateTime;
import java.util.*;

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

    Map<User, Set<Item>> wonAuctions = new HashMap<>();

    Map<User, Set<Item>> won_auctions_Report = new HashMap<>();

    ServerGUI serverGUI;

    public Server(int port) throws IOException
    {
        this.port = port;

        this.serverGUI = new ServerGUI(this);

        try {
            SwingUtilities.invokeAndWait(() -> this.serverGUI.init());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        this.commsWorker = new ServerComms(this);

        this.responseWorker = new ResponseWorker();

        this.auctionGraveWorker = new AuctionGraveWorker(this);

        System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][SRV]\tStarting server...");

        this.configureServer();

        new Thread(this.commsWorker).start();
        new Thread(this.responseWorker).start();
        new Thread(this.auctionGraveWorker).start();

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

            this.auctionGraveWorker.closeExpiredAuctions();

            if (message.type() == MessageType.LOGIN_REQUEST) {

                LoginRequest lr = (LoginRequest) message;
                User tmpUser = lr.getUser();

                // Hashing password provided at Login Screen and comparing to stored hash.
                // NOTE: Hashing passwords drastically decreases Server performance
                if (!this.validCredentials(lr.getUser(), new String(lr.getPassword()))) {
                    // Inform the client of the failed login attempt
                    this.responseWorker.queueResponse(this, socket, new ErrMessage(null, ErrType.INVALID_LOGIN_ERR));

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
                this.responseWorker.queueResponse(this, socket, new AcknowledgedMessage(null, AckType.ACK_LOGIN));

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
                if (this.passwords.keySet().contains(tmpUser)) {

                    // Inform client of already existing alias
                    this.responseWorker.queueResponse(this, socket, new ErrMessage(null, ErrType.USER_EXISTS_ERR));
                    return;
                }

                // Add the new user, together with their password
                this.passwords.put(tmpUser, hashedPassword);

                // Inform client of the successful registration
                this.responseWorker.queueResponse(this, socket, new AcknowledgedMessage(null, AckType.ACK_REGISTRATION));

                // Log the new registration with the current timestamp and host address.
                System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][USR]\t" +
                        "New registration. Username: " + tmpUser.getUsername() +"; Host: " + socket.socket().getInetAddress());

            } else if (message.type() == MessageType.NEW_BID_REQUEST) {

                NewBidRequest newBidRequest = (NewBidRequest) message;
                Bid bid = newBidRequest.getBid();
                Item item;

                // Avoid race conditions when trying to fetch item
                synchronized (auctions) {
                    item = this.auctions.get(newBidRequest.getItem().getItemID());
                }


                try {
                    item.addBid(bid);

                    this.responseWorker.queueResponse(this, socket, new BidAcknowledgedMessage(null, item));

                    // Log the new bid with the current timestamp and host address.
                    System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][BID]\t" +
                            "New bid on item "
                            + item.getTitle() + ". Value: " + newBidRequest.getBid().getBidAmmount());

                    // In case the bid is lower than the current highest bid, send a BidFailedMessage response to user
                } catch (InvalidBidException e) {
                    this.responseWorker.queueResponse(this, socket, new BidFailedMessage(null, item));
                }

            } else if (message.type() == MessageType.DATA_REQUEST) {
                DataRequest dataRequest = (DataRequest) message;

                DataReceivedMessage dataReceivedMessage;

                if (dataRequest.data_req_type() == DataRequestType.AUCTIONS_REQ)
                    // Client requests Auction data
                    synchronized (this.wonAuctions) {
                        dataReceivedMessage =
                                new DataReceivedMessage(null,
                                        DataReceivedType.AUCTIONS_RECV,
                                        auctions,
                                        wonAuctions.get(dataRequest.getSender()));

                        // Store the current won auctions for logging & reporting purposes (See Part Five of the Spec)
                        this.appendToLoggedWins(this.wonAuctions);

                        // Reset the wonAuctions set for this client. Prevents client
                        // being notified about wins every time it requests data
                        this.wonAuctions.put(dataRequest.getSender(), new HashSet<>());
                    }
                else
                    // Client requests bid data
                    dataReceivedMessage =
                            new DataReceivedMessage(null, DataReceivedType.BIDS_RECV, auctions);

                // Queue a DataReceivedResponse based on the DataRequest
              this.responseWorker.queueResponse(this, socket, dataReceivedMessage);

            } else if (message.type() == MessageType.CREATE_AUCTION_REQUEST) {
                CreateAuctionRequest auctionRequest = (CreateAuctionRequest) message;

                // If the auction could not be added for any reason, return.
                //  Note: we deal with the logging of the error within the newAuction() method.
                if (!this.newAuction(auctionRequest.getAuction())) {
                    return;
                }

                // Otherwise, acknowledge the auction...
                this.responseWorker.queueResponse(this, socket, new DataReceivedMessage(null, DataReceivedType.AUCTIONS_RECV, auctions));

                // ...and log the transaction together with timestamp and auction information
                System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][AUC]\t" +
                        "New auction: " + auctionRequest.getAuction().getTitle() + ". Price: " + auctionRequest.getAuction().getReservePrice());
            }
        }

        // Catches error thrown by the PasswordStorage class in case the system is malfunctioning
        //      e.g. The system's randon number generator is not working properly
        catch (PasswordStorage.CannotPerformOperationException e)
        {
            // Log the error that occured with the current timestamp
            System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][ERR]\tPassword hasher error: " + e.getMessage());
        }

        // Catches errors thrown by the PasswordStorage class in case the correctHash has been corrupted.
        catch (PasswordStorage.InvalidHashException e)
        {

            // Log the error that occured with the current timestamp
            System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][ERR]\tInvalid hash: " + e.getMessage());
        }
        finally {

            // Keeping track of the current socket that the client is sending messages on.
            // For some reason, I need to keep track of this because my client is always
            //  opening / closing connections everytime it sends a message.
            // Tried a workaround... no success.
            this.clients.put(message.getSender(), socket);
        }
    }

    private boolean newAuction(Item auction)
    {
        try {

            // Starting the auction involves setting a timestamp on it
            auction.startAuction();

            // Track the new auction in the server's auctions HashMap
            this.auctions.put(auction.getItemID(), auction);

            // New auction creation successful, yaay!
            return true;
        }

        // In case any errors occur during this creation, log them and return false.
        catch (Exception e) {
            System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][ERR]\tError adding new auction: " + e.getMessage());
        }
        return false;
    }

    /**
     * Returns category-filtered auction data to the client
     *
     * @param category Category that the user has filtered by
     * @return Map of all auctions in the filtered category
     */
    private Map<Integer, Item> filterAuctions(Category category) {
        Map<Integer, Item> filteredAuctions = new HashMap<>();

        Set<Map.Entry<Integer, Item>> entries;

        // Synchronize access of the entrySet to prevent any concurrency issues
        synchronized (this.auctions) {
             entries = this.auctions.entrySet();
        }

        // Check through the items and only add those that have the correct category
        for (Map.Entry<Integer, Item> entry : entries)
            if (entry.getValue().getCategory().equals(category))
                filteredAuctions.put(entry.getKey(), entry.getValue());

        return filteredAuctions;
    }

    /**
     * Returns keyword-filtered auction data to the client
     * Checks if either title, description or vendorName of the item
     * contain said keyword.
     *
     * @param keyword Keyword that the user has filtered by
     * @return Map of all auctions containing said keyword.
     */
    private Map<Integer, Item> filterAuctions(String keyword)
    {
        Map<Integer, Item> filteredAuctions = new HashMap<>();

        Set<Map.Entry<Integer, Item>> entries;

        // Synchronize access of the entrySet to prevent any concurrency issues
        synchronized (this.auctions) {
            entries = this.auctions.entrySet();
        }

        // Check through the items and only add those that contain the keyword
        for (Map.Entry<Integer, Item> entry : entries)
            if (entry.getValue().getTitle().contains(keyword)
                    || entry.getValue().getDescription().contains(keyword))
                filteredAuctions.put(entry.getKey(), entry.getValue());
        return filteredAuctions;
    }

    /**
     * Checks the received user password (from client) against the stored one.
     *      It hashes the received password (using a salt) and compares it to the
     *      stored hash.
     *
     * @param user User requesting login
     * @param password Password of user requesting login
     * @return Returns true or false depending on whether the hashes match or not
     * @throws PasswordStorage.InvalidHashException Thrown when the stored hash has been corrupted
     * @throws PasswordStorage.CannotPerformOperationException Thrown if the system is malfunctioning (e.g. random number generator malfunction)
     */
    protected boolean validCredentials(User user, String password)
            throws PasswordStorage.InvalidHashException, PasswordStorage.CannotPerformOperationException
    {
        // Don't even bother checking password if the user is not registered
        if (!this.passwords.keySet().contains(user)) return false;

        return PasswordStorage.verifyPassword(password, passwords.get(user));
    }

    /**
     * Closes an auction, providing a message to either the winner or vendor depending on whether or not
     * the auction has any bids higher than the reserve price.
     *
     * @param auction Auction to be closed.
     */
    protected void closeAuction(Item auction) {
        try {

            // First part of the confirmation message (append either winner or vendor later on)
            String confirmationMessage = "[" + Date.from(ZonedDateTime.now().toInstant()) + "][AUC]\tAuction Id: " +
                    auction.getItemID() + " Title: " + auction.getTitle();

            // Since the initial highest bid is the vendor's reserve price,
            // the winner variable will store the vendor in case there are no bids over the reserve price.
            User winner = auction.getBids().peek().getUser();

            // Set closeTime on the auction and set it's closed flag to TRUE
            auction.closeAuction();
            synchronized (this.wonAuctions) {

                // Get the set of items won by this winner and append this item to it.
                //      See: wonItems comments at the top.
                //          wonItems stores the pending notifications to be sent to the client mapped in the key.
                Set<Item> wonItems = (this.wonAuctions.get(winner) == null)
                        ? new HashSet<>()                       // create a new HashSet if the current winner has no other won items
                        : this.wonAuctions.get(winner);         // get existing HashSet otherwise

                wonItems.add(auction);
                this.wonAuctions.put(winner, wonItems);
            }

            // Only one bid means that the highest bid is the vendor's reserve price. Return notification to the vendor
            if (auction.getBids().size() == 1) {
                confirmationMessage = confirmationMessage + " has no bids higher than reserve price. Notifying Vendor " + winner.getUsername();
            }

            // Otherwise, notify winner.
            else {
                confirmationMessage = confirmationMessage + " has successfully been won by user " + winner.getUsername();
            }
            System.out.println(confirmationMessage);
        }

        // Log any errors
        catch (Exception e) {
            System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][ERR]\t" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * On client login, map the user to the SocketChannel they used to communicate
     *
     * @param user User that logged in
     * @param socketChannel User's SocketChannel used for communicating
     */
    void newClient(Object user, SocketChannel socketChannel)
    {
        this.clients.put((User) user, socketChannel);
    }

    /**
     * Loads all the stored data (Auctions, Bids, Pending notifications(in form of wonAuctions))
     */
    @SuppressWarnings("unchecked")
    private void configureServer()
    {
        try {
            this.passwords = (HashMap<User, String>) DataPersistance.loadData(DataPersistance.LoadType.LOAD_USERS);
            this.auctions = (HashMap<Integer, Item>) DataPersistance.loadData(DataPersistance.LoadType.LOAD_AUCTIONS);
            this.wonAuctions = (HashMap<User, Set<Item>>) DataPersistance.loadData(DataPersistance.LoadType.LOAD_WON_AUCTIONS);
        }
        catch (ClassNotFoundException e) {
            System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][ERR]\tCorrupted file store.");
        }
        catch (IOException e) {
            System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][ERR]\tStored user data could not be loaded.");
        }


        // Add a Shutdown Hook to save the current configuration of the server to file on shutdown.
        Runtime.getRuntime().addShutdownHook(new Thread(() ->{
            try {
                System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][SRV]\tServer shutting down...");
                DataPersistance.storeData(this.passwords, this.auctions, this.wonAuctions, this.serverGUI.getTxt_console().getText());
                System.out.println("Bye.");
            }
            catch (IOException e) {
                System.out.println("[" + Date.from(ZonedDateTime.now().toInstant()) + "][ERR]\tFATAL_ERR: Unable to store session data. Reason: " + e.getMessage());
            }
        }));
    }

    private void appendToLoggedWins(Map<User, Set<Item>> wonAuctions) {

        for (User user : wonAuctions.keySet()) {
            if (!this.won_auctions_Report.keySet().contains(user)) {
                this.won_auctions_Report.put(user, wonAuctions.get(user));
                continue;
            }
            for (Item item : wonAuctions.get(user))
                if (!this.won_auctions_Report.get(user).contains(item))
                    this.won_auctions_Report.get(user).add(item);
        }
    }

    /**
     * Return the HashMap of won auction
     *
     * @return HashMap of won Auctions
     */
    public HashMap<User, Set<Item>> getWonAuctions() {
        HashMap<User, Set<Item>> copy;

        // Synchronizing to avoid concurrency issues when returning the won auctions
        // I am grabbing a copy of the wonAuctions Map to reduce the amount of operations
        // held in the synchronized blod.
        synchronized (this.won_auctions_Report) {
            copy = new HashMap<>(this.won_auctions_Report);
        }

        return copy;
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
