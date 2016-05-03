package org.bogdanbuduroiu.auction.client.view;

import org.bogdanbuduroiu.auction.model.User;

import javax.swing.*;
import java.awt.*;

/**
 * Created by bogdanbuduroiu on 29.04.16.
 */
class ClientLoginPanel extends JPanel {

    private ClientLoginScreen clientLoginScreen;
    private JLabel lbl_username;
    private JLabel lbl_password;
    private JLabel lbl_register;
    private JLabel lbl_err;
    private JTextField txt_username;
    private JPasswordField txt_password;
    private JButton btn_submit;
    private JButton btn_register;

    public ClientLoginPanel(ClientLoginScreen cls) {
        this.clientLoginScreen = cls;
        init();
    }

    private void init() {
        lbl_username = new JLabel("Username:");
        lbl_password = new JLabel("Password:");
        lbl_register = new JLabel("New to this? Click here:");
        lbl_err = new JLabel();
        lbl_err.setForeground(Color.RED);
        txt_username = new JTextField(20);
        txt_password = new JPasswordField(20);
        btn_submit = new JButton("Submit");
        btn_register = new JButton("Register");

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = 3;
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1;
        add(lbl_err, c);
        c.gridwidth = 1;
        c.weightx = 0.3;
        c.gridx = 2;
        c.gridy = 2;
        add(lbl_username, c);
        c.gridwidth = 2;
        c.weightx = 0.6;
        c.gridx = 3;
        c.gridy = 2;
        add(txt_username, c);
        c.gridwidth = 1;
        c.weightx = 0.3;
        c.gridx = 2;
        c.gridy = 3;
        add(lbl_password, c);
        c.gridwidth = 2;
        c.weightx = 0.6;
        c.gridx = 3;
        c.gridy = 3;
        add(txt_password, c);
        c.gridwidth = 1;
        c.weightx = 0.2;
        c.gridx = 3;
        c.gridy = 4;
        add(btn_submit, c);
        c.gridwidth = 3;
        c.gridheight = 2;
        c.weightx = 1;
        c.gridx = 1;
        c.gridy = 7;
        add(lbl_register, c);
        c.gridwidth = 1;
        c.weightx = 1;
        c.gridx = 4;
        c.gridy = 7;
        add(btn_register, c);


        btn_submit.addActionListener((e) -> {
            User tmpUser = new User(txt_username.getText());
            clientLoginScreen.client.setCurrentUser(tmpUser);
            clientLoginScreen.client.validateDetails(tmpUser, txt_password.getPassword());
        });



        btn_register.addActionListener((event) -> {
            clientLoginScreen.cardLayout.show(clientLoginScreen.cards, ClientLoginScreen.REGISTRATION_CARD);
        });
    }

    public void setErr(String errMsg) {
        lbl_err.setText(errMsg);
    }
}
