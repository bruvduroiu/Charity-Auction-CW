package org.bogdanbuduroiu.auction.server.view;

import org.bogdanbuduroiu.auction.model.Item;
import org.bogdanbuduroiu.auction.model.User;
import org.bogdanbuduroiu.auction.server.controller.Server;
import org.bogdanbuduroiu.auction.server.controller.TextAreaOutputStream;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by bogdanbuduroiu on 09.05.16.
 */
public class ServerGUI extends JFrame {

    private Server server;

    private JTextArea txt_console;

    private JButton btn_report;

    private JScrollPane scrl_report;
    private JTable tbl_report;
    private TextAreaOutputStream out_console;

    public ServerGUI(Server server) throws HeadlessException {
        this.server = server;

    }

    public void init() {

        txt_console = new JTextArea();
        txt_console.setBackground(Color.BLACK);
        txt_console.setForeground(Color.WHITE);

        tbl_report = new JTable();
        scrl_report = new JScrollPane(tbl_report);
        scrl_report.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrl_report.setPreferredSize(new Dimension(400,800));

        btn_report = new JButton("Generate Report");

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        out_console = new TextAreaOutputStream(txt_console);

        System.setOut(new PrintStream(out_console));

        c.insets = new Insets(0,5,0,5);
        c.gridx = 2;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weighty = 0.1;
        add(btn_report, c);

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 2;
        c.gridheight = 2;
        c.weightx = 0.3;
        c.weighty = 0.9;
        add(scrl_report, c);

        c.fill = GridBagConstraints.BOTH;
        c.gridx = 3;
        c.gridy = 2;
        c.gridwidth = 2;
        c.gridheight = 2;
        c.weightx = 0.7;
        c.weighty = 0.9;
        add(txt_console, c);

        btn_report.addActionListener(e -> loadTableData());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800,600);
        setVisible(true);
    }

    public TextAreaOutputStream getOutConsole() {
        return out_console;
    }

    private void loadTableData() {
        HashMap<User, Set<Item>> data = server.getWonAuctions();

        String[] columnName = new String[] {"Username", "ItemID", "Item"};
        int totalSize = 0;
        for (Set<Item> set : data.values())
            totalSize+=set.size();

        int i = 0;
        Object[][] formatted_data = new Object[totalSize][];
        for (User entry : data.keySet())
            for (Item item : data.get(entry))
                formatted_data[i++] = new Object[] {
                        entry.getUsername(),
                        item.getTitle(),
                        item.getBids().peek().getBidAmmount()
                };

        DefaultTableModel model = new DefaultTableModel(formatted_data, columnName);
        tbl_report.setModel(model);
    }

    public JTextArea getTxt_console() {
        return txt_console;
    }
}
