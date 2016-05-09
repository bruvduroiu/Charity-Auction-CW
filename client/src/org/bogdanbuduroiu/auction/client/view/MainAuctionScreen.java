package org.bogdanbuduroiu.auction.client.view;

import org.bogdanbuduroiu.auction.client.controller.Client;
import org.bogdanbuduroiu.auction.model.Bid;
import org.bogdanbuduroiu.auction.model.Category;
import org.bogdanbuduroiu.auction.model.Item;
import org.bogdanbuduroiu.auction.model.User;
import org.bogdanbuduroiu.auction.model.comms.message.DataRequestType;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

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
    private Map<Integer, Item> auctionData;
    private Map<Integer, AuctionBidScreen> bidScreensActive = new HashMap<>();

    private final HashMap<String, Category> CATEGORIES = new HashMap<String, Category>() {{
        put("ALL", Category.ALL);
        put("Accessories", Category.ACCESSORIES);
        put("Action Cameras", Category.ACTION_CAMERAS);
        put("Antiques", Category.ANTIQUES);
        put("Audio Video", Category.AUDIO_VIDEO);
        put("Auto", Category.AUTO);
        put("Beauty", Category.BEAUTY);
        put("Clothing", Category.CLOTHING);
        put("Consumables", Category.CONSUMABLES);
        put("Crypto-Currency", Category.CRYPTO_CURRENCY);
        put("E-Books", Category.EBOOKS);
        put("Health & Nutrition", Category.HEALTH_NUTRITION);
        put("Home & Garden", Category.HOME_GARDEN);
        put("Home Networking", Category.HOME_NETWORKING);
        put("Household", Category.HOUSEHOLD);
        put("Jewelry", Category.JEWELRY);
        put("Laptops", Category.LAPTOPS);
        put("Software", Category.SOFTWARE);
        put("Sports & Outdoor", Category.SPORTS_OUTDOOR);
        put("Watches", Category.WATCHES);
        put("Weapons", Category.WEAPONS);
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
            this.auctionData = data;
            pnl_auctions.loadAuctions(processAuctionData(data, null));
        });
    }

    private Object[][] processAuctionData(Map<Integer, Item> auctions, User user) {
        Object[][] result = new Object[auctions.size()][];

        int i = 0;
        for (Item item : auctions.values()) {
            if (item.isClosed())
                continue;
            if (!matchesCategory(item) || !matchesInfo(item))
                continue;
            if (user == null) {
                result[i++] = new Object[]{
                        item.getItemID(),
                        item.getTitle(),
                        item.getDescription(),
                        item.getBids().size() - 1,
                        item.getVendor().getUsername(),
                        item.getTimeRemainingString(),
                        item.getBids().peek().getBidAmmount()};
            }
            else if (user.getUserID() == item.getVendor().getUserID()) {
                result[i++] = new Object[] {
                        item.getItemID(),
                        item.getTitle(),
                        item.getBids().size() - 1,
                        item.getTimeRemainingString(),
                        item.getBids().peek().getBidAmmount(),
                        new JButton("Cancel")};
            }
        }

        return result;
    }

    private boolean matchesInfo(Item item) {
        if (pnl_auctions.filter_keyword.equals(""))
            return true;
        return item.getTitle().contains(pnl_auctions.filter_keyword)
                || item.getDescription().contains(pnl_auctions.filter_keyword);
    }

    private boolean matchesCategory(Item item) {
        if (pnl_auctions.filter_category == Category.ALL)
            return true;
        return item.getCategory() == pnl_auctions.filter_category;
    }

    public void bidSuccess(Item item) {
        JOptionPane.showMessageDialog(null, "Bid on item " + item.getTitle() + " was successful.");
        bidScreensActive.get(item.getItemID()).dispose();
    }

    public void bidFail(Item item) {
        AuctionBidScreen bidScreen = bidScreensActive.get(item);
        bidScreen.setErrorMessage("Error: Your bid must be higher than the current highest bid.");
    }

    class BrowsePanel extends JPanel {

        private JTextField txt_searchField;
        private JComboBox<String> cmb_searchCategory;
        private JButton btn_submitSearch;
        private JLabel lbl_user;
        private JLabel lbl_usrBids;
        private JList<String> lst_categories;
        private JTable tbl_usrBids;
        private JTable tbl_auctions;
        private JScrollPane scrl_auctions;
        private JScrollPane scrl_usrBids;
        private JScrollPane scrl_tblUsrBids;
        private JScrollPane scrl_lstCategories;

        private Category filter_category = Category.ALL;
        private String filter_keyword = "";

        public BrowsePanel() {
            String[] categories = CATEGORIES.keySet().toArray(new String[CATEGORIES.size()]);

            txt_searchField = new JTextField("Search", 20);

            cmb_searchCategory = new JComboBox<>(categories);

            btn_submitSearch = new JButton("Search");

            lbl_user = new JLabel(client.getCurrentUser().getUsername());
            lbl_usrBids = new JLabel("My Bids");

            lst_categories = new JList<>(categories);

            tbl_usrBids = new JTable() {
                private static final long serialVersionUID = 1L;

                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            tbl_auctions = new JTable() {
                private static final long serialVersionUID = 1L;

                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            scrl_auctions = new JScrollPane(tbl_auctions);
            scrl_auctions.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            scrl_usrBids = new JScrollPane(tbl_usrBids);
            scrl_usrBids.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            scrl_tblUsrBids = new JScrollPane(tbl_usrBids);
            scrl_tblUsrBids.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            scrl_lstCategories = new JScrollPane(lst_categories);
            scrl_lstCategories.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

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
            c.weightx = 0.3;
            c.weighty = 0.5;
            c.gridx = 1;
            c.gridy = 2;
            c.gridwidth = 1;
            c.gridheight = 3;
            add(scrl_lstCategories, c);

            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.SOUTHWEST;
            c.weightx = 0.3;
            c.weighty = 0.1;
            c.gridx = 1;
            c.gridy = 5;
            c.gridwidth = 1;
            c.gridheight = 1;
            add(lbl_usrBids, c);

            c.fill = GridBagConstraints.BOTH;
            c.weightx = 0.3;
            c.weighty = 0.6;
            c.gridx = 1;
            c.gridy = 6;
            c.gridwidth = 1;
            c.gridheight = 2;
            add(scrl_tblUsrBids, c);

            c.fill = GridBagConstraints.BOTH;
            c.weighty = 0.9;
            c.weightx = 0.7;
            c.gridx = 2;
            c.gridy = 2;
            c.gridwidth = 5;
            c.gridheight = 7;
            add(scrl_auctions, c);

            tbl_auctions.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    JTable table = (JTable) e.getSource();
                    Point p = e.getPoint();
                    int row = table.rowAtPoint(p);
                    if (e.getClickCount() == 2) {
                        Object[] rowData = new Object[table.getModel().getColumnCount()];
                        for (int i = 0; i < auctionData.size(); i++)
                            rowData[i] = table.getModel().getValueAt(row, i);
                        Item item = auctionData.get(rowData[0]);
                        //TODO: Fix this leading to NullPointerException
                        bidScreensActive.put(item.getItemID(), new AuctionBidScreen(client, item));
                    }
                }
            });

            client.addBidsReceivedListener((auctions, user) -> {

                List<Object[]> bids = new ArrayList<>();
                for (Item item : auctions.values())
                    for (Bid bid : item.getBids())
                        if (bid.getUser().equals(user))
                            bids.add(new Object[] {
                                    item.getItemID(),
                                    item.getTitle(),
                                    bid.getBidAmmount()});

                Object[][] result = new Object[bids.size()][];

                int i = 0;
                for (Object[] bid : bids)
                    result[i++] = bid;
                loadBids(result);
            });

            lst_categories.addListSelectionListener(e -> {
                client.requestData(DataRequestType.AUCTIONS_REQ);
                this.filter_category = CATEGORIES.get(lst_categories.getSelectedValue());
            });

            btn_submitSearch.addActionListener(e -> {
                client.requestData(DataRequestType.AUCTIONS_REQ);
                this.filter_keyword = txt_searchField.getText();
            });
        }

        private void loadBids(Object[][] bidData) {
            String[] columnName = {"ID", "Title", "Bid"};
            DefaultTableModel model = new DefaultTableModel(bidData, columnName);
            tbl_usrBids.setModel(model);
        }


        public void loadAuctions(Object[][] auctionData) {
            String[] columnName = {"ID", "Title", "Description", "No. Bids", "Seller", "Time Remaining", "Price"};
            DefaultTableModel model = new DefaultTableModel(auctionData, columnName);
            tbl_auctions.setModel(model);
            tbl_auctions.setRowHeight(64);
            tbl_auctions.getColumnModel().getColumn(1).setMinWidth(100);
            tbl_auctions.getColumnModel().getColumn(2).setMinWidth(200);
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
            JLabel lbl_expiryTime = new JLabel("Close date");
            JLabel lbl_reservePrice = new JLabel("Reserve Price");
            JLabel lbl_err = new JLabel();

            /**
             * Implementation for the JSpinner that represents the end-time of the auction taken from:
             * http://stackoverflow.com/questions/21960236/jspinner-time-picker-model-editing
             */

            JSpinner spnr_Time;

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 12);
            calendar.set(Calendar.MINUTE, 00);

            SpinnerDateModel model = new SpinnerDateModel();
            model.setValue(calendar.getTime());
            spnr_Time = new JSpinner(model);

            JSpinner.DateEditor editor = new JSpinner.DateEditor(spnr_Time, "dd-MMM-yyyy   h:mm a");
            spnr_Time.setEditor(editor);

            /**
             * ************************** END JSpinner code ******************************************
             */

            JTextField txt_title = new JTextField(20);
            JTextField txt_reservePrice = new JTextField(20);

            lbl_err.setForeground(Color.RED);

            JTextArea txt_description = new JTextArea(6,20);
            txt_description.setLineWrap(true);

            JList<String> lst_categories = new JList<>(categories);

            JButton btn_submit = new JButton("Submit");

            JScrollPane scrl_description = new JScrollPane(txt_description);
            scrl_description.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            JScrollPane scrl_categories = new JScrollPane(lst_categories);
            scrl_categories.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

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
            c.insets = new Insets(2,20,20,2);
            pnl_sell.add(lbl_title, c);

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 2;
            c.gridy = 1;
            c.gridwidth = 2;
            c.gridheight = 1;
            c.insets = new Insets(2,20,20,2);
            pnl_sell.add(txt_title, c);

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.gridy = 2;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.insets = new Insets(2,20,20,2);
            pnl_sell.add(lbl_description, c);

            c.fill = GridBagConstraints.BOTH;
            c.gridx = 2;
            c.gridy = 2;
            c.weighty = 0.6;
            c.gridwidth = 3;
            c.gridheight = 3;
            c.insets = new Insets(2,20,20,2);
            pnl_sell.add(scrl_description, c);

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.gridy = 7;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.insets = new Insets(2,20,20,2);
            pnl_sell.add(lbl_category, c);

            c.fill = GridBagConstraints.BOTH;
            c.gridx = 2;
            c.gridy = 7;
            c.gridwidth = 3;
            c.gridheight = 3;
            c.insets = new Insets(2,20,20,2);
            pnl_sell.add(scrl_categories, c);

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.gridy = 10;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.insets = new Insets(2,20,20,2);
            pnl_sell.add(lbl_expiryTime, c);

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 3;
            c.gridy = 10;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.insets = new Insets(2,20,20,2);
            pnl_sell.add(spnr_Time, c);

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.gridy = 11;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.insets = new Insets(2,20,20,2);
            pnl_sell.add(lbl_reservePrice, c);

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 2;
            c.gridy = 11;
            c.gridwidth = 2;
            c.gridheight = 1;
            c.insets = new Insets(2,20,20,2);
            //TODO: Implement error handling
            pnl_sell.add(txt_reservePrice, c);

            c.gridx = 3;
            c.gridy = 13;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.insets = new Insets(2,20,20,2);
            pnl_sell.add(btn_submit, c);

            btn_submit.addActionListener((e) -> {
                try {
                    Item newItem = new Item(
                            txt_title.getText(),
                            txt_description.getText(),
                            CATEGORIES.get(lst_categories.getSelectedValue()),
                            client.getCurrentUser(),
                            ((Date) spnr_Time.getModel().getValue()).toInstant().toEpochMilli(),
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

            client.addAuctionDataReceivedListener((data) -> loadUsrAuctions(processAuctionData(data, client.getCurrentUser())));

            pnl_myAuctions.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "My Auctions"
            ));

            pnl_myAuctions.add(tbl_myAuctions, BorderLayout.CENTER);

        }



        private void loadUsrAuctions(Object[][] usr_auctions) {

            String[] columnName = {"ID", "Title", "No. Bidders", "Time Remaining", "Highest Bid", "Cancel"};
            DefaultTableModel model = new DefaultTableModel(usr_auctions, columnName);
            tbl_myAuctions.setModel(model);
            tbl_myAuctions.setRowHeight(64);
            tbl_myAuctions.getColumnModel().getColumn(1).setMinWidth(150);
        }
    }
}
