import org.bogdanbuduroiu.auction.server.controller.Server;

import java.io.IOException;

/**
 * Created by bogdanbuduroiu on 02.05.16.
 */
public class ServerMain {
    public static void main(String[] args) {
        try {
            new Server(8080);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
