package org.bogdanbuduroiu.auction.server.controller;

import org.bogdanbuduroiu.auction.model.Item;
import org.bogdanbuduroiu.auction.model.User;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bogdanbuduroiu on 05.05.16.
 */
public class DataPersistance {

    private static final String DIR_PATH = "../server/data";
    private static final String USERS_REL_PATH = "users.dat";
    private static final String AUCTIONS_REL_PATH = "auctions.dat";

    public static final int LOAD_USERS = 1;
    public static final int LOAD_AUCTIONS = 2;

    @SuppressWarnings("unchecked")
    public static Object loadData(int type) throws IOException, ClassNotFoundException {

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
        return null;
    }

    public static void storeData(Map<User, String> passwords, Map<Integer, Item> auctions) throws IOException {
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
    }}
