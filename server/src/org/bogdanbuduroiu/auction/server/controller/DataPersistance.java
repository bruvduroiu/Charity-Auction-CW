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
public class DataPersistance {

    private static final String DIR_PATH = "../server/data";
    private static final String USERS_REL_PATH = "users.dat";
    private static final String AUCTIONS_REL_PATH = "auctions.dat";
    private static final String WON_AUCTIONS_REL_PATH = "won_auctions.dat";

    public static enum LoadType {
        LOAD_USERS, LOAD_AUCTIONS, LOAD_WON_AUCTIONS
    }

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

    public static void storeData(Map<User, String> passwords, Map<Integer, Item> auctions, Map<User, Set<Item>> won_auctions) throws IOException {
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

        file = new File(DIR_PATH, WON_AUCTIONS_REL_PATH);
        if (!file.exists()) {
            file = new File(DIR_PATH, WON_AUCTIONS_REL_PATH);
            file.createNewFile();
        }

        oos = new ObjectOutputStream(new FileOutputStream(file));
        oos.writeObject(won_auctions);
        oos.close();
    }}
