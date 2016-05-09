package org.bogdanbuduroiu.auction.client.view;

import javax.swing.*;
import java.awt.*;
import java.net.InetSocketAddress;

/**
 * Created by bogdanbuduroiu on 09.05.16.
 */
public class AddServerScreen extends JFrame {

    ClientLoginPanel clientLoginPanel;

    JLabel lbl_title;
    JLabel lbl_host;
    JLabel lbl_port;

    JTextField txt_host;
    JTextField txt_port;

    JButton btn_submit;

    public AddServerScreen(ClientLoginPanel clp) throws HeadlessException {
        this.clientLoginPanel = clp;

        init();
    }

    private void init() {

        lbl_title = new JLabel("New Server");
        lbl_title.setFont(new Font("Arial", Font.BOLD, 16));

        lbl_host = new JLabel("Host: ");
        lbl_port = new JLabel("Port: ");

        txt_host = new JTextField(10);
        txt_port = new JTextField(10);

        btn_submit = new JButton("Submit");
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.5;
        add(lbl_title, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(10,10,15,10);
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.3;
        add(lbl_host, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10,10,15,10);
        c.gridx = 2;
        c.gridy = 2;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 0.6;
        add(txt_host, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10,10,15,10);
        c.gridx = 1;
        c.gridy = 3;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.3;
        add(lbl_port, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10,10,15,10);
        c.gridx = 2;
        c.gridy = 3;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 0.6;
        add(txt_port, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10,10,15,10);
        c.gridx = 2;
        c.gridy = 4;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.3;
        add(btn_submit, c);

        setSize(300,400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);

        btn_submit.addActionListener(
                e -> {
                    clientLoginPanel.addServer(txt_host.getText(),
                            new InetSocketAddress(txt_host.getText(), Integer.parseInt(txt_port.getText())));

                    this.dispose();
                });
    }
}
