package org.bogdanbuduroiu.auction.client.utils;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * Created by bogdanbuduroiu on 11.05.16.
 */

/**
 * Limits the number of characters enterred into a JTextField.
 *
 * Implemented from: http://stackoverflow.com/questions/3519151/how-to-limit-the-number-of-characters-in-jtextfield
 */
public class JTextFieldLimit extends PlainDocument {
    private int limit;

    public JTextFieldLimit(int limit) {
        super();
        this.limit = limit;
    }

    public void insertString( int offset, String  str, AttributeSet attr ) throws BadLocationException {
        if (str == null) return;

        if ((getLength() + str.length()) <= limit) {
            super.insertString(offset, str, attr);
        }
    }
}
