package org.bogdanbuduroiu.auction.client.controller;

import org.bogdanbuduroiu.auction.model.comms.message.Message;

import java.nio.ByteBuffer;

/**
 * Created by bogdanbuduroiu on 29.04.16.
 */

/**
 * Waits for response from server.
 * Implemented from: http://rox-xmlrpc.sourceforge.net/niotut/
 */
public class ResponseHandler {

    private Message response;
    private Client client;

    public synchronized boolean handleResponse(Message response) {
        this.response = response;
        this.notify();
        return true;
    }

    public synchronized void waitForResponse(Client client) {

        this.client = client;

        while (this.response == null) {
            try {
                this.wait();
            }
            catch (InterruptedException e) {}
        }
        client.processMessage(response);
    }
}
