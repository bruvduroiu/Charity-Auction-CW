package org.bogdanbuduroiu.auction.client.view;

import org.bogdanbuduroiu.auction.client.controller.Client;
import org.bogdanbuduroiu.auction.client.utils.DateLabelFormatter;
import org.bogdanbuduroiu.auction.model.Category;
import org.bogdanbuduroiu.auction.model.Item;
import org.bogdanbuduroiu.auction.model.User;
import org.bogdanbuduroiu.auction.model.comms.message.DataRequestType;
import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

/**
 * Created by bogdanbuduroiu on 29.04.16.
 */
public class MainAuctionScreen extends JFrame {
    private static final int WIDTH = 1100;
    private static final int HEIGHT = 600;
    private static final String TITLE = "jBay - Overview";
    private Client client;
    private JTabbedPane pnl_tabbed;
    private BrowsePanel pnl_auctions;
    private SellPanel pnl_newAuction;
    private Object[][] auctionData;

    private final HashMap<String, Category> CATEGORIES = new HashMap<String, Category>() {{
        put("Audio & Video", Category.AUDIO_VIDEO);
        put("Auto", Category.AUTO);
        put("Home & Garden", Category.HOME_GARDEN);
        put("Health & Nutrition", Category.HEALTH_NUTRITION);
        put("Jewelry", Category.JEWELRY);
        put("Sports & Outdoor", Category.SPORTS_OUTDOOR);
        put("Watches", Category.WATCHES);
    }};
    private MainAuctionScreen(Client client) {
        //TODO: Sort the Categories List
        this.client = client;

        pnl_tabbed = new JTabbedPane(SwingConstants.BOTTOM, JTabbedPane.SCROLL_TAB_LAYOUT);
        pnl_auctions = new BrowsePanel();
        pnl_newAuction = new SellPanel();


        init();
        initListeners();
    }

    public static MainAuctionScreen initializeScreen(Client client) {
        return new MainAuctionScreen(client);
    }


