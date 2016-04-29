package org.bogdanbuduroiu.auction.client.view;

import org.bogdanbuduroiu.auction.model.Category;
import org.bogdanbuduroiu.auction.model.Item;
import org.bogdanbuduroiu.auction.model.User;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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
    private User currentUser;
    private JTextField txt_searchField;
    private JComboBox<String> cmb_searchCategory;
    private JButton btn_submitSearch;
    private JLabel lbl_user;
    private JLabel lbl_categories;
    private JList<String> lst_categories;
    private JTable tbl_auctions;
    private JScrollPane scrl_auctions;

    private final HashMap<String, Category> CATEGORIES = new HashMap<String, Category>() {{
        put("Audio & Video", Category.AUDIO_VIDEO);
        put("Auto", Category.AUTO);
        put("Home & Garden", Category.HOME_GARDEN);
        put("Health & Nutrition", Category.HEALTH_NUTRITION);
        put("Jewelry", Category.JEWELRY);
        put("Sports & Outdoor", Category.SPORTS_OUTDOOR);
        put("Watches", Category.WATCHES);
    }};
    private MainAuctionScreen(User user) {
        //TODO: Sort the Categories List
        this.currentUser = user;
        String[] categories = CATEGORIES.keySet().toArray(new String[CATEGORIES.size()]);
        txt_searchField = new JTextField("Search", 20);
        cmb_searchCategory = new JComboBox<>(categories);
        btn_submitSearch = new JButton("Search");
        lbl_user = new JLabel(user.getUsername());
        lbl_categories = new JLabel("Categories");
        lst_categories = new JList<>(categories);

        tbl_auctions = loadAuctions();
        scrl_auctions = new JScrollPane(tbl_auctions);
        scrl_auctions.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);


        init();
    }

    public static MainAuctionScreen initializeScreen(User user) {
        return new MainAuctionScreen(user);
    }

    private JTable loadAuctions() {
        try {
            //TODO: Implement loading items from method
            Item test = new Item(12312, "Test", "Test descript", Category.AUTO, 1231235, System.currentTimeMillis() + 100000, 22.56, ImageIO.read(new File("/Users/bogdanbuduroiu/Downloads/schneider.png")));
            DateFormat dateFormat = new SimpleDateFormat("hh:mm:ss");
            String[] columnName = {"Title", "Description", "No. Bidders", "Seller", "Time Remaining", "Price"};
            Object[][] data = {
                    {test.getTitle(), test.getDescription(), test.getBids().size(), test.getVendorID(), dateFormat.format(test.getTimeRemaining()), test.getReservePrice()},
                    {test.getTitle(), test.getDescription(), test.getBids().size(), test.getVendorID(), dateFormat.format(test.getTimeRemaining()), test.getReservePrice()},
                    {test.getTitle(), test.getDescription(), test.getBids().size(), test.getVendorID(), dateFormat.format(test.getTimeRemaining()), test.getReservePrice()},
                    {test.getTitle(), test.getDescription(), test.getBids().size(), test.getVendorID(), dateFormat.format(test.getTimeRemaining()), test.getReservePrice()},
                    {test.getTitle(), test.getDescription(), test.getBids().size(), test.getVendorID(), dateFormat.format(test.getTimeRemaining()), test.getReservePrice()},
                    {test.getTitle(), test.getDescription(), test.getBids().size(), test.getVendorID(), dateFormat.format(test.getTimeRemaining()), test.getReservePrice()},
                    {test.getTitle(), test.getDescription(), test.getBids().size(), test.getVendorID(), dateFormat.format(test.getTimeRemaining()), test.getReservePrice()},
                    {test.getTitle(), test.getDescription(), test.getBids().size(), test.getVendorID(), dateFormat.format(test.getTimeRemaining()), test.getReservePrice()},
                    {test.getTitle(), test.getDescription(), test.getBids().size(), test.getVendorID(), dateFormat.format(test.getTimeRemaining()), test.getReservePrice()},
                    {test.getTitle(), test.getDescription(), test.getBids().size(), test.getVendorID(), dateFormat.format(test.getTimeRemaining()), test.getReservePrice()},
                    {test.getTitle(), test.getDescription(), test.getBids().size(), test.getVendorID(), dateFormat.format(test.getTimeRemaining()), test.getReservePrice()},
                    {test.getTitle(), test.getDescription(), test.getBids().size(), test.getVendorID(), dateFormat.format(test.getTimeRemaining()), test.getReservePrice()},
                    {test.getTitle(), test.getDescription(), test.getBids().size(), test.getVendorID(), dateFormat.format(test.getTimeRemaining()), test.getReservePrice()},
                    {test.getTitle(), test.getDescription(), test.getBids().size(), test.getVendorID(), dateFormat.format(test.getTimeRemaining()), test.getReservePrice()},
                    {test.getTitle(), test.getDescription(), test.getBids().size(), test.getVendorID(), dateFormat.format(test.getTimeRemaining()), test.getReservePrice()}
            };
            JTable table = new JTable(data, columnName);
            table.setRowHeight(64);
            table.getColumnModel().getColumn(1).setMinWidth(300);
            return table;
        }
        catch (IOException e) { e.printStackTrace(); }
        return new JTable();
    }

    private void init() {
        Container container = getContentPane();
        container.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0.1;
        c.weightx = 1;
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 2;
        c.gridheight = 2;
        container.add(txt_searchField,c);
        c.gridx = 3;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 2;
        container.add(cmb_searchCategory, c);
        c.gridx = 4;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 2;
        container.add(btn_submitSearch, c);
        c.gridx = 5;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        container.add(lbl_user, c);
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 0.9;
        c.weightx = 0.3;
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        container.add(lst_categories, c);
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 0.9;
        c.weightx = 0.7;
        c.gridx = 2;
        c.gridy = 2;
        c.gridwidth = 5;
        container.add(scrl_auctions, c);



        setSize(WIDTH, HEIGHT);
        setTitle(TITLE);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    class AuctionListDisplay extends JPanel{

        //TODO: Do I still need this?
        private JLabel lbl_thumbnail;
        private JLabel lbl_title;
        private JLabel lbl_noBidders;
        private JLabel lbl_timeRemaining;
        private JLabel lbl_seller;
        private JLabel lbl_currentPrice;
        private static final int HEIGHT = 100;

        public AuctionListDisplay(Item auction) {
            lbl_title = new JLabel();
            lbl_noBidders = new JLabel();
            lbl_timeRemaining = new JLabel();
            lbl_seller = new JLabel();
            lbl_currentPrice = new JLabel();

            lbl_thumbnail = new JLabel(new ImageIcon(auction.getItemImage()));
            lbl_title.setText(auction.getTitle());
            lbl_noBidders.setText(Integer.toString(auction.getBids().size()));

            Date timeRemaining = new Date((auction.getExpiryTime() - auction.getStartTime() * 1000));
            lbl_timeRemaining.setText("Time: " + timeRemaining.toString());
            lbl_seller.setText(Integer.toString(auction.getVendorID()));

            Double price = (auction.getBids().size() == 0) ? auction.getReservePrice() : auction.getBids().poll().getBidAmmount();
            lbl_currentPrice.setText("Bid: " + price.toString());

            init();
        }

        private void init() {
            GridLayout gridLayout = new GridLayout(1,6);
            setLayout(gridLayout);
            add(lbl_thumbnail);
            add(lbl_title);
            add(lbl_noBidders);
            add(lbl_seller);
            add(lbl_timeRemaining);
            add(lbl_currentPrice);
            pack();

            setSize(300, HEIGHT);
        }

        class AuctionDisplayTable extends JTable {
            //TODO: Implement adding Images in Auction Table
            public void init() {
                DefaultTableModel model;
                model = new DefaultTableModel() {
                    @Override
                    public Class<?> getColumnClass(int index) {
                        return BufferedImage.class;
                    }
                };
            }

            class BufferedImageCellRenderer extends DefaultTableCellRenderer {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                                                               boolean isSelected, boolean hasFocus,
                                                               int row, int column) {

                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                            row, column);
                    if (value instanceof BufferedImage) {
                        setIcon(new ImageIcon((BufferedImage)value));
                        setText(null);
                    } else {
                        setIcon(new ImageIcon());
                        setText("No Image");
                    }
                    return this;
                }
            }
        }
    }
}
