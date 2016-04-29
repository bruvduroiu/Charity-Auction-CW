package org.bogdanbuduroiu.auction.server.controller;


import org.bogdanbuduroiu.auction.model.comms.ServerComms;
import org.bogdanbuduroiu.auction.model.comms.events.MessageReceivedEvent;
import org.bogdanbuduroiu.auction.model.comms.message.*;
import org.bogdanbuduroiu.auction.model.User;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Created by bogdanbuduroiu on 21.04.16.
 */


public class Server {

    private ServerComms worker;
    private int port;
    private Map<User, char[]> passwords = new HashMap<>();
    private Map<User, SocketChannel> clients = new HashMap<>();
    private Map<String, User> registeredUsers = new HashMap<>();
    private Set<User> activeUsers = new HashSet<>();

    public Server(int port) throws IOException{
        this.port = port;
        worker = new ServerComms();
        System.out.println("[SRV]\tStarting server...");
        this.initListeners();
        new Thread(worker).start();
        System.out.println("[SRV]\tServer initialized on port " + this.port + ".");
    }

    private void initListeners() {
        worker.addMessageReceivedListener(this::processMessage);
    }

    private void processMessage(MessageReceivedEvent messageReceivedEvent) {
        SocketChannel responseSocket = (SocketChannel) messageReceivedEvent.getSource();
        Message message = messageReceivedEvent.getMessage();
        try {
            if (message.type() == MessageType.LOGIN_REQUEST) {
                LoginRequest lr = (LoginRequest) message;
                if (!validCredentials(lr.getUser(), lr.getPassword()))
                    return;
                    //TODO: Implement Bad Login
                worker.sendMessage(responseSocket, new AcknowledgedMessage(null, MessageType.ACK, AckType.ACK_LOGIN));
                System.out.println("[USR]\tNew login from user " + lr.getUser().getUsername() + " at " + Date.from(ZonedDateTime.now().toInstant()) + ".");
            }

            if (message.type() == MessageType.REGISTRATION_REQUEST) {
                RegistrationRequest registrationRequest = (RegistrationRequest) message;
                User tmpUser = registrationRequest.getUser();
                char[] password = registrationRequest.getPassword();
                registeredUsers.put(tmpUser.getUsername(), tmpUser);
                passwords.put(tmpUser, password);
                worker.sendMessage(responseSocket, new AcknowledgedMessage(null, MessageType.ACK, AckType.ACK_NEW_BID));
                System.out.println("[USR]\tNew registration. Username: " + tmpUser.getUsername() + " at " + Date.from(ZonedDateTime.now().toInstant()) + ".");
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean validCredentials(User user, char[] password) {
        String passwd = new String(password);

        if (!passwords.keySet().contains(user)) return false;

        if (passwd.equals(new String(passwords.get(user)))) return true;

        return false;
    }

    private void loadData() {

    }


    public static void main(String[] args) {
        try {
            new Server(8080);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