    private void init() {

        pnl_tabbed.addTab("Browse", pnl_auctions);
        pnl_tabbed.addTab("Sell", pnl_newAuction);

        setContentPane(pnl_tabbed);
        setSize(WIDTH, HEIGHT);
        setTitle(TITLE);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void initListeners() {
        client.addAuctionDataReceivedListener((data) -> {
            auctionData = processAuctionData(data, null);
            pnl_auctions.loadAuctions();
        });
    }

    private Object[][] processAuctionData(Set<Item> auctions, User user) {
        Object[][] result = new Object[auctions.size()][];

        int i = 0;
        for (Item item : auctions) {
            if (user == null || user.getUserID() == item.getVendorID()) {
                DateFormat dateFormat = new SimpleDateFormat("hh:mm:ss");
                result[i++] = new Object[]{item.getTitle(), item.getDescription(), item.getBids().size(), item.getVendorID(), dateFormat.format(item.getTimeRemaining()), item.getReservePrice()};
            }
        }

        return result;
    }

    class BrowsePanel extends JPanel {

        private JTextField txt_searchField;
        private JComboBox<String> cmb_searchCategory;
        private JButton btn_submitSearch;
        private JLabel lbl_user;
        private JLabel lbl_categories;
        private JList<String> lst_categories;
        private JTable tbl_auctions;
        private JScrollPane scrl_auctions;

        public BrowsePanel() {
            String[] categories = CATEGORIES.keySet().toArray(new String[CATEGORIES.size()]);

            txt_searchField = new JTextField("Search", 20);

            cmb_searchCategory = new JComboBox<>(categories);

            btn_submitSearch = new JButton("Search");

            lbl_user = new JLabel(client.getCurrentUser().getUsername());
            lbl_categories = new JLabel("Categories");

            lst_categories = new JList<>(categories);

            tbl_auctions = new JTable();

            scrl_auctions = new JScrollPane(tbl_auctions);
            scrl_auctions.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            init();
        }

        void init() {

            setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.NORTHWEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weighty = 0.1;
            c.weightx = 1;
            c.gridx = 1;
            c.gridy = 1;
            c.gridwidth = 2;
            c.gridheight = 2;
            add(txt_searchField,c);
            c.gridx = 3;
            c.gridy = 1;
            c.gridwidth = 1;
            c.gridheight = 2;
            add(cmb_searchCategory, c);
            c.gridx = 4;
            c.gridy = 1;
            c.gridwidth = 1;
            c.gridheight = 2;
            add(btn_submitSearch, c);
            c.gridx = 5;
            c.gridy = 1;
            c.gridwidth = 1;
            c.gridheight = 1;
            add(lbl_user, c);
            c.fill = GridBagConstraints.BOTH;
            c.weighty = 0.9;
            c.weightx = 0.3;
            c.gridx = 1;
            c.gridy = 2;
            c.gridwidth = 1;
            add(lst_categories, c);
            c.fill = GridBagConstraints.BOTH;
            c.weighty = 0.9;
            c.weightx = 0.7;
            c.gridx = 2;
            c.gridy = 2;
            c.gridwidth = 5;
            add(scrl_auctions, c);
        }

        public void loadAuctions() {
            //TODO: Implement loading items from method
            DateFormat dateFormat = new SimpleDateFormat("hh:mm:ss");
            String[] columnName = {"Title", "Description", "No. Bidders", "Seller", "Time Remaining", "Price"};
            DefaultTableModel model = new DefaultTableModel(auctionData, columnName);
            tbl_auctions.setModel(model);
            tbl_auctions.setRowHeight(64);
            tbl_auctions.getColumnModel().getColumn(1).setMinWidth(300);
        }
    }

    class SellPanel extends JPanel {

        private JPanel pnl_myAuctions;
        private JPanel pnl_sell;
        private Object[][] usr_auctions;
        JTable tbl_myAuctions = new JTable();


        public SellPanel() {
            pnl_myAuctions = new JPanel(new BorderLayout(10,10));
            pnl_sell = new JPanel(new GridBagLayout());


            add(pnl_myAuctions);
            add(pnl_sell);

            initAuctionsPanel();
            initSellPanel();
        }

        private void initSellPanel() {

            String[] categories = CATEGORIES.keySet().toArray(new String[CATEGORIES.size()]);

            JLabel lbl_title = new JLabel("Title");
            JLabel lbl_description = new JLabel("Description");
            JLabel lbl_category = new JLabel("Category");
            JLabel lbl_vendor = new JLabel("Vendor");
            JLabel lbl_expiryTime = new JLabel("Close date");
            JLabel lbl_reservePrice = new JLabel("Reserve Price");
            JLabel lbl_itemImage = new JLabel("Image");
            JLabel lbl_err = new JLabel();

            JTextField txt_title = new JTextField(20);
            JTextField txt_vendor = new JTextField(20);
            JTextField txt_reservePrice = new JTextField(20);

            Properties p = new Properties();
            p.put("text.today", "Today");
            p.put("text.month", "Month");
            p.put("text.year", "Year");

            JDatePanelImpl datePanel = new JDatePanelImpl(new UtilDateModel(), p);
            JDatePickerImpl pck_datePicker = new JDatePickerImpl(datePanel, new DateLabelFormatter());

            lbl_err.setForeground(Color.RED);

            JTextArea txt_description = new JTextArea(6,20);
            txt_description.setWrapStyleWord(true);

            JList<String> lst_categories = new JList<>(categories);

            JButton btn_submit = new JButton("Submit");

            pnl_sell.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Sell"
            ));

            setLayout(new GridLayout(1,2));
            GridBagConstraints c = new GridBagConstraints();

            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 0.6;

            c.gridx = 1;
            c.gridy = 1;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.insets = new Insets(20,20,20,20);
            pnl_sell.add(lbl_title, c);

            c.gridx = 2;
            c.gridy = 1;
            c.gridwidth = 2;
            c.gridheight = 1;
            pnl_sell.add(txt_title, c);

            c.gridx = 1;
            c.gridy = 2;
            c.gridwidth = 1;
            c.gridheight = 1;
            pnl_sell.add(lbl_description, c);

            c.gridx = 2;
            c.gridy = 2;
            c.gridwidth = 3;
            c.gridheight = 3;
            pnl_sell.add(txt_description, c);

            c.gridx = 1;
            c.gridy = 7;
            c.gridwidth = 1;
            c.gridheight = 1;
            pnl_sell.add(lbl_category, c);

            c.gridx = 2;
            c.gridy = 7;
            c.gridwidth = 3;
            c.gridheight = 3;
            pnl_sell.add(lst_categories, c);

            c.gridx = 1;
            c.gridy = 10;
            c.gridwidth = 1;
            c.gridheight = 1;
            pnl_sell.add(lbl_expiryTime, c);

            c.gridx = 1;
            c.gridy = 11;
            c.gridwidth = 3;
            c.gridheight = 1;
            pnl_sell.add(pck_datePicker, c);

            c.gridx = 1;
            c.gridy = 12;
            c.gridwidth = 1;
            c.gridheight = 1;
            pnl_sell.add(lbl_reservePrice, c);

            c.gridx = 2;
            c.gridy = 12;
            c.gridwidth = 2;
            c.gridheight = 1;
            //TODO: Implement error handling
            pnl_sell.add(txt_reservePrice, c);

            c.gridx = 3;
            c.gridy = 13;
            c.gridwidth = 1;
            c.gridheight = 1;
            pnl_sell.add(btn_submit, c);

            btn_submit.addActionListener((e) -> {
                try {
                    Item newItem = new Item(
                            txt_title.getText(),
                            txt_description.getText(),
                            CATEGORIES.get(lst_categories.getSelectedValue()),
                            client.getCurrentUser().getUserID(),
                            ((Date) pck_datePicker.getModel().getValue()).toInstant().toEpochMilli(),
                            Double.parseDouble(txt_reservePrice.getText())
                    );

                    client.newAuction(newItem);
                    JOptionPane.showMessageDialog(null, "Auction Created Successfully!");
                }
                catch (IOException ioe) {
                    System.out.println("[ERR]\tError occurred when submitting new auction. " + ioe.getMessage());
                    ioe.printStackTrace();
                }
            });
        }

        private void initAuctionsPanel() {

            client.addAuctionDataReceivedListener((data) -> {
                usr_auctions = processAuctionData(data, client.getCurrentUser());
                loadUsrAuctions();
            });

            pnl_myAuctions.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "My Auctions"
            ));

            pnl_myAuctions.add(tbl_myAuctions, BorderLayout.CENTER);

        }



        private void loadUsrAuctions() {

            DateFormat dateFormat = new SimpleDateFormat("hh:mm:ss");
            String[] columnName = {"ID", "Title", "No. Bidders", "Time Remaining", "Highest Bid", "Cancel"};
            DefaultTableModel model = new DefaultTableModel(usr_auctions, columnName);
            tbl_myAuctions.setModel(model);
            tbl_myAuctions.setRowHeight(64);
            tbl_myAuctions.getColumnModel().getColumn(1).setMinWidth(150);
        }
    }
}
