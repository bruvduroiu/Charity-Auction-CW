package org.bogdanbuduroiu.auction.client.view;

import org.bogdanbuduroiu.auction.client.controller.Client;
import org.bogdanbuduroiu.auction.model.Item;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by bogdanbuduroiu on 02.05.16.
 */
public class AuctionBidScreen extends JFrame {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("hh:mm:ss");

    private static Client client;

    private JLabel lbl_title;
    private JLabel lbl_description;
    private JLabel lbl_seller;
    private JLabel lbl_noBidders;
    private JLabel lbl_reservePrice;
    private JLabel lbl_highestBid;
    private JLabel lbl_timeRemaining;

    private JTextField txt_bid;

    private JButton btn_bid;

    public AuctionBidScreen(Client client, Item item) throws HeadlessException {
        super(item.getTitle());

        this.client = client;

        lbl_title = new JLabel(item.getTitle());
        lbl_description = new JLabel(item.getDescription());
        lbl_seller = new JLabel("Seller: " + item.getVendorID());
        lbl_noBidders = new JLabel("Bidders: " + item.getBids().size());
        lbl_reservePrice = new JLabel("Reserve: " + item.getReservePrice());
        lbl_highestBid = new JLabel("High Bid: " +
                ((item.getBids().isEmpty())
                        ? "N/A"
                        : item.getBids().peek().getBidAmmount()));

        lbl_timeRemaining = new JLabel("Remaining: " + DATE_FORMAT.format(item.getTimeRemaining()));

        txt_bid = new JTextField(7);
        txt_bid.setText(
                ((item.getBids().isEmpty())
                        ? Double.toString(item.getReservePrice() + 10)
                        : Double.toString(item.getBids().peek().getBidAmmount() + 10)));
        txt_bid.setHorizontalAlignment(SwingConstants.RIGHT);

        btn_bid = new JButton("Bid");
        btn_bid.addActionListener((event) -> {
            //TODO: Implement bid action
        });

        init();
    }

    private void init() {

        Container container = getContentPane();
        container.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.PAGE_START;
        c.insets = new Insets(20,20,20,20);

        c.gridx = 2;
        c.gridy = 1;
        c.gridwidth = 2;
        c.gridheight = 1;
        lbl_title.setFont(new Font(
                lbl_title.getFont().getName(),
                Font.BOLD + Font.ITALIC,
                18
        ));
        container.add(lbl_title, c);

        c.gridx = 2;
        c.gridy = 2;
        c.gridwidth = 2;
        c.gridheight = 1;
        container.add(lbl_description, c);

        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 2;
        c.gridheight = 1;
        container.add(lbl_seller, c);

        c.gridx = 3;
        c.gridy = 3;
        c.gridwidth = 1;
        c.gridheight = 1;
        container.add(lbl_noBidders, c);

        c.gridx = 1;
        c.gridy = 4;
        c.gridwidth = 2;
        c.gridheight = 1;
        container.add(lbl_reservePrice, c);

        c.gridx = 3;
        c.gridy = 4;
        c.gridwidth = 1;
        c.gridheight = 1;
        container.add(lbl_highestBid, c);

        c.gridx = 1;
        c.gridy = 5;
        c.gridwidth = 2;
        c.gridheight = 1;
        container.add(txt_bid, c);

        c.gridx = 3;
        c.gridy = 5;
        c.gridwidth = 1;
        c.gridheight = 1;
        container.add(btn_bid, c);

        setSize(400,600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);
    }
}
