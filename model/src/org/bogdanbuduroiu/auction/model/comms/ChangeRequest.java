package org.bogdanbuduroiu.auction.model.comms;

/**
 * Created by bogdanbuduroiu on 28.04.16.
 */

import java.nio.channels.SocketChannel;

/**
 * The idea for implementing ChangeRequests came from this tutorial:
 * http://rox-xmlrpc.sourceforge.net/niotut
 *
 * Although this was not my idea, I implemented it myself after reading the tutorial.
 * This did not involve copying the code in the tutorial, although the code provided
 * helped me tremendously when implementing a non-blocking I/O server-client pair.
 *
 * The idea behind the ChangeRequest is that you queue multiple ChangeRequests, each with
 * an Interest in some operation: OP_CONNECT, OP_WRITE, OP_READ, OP_ACCEPT.
 *
 * In the run() method of the client/server, constantly check for the queued ChangeRequests and
 * create a SelectionKey for each of them, with their specified Interest. Pretty smart if you ask me!
 */
public class ChangeRequest {

    public static final int REGISTER = 1;
    public static final int CHANGEOPS = 2;

    public SocketChannel socket;
    public int type;
    public int ops;

    public ChangeRequest(SocketChannel socket, int type, int ops) {
        this.socket = socket;
        this.type = type;
        this.ops = ops;
    }
}
