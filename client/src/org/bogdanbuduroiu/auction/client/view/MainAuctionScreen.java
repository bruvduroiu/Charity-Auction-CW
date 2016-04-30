package org.bogdanbuduroiu.auction.client.view;

import org.bogdanbuduroiu.auction.client.controller.Client;
import org.bogdanbuduroiu.auction.model.Category;
import org.bogdanbuduroiu.auction.model.Item;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

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
            auctionData = data;
            pnl_auctions.loadAuctions();
        });
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

            lbl_user = new JLabel(client.getCurrentUsername());
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

        private JLabel lbl_title;
        private JLabel lbl_description;
        private JLabel lbl_category;
        private JLabel lbl_vendor;
        private JLabel lbl_expiryTime;
        private JLabel lbl_reservePrice;
        private JLabel lbl_itemImage;

        private JTextField txt_title;
        private JTextField txt_vendor;
        private JTextField txt_reservePrice;

        private JTextArea txt_description;

        private JList<String> lst_categories;

        private JButton btn_submit;

        public SellPanel() {
            lbl_title = new JLabel("Title");
            lbl_description = new JLabel("Description");
            lbl_category = new JLabel("Category");
            lbl_vendor = new JLabel("Vendor");
            lbl_expiryTime = new JLabel("Close date");
            lbl_reservePrice = new JLabel("Reserve Price");
            lbl_itemImage = new JLabel("Image");

            txt_title = new JTextField(20);
            txt_description = new JTextArea(6,20);
            txt_vendor = new JTextField(20);
            txt_reservePrice = new JTextField(20);

            lst_categories = new JList<>();

            btn_submit = new JButton("Submit");

            init();
        }

        private void init() {

            setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 1;
            c.gridwidth = 1;
            c.gridheight = 1;
            add(lbl_title, c);

            c.gridx = 1;
            c.gridy = 2;
            c.gridwidth = 2;
            c.gridheight = 1;
            add(txt_title, c);

            c.gridx = 1;
            c.gridy = 2;
            c.gridwidth = 1;
            c.gridheight = 1;
            add(lbl_description, c);

            c.gridx = 1;
            c.gridy = 3;
            c.gridwidth = 3;
            c.gridheight = 3;
            add(txt_description, c);

            c.gridx = 1;
            c.gridy = 7;
            c.gridwidth = 1;
            c.gridheight = 1;
            add(lbl_category, c);

            c.gridx = 2;
            c.gridy = 7;
            c.gridwidth = 1;
            c.gridheight = 1;
            add(lst_categories, c);
        }
    }
}
