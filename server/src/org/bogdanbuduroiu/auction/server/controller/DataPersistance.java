package org.bogdanbuduroiu.auction.server.controller;

import org.bogdanbuduroiu.auction.model.Item;
import org.bogdanbuduroiu.auction.model.User;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.bogdanbuduroiu.auction.server.controller.DataPersistance.LoadType.LOAD_AUCTIONS;
import static org.bogdanbuduroiu.auction.server.controller.DataPersistance.LoadType.LOAD_USERS;
import static org.bogdanbuduroiu.auction.server.controller.DataPersistance.LoadType.LOAD_WON_AUCTIONS;

/**
 * Created by bogdanbuduroiu on 05.05.16.
 */

/**
 * Manages:
 *      1) Loading of the server data stored in the files.
 *      2) Saving of the server's current data to the files.
 *
 * Handles:
 *      - Users
 *      - Auctions
 *      - Won Auctions for which the user has not been notified
 *      - Server's log
 */
public class DataPersistance {

    private static final String DIR_PATH = "../server/data";
    private static final String USERS_REL_PATH = "users.dat";
    private static final String AUCTIONS_REL_PATH = "auctions.dat";
    private static final String WON_AUCTIONS_REL_PATH = "won_auctions.dat";
    private static final String LOG_REL_PATH = "log.txt";

    // Enum identifies the type of data that the server is expecting
    public enum LoadType {
        LOAD_USERS, LOAD_AUCTIONS, LOAD_WON_AUCTIONS
    }

    /**
     * Loads the stored server data
     *
     * @param type Type of data the server is expecting (includes: Users, Auctions, Won Auctions)
     * @return Depends on the requested LoadType. See below:
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    public static Object loadData(LoadType type) throws IOException, ClassNotFoundException {

        File file;
        ObjectInputStream ois;

        if (type == LOAD_USERS) {
            file = new File(DIR_PATH, USERS_REL_PATH);
            ois = new ObjectInputStream(new FileInputStream(file));
            Map<User, String> users = (HashMap<User, String>) ois.readObject();
            ois.close();
            return users;
        }

        else if (type == LOAD_AUCTIONS) {
            file = new File(DIR_PATH, AUCTIONS_REL_PATH);
            ois = new ObjectInputStream(new FileInputStream(file));
            Map<Integer, Item> auctions = (HashMap<Integer, Item>) ois.readObject();
            ois.close();
            return auctions;
        }

        else if (type == LOAD_WON_AUCTIONS) {
            file = new File(DIR_PATH, WON_AUCTIONS_REL_PATH);
            ois = new ObjectInputStream(new FileInputStream(file));
            Map<User, Set<Item>> wonAuctions = ((HashMap<User, Set<Item>>) ois.readObject());
            return wonAuctions == null
                    ? new HashMap<User, Set<Item>>()
                    : wonAuctions;
        }
        return null;
    }

    /**
     * Manages the storing of server data. Creates new files in case the files do not exist
     *
     * @param passwords Map of Users to Passwords to be stored (User -> String)
     * @param auctions  Map of all the auctions to be stored (ItemID -> Item)
     * @param won_auctions Map of all the won auctions to be stored (ItemID -> Item)
     * @param log String of the server's log
     * @throws IOException
     */
    public static void storeData(Map<User, String> passwords, Map<Integer, Item> auctions, Map<User, Set<Item>> won_auctions, String log) throws IOException {

        File file = new File(DIR_PATH, USERS_REL_PATH);
        if (!(new File(DIR_PATH)).exists())
            new File(DIR_PATH).mkdir();
        if (!(file.exists()))
            file.createNewFile();

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
        oos.writeObject(passwords);
        oos.close();

        file = new File(DIR_PATH, AUCTIONS_REL_PATH);
        if (!file.exists())
            file.createNewFile();

        oos = new ObjectOutputStream(new FileOutputStream(file));
        oos.writeObject(auctions);
        oos.close();

        file = new File(DIR_PATH, WON_AUCTIONS_REL_PATH);
        if (!file.exists())
            file.createNewFile();

        oos = new ObjectOutputStream(new FileOutputStream(file));
        oos.writeObject(won_auctions);
        oos.close();

        file = new File(DIR_PATH, LOG_REL_PATH);
        if (!file.exists())
            file.createNewFile();

        BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
        bw.write(log);
        bw.flush();
        bw.close();
    }
}
