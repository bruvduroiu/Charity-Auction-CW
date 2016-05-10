package org.bogdanbuduroiu.auction.client.view;

import org.bogdanbuduroiu.auction.model.User;

import javax.swing.*;
import java.awt.*;

/**
 * Created by bogdanbuduroiu on 29.04.16.
 */
class ClientRegistrationPanel extends JPanel {
    private ClientLoginScreen clientLoginScreen;
    private JLabel lbl_fName;
    private JLabel lbl_lName;
    private JLabel lbl_username;
    private JLabel lbl_passwd;
    private JLabel lbl_cnfrmPasswd;
    private JLabel lbl_err_msg;
    private JTextField txt_fName;
    private JTextField txt_lName;
    private JTextField txt_username;
    private JPasswordField txt_passwd;
    private JPasswordField txt_cnfrmPasswd;
    private JButton btn_submit;
    private JButton btn_back;

    public ClientRegistrationPanel(ClientLoginScreen cls) {
        this.clientLoginScreen = cls;
        init();
    }

    private void init() {
        lbl_fName = new JLabel("Name:");
        lbl_lName = new JLabel("Surname:");
        lbl_username = new JLabel("Username:");
        lbl_passwd = new JLabel("Password:");
        lbl_cnfrmPasswd = new JLabel("Confirm Password:");
        lbl_err_msg = new JLabel();
        lbl_err_msg.setForeground(Color.RED);
        txt_fName = new JTextField(20);
        txt_lName = new JTextField(20);
        txt_username = new JTextField(20);
        txt_passwd = new JPasswordField(20);
        txt_cnfrmPasswd = new JPasswordField(20);
        btn_submit = new JButton("Submit");
        btn_back = new JButton("Back");

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = 1;
        c.weightx = 0.3;
        c.gridx = 2;
        c.gridy = 1;
        add(lbl_fName, c);
        c.gridwidth = 2;
        c.weightx = 0.6;
        c.gridx = 3;
        c.gridy = 1;
        add(txt_fName, c);
        c.gridwidth = 1;
        c.weightx = 0.3;
        c.gridx = 2;
        c.gridy = 2;
        add(lbl_lName, c);
        c.gridwidth = 2;
        c.weightx = 0.6;
        c.gridx = 3;
        c.gridy = 2;
        add(txt_lName, c);
        c.gridwidth = 1;
        c.weightx = 0.3;
        c.gridx = 2;
        c.gridy = 3;
        add(lbl_username, c);
        c.gridwidth = 2;
        c.weightx = 0.6;
        c.gridx = 3;
        c.gridy = 3;
        add(txt_username, c);
        c.gridwidth = 1;
        c.weightx = 0.3;
        c.gridx = 2;
        c.gridy = 4;
        add(lbl_passwd, c);
        c.gridwidth = 2;
        c.weightx = 0.6;
        c.gridx = 3;
        c.gridy = 4;
        add(txt_passwd, c);
        c.gridwidth = 1;
        c.weightx = 0.3;
        c.gridx = 2;
        c.gridy = 5;
        add(lbl_cnfrmPasswd, c);
        c.gridwidth = 2;
        c.weightx = 0.6;
        c.gridx = 3;
        c.gridy = 5;
        add(txt_cnfrmPasswd, c);
        c.anchor = GridBagConstraints.FIRST_LINE_END;
        c.gridwidth = 1;
        c.weightx = 0.2;
        c.gridx = 3;
        c.gridy = 6;
        add(btn_submit, c);
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.gridwidth = 1;
        c.weightx = 0.2;
        c.gridx = 4;
        c.gridy = 6;
        add(btn_back, c);
        c.gridx = 2;
        c.gridy = 8;
        c.gridwidth = 4;
        c.weightx = 1;
        add(lbl_err_msg, c);

        btn_submit.addActionListener((e) -> {
            User tmpUser = new User(txt_fName.getText(), txt_lName.getText(), txt_username.getText());
            String passString = new String(txt_passwd.getPassword());
            String cnfrmPassString = new String(txt_cnfrmPasswd.getPassword());
            if (!passString.equals(cnfrmPassString)) {
                lbl_err_msg.setText("Passwords do not match.");
                return;
            }
            char[] tmpPass = txt_passwd.getPassword();

            clientLoginScreen.client.newRegistration(tmpUser, tmpPass);
        });

        btn_back.addActionListener(event -> clientLoginScreen.cardLayout.show(clientLoginScreen.cards, ClientLoginScreen.LOGIN_CARD));
    }
}
