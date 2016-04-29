package org.bogdanbuduroiu.auction.server.controller;


import org.bogdanbuduroiu.auction.model.User;
import org.bogdanbuduroiu.auction.model.comms.message.*;

import java.io.IOException;
import java.nio.channels.SocketChannel;
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
    Map<String, User> registeredUsers = new HashMap<>();
    Set<User> activeUsers = new HashSet<>();

    public Server(int port) throws IOException{
        this.port = port;
        commsWorker = new ServerComms(this);
        responseWorker = new ResponseWorker();
        System.out.println("[SRV]\tStarting server...");
        new Thread(commsWorker).start();
        new Thread(responseWorker).start();
        System.out.println("[SRV]\tServer initialized on port " + this.port + ".");
    }

    public void processMessage(SocketChannel socket, Message message) {
            if (message.type() == MessageType.LOGIN_REQUEST) {
                LoginRequest lr = (LoginRequest) message;
                User tmpUser = lr.getUser();
                if (!validCredentials(tmpUser, lr.getPassword()))
                    return;
                //TODO: Implement Bad Login
                this.activeUsers.add(tmpUser);
                this.clients.put(tmpUser,socket);
                responseWorker.queueResponse(this, socket, new AcknowledgedMessage(tmpUser, AckType.ACK_LOGIN));
                System.out.println("[USR]\tNew login from user " + tmpUser.getUsername() + " at " + Date.from(ZonedDateTime.now().toInstant()) + ".");
            }

            if (message.type() == MessageType.REGISTRATION_REQUEST) {
                RegistrationRequest registrationRequest = (RegistrationRequest) message;
                User tmpUser = registrationRequest.getUser();
                char[] password = registrationRequest.getPassword();
                this.registeredUsers.put(tmpUser.getUsername(), tmpUser);
                this.passwords.put(tmpUser, password);
                this.responseWorker.queueResponse(this, socket, new AcknowledgedMessage(tmpUser, AckType.ACK_REGISTRATION));
                System.out.println("[USR]\tNew registration. Username: " + tmpUser.getUsername() + " at " + Date.from(ZonedDateTime.now().toInstant()) + ".");
            }
    }



    protected boolean validCredentials(User user, char[] password) {
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
