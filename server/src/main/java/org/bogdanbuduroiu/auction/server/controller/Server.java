package org.bogdanbuduroiu.auction.server.controller;


import org.bogdanbuduroiu.auction.model.Category;
import org.bogdanbuduroiu.auction.model.Item;
import org.bogdanbuduroiu.auction.model.User;
import org.bogdanbuduroiu.auction.model.comms.message.*;

import java.io.*;
import java.nio.channels.SocketChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.*;

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
    Set<Item> auctions = new HashSet<>();
    private static final String DIR_PATH = "../server/data";
    private static final String USERS_REL_PATH = "users.dat";

    public Server(int port) throws IOException{
        this.port = port;
        commsWorker = new ServerComms(this);
        responseWorker = new ResponseWorker();
        System.out.println("[SRV]\tStarting server...");
        configureServer();
        testAuctions();
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
                    System.out.println("[USR]\tFailed login attempt. User: " + tmpUser.getUsername());
                    return;
                }
                this.activeUsers.add(tmpUser);
                this.clients.put(tmpUser,socket);
                responseWorker.queueResponse(this, socket, new AcknowledgedMessage(tmpUser, AckType.ACK_LOGIN));
                System.out.println("[USR]\tNew login from user " + tmpUser.getUsername() + " at " + Date.from(ZonedDateTime.now().toInstant()) + ".");
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
                System.out.println("[USR]\tNew registration. Username: " + tmpUser.getUsername() + " at " + Date.from(ZonedDateTime.now().toInstant()) + ".");
            }
            else if (message.type() == MessageType.DATA_REQUEST) {
                DataRequest dataRequest = (DataRequest) message;
                if (dataRequest.data_req_type() == DataRequestType.AUCTIONS_REQ) {
                    responseWorker.queueResponse(this, socket, new DataReceivedMessage(null, DataRequestType.AUCTIONS_RECV, processAuctionData()));
                }
                else if (dataRequest.data_req_type() == DataRequestType.BIDS_REQ) {

                }
            }
    }

    private void testAuctions() {
        auctions.add(new Item("Test1", "Test Descript1", Category.AUTO, 123124, System.currentTimeMillis() + 100000, 100.20, null));
        auctions.add(new Item("Test2", "Test Descript2", Category.AUTO, 123124, System.currentTimeMillis() + 100000, 100.20, null));
        auctions.add(new Item("Test3", "Test Descript3", Category.AUTO, 123124, System.currentTimeMillis() + 100000, 100.20, null));
    }

    private Object[][] processAuctionData() {
        Object[][] result = new Object[auctions.size()][];

        int i = 0;
        for (Item item : auctions) {
            DateFormat dateFormat = new SimpleDateFormat("hh:mm:ss");
            result[i++] = new Object[]{item.getTitle(), item.getDescription(), item.getVendorID(), dateFormat.format(item.getTimeRemaining()), item.getReservePrice()};
        }

        return result;
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
        File file = new File(new File(DIR_PATH), USERS_REL_PATH);
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
        passwords = (HashMap<User, char[]>) ois.readObject();
        ois.close();
    }

    private void storeData() throws IOException {
        File file = new File(new File(DIR_PATH), USERS_REL_PATH);
        if (!(new File(new File(DIR_PATH), USERS_REL_PATH).exists())) {
            File dir = new File(DIR_PATH);
            dir.mkdir();
            file = new File(dir, USERS_REL_PATH);
            file.createNewFile();
        }
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
        oos.writeObject(passwords);
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
