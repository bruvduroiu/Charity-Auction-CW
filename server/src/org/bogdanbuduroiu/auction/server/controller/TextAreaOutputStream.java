package org.bogdanbuduroiu.auction.server.controller;

import javax.swing.*;
import java.io.IOException;
import java.io.OutputStream;
/**
 * The code for redirecting the output of System.out to
 * my JText area was taken from:
 * http://stackoverflow.com/questions/14706674/system-out-println-to-jtextarea
 *
 * Credits: Mikhail Vladimirov (http://stackoverflow.com/users/2038768/mikhail-vladimirov)
 */

public class TextAreaOutputStream extends OutputStream {
    private final JTextArea destination;

    public TextAreaOutputStream(JTextArea destination) {
        if (destination == null)
            throw new IllegalArgumentException("Destination is null");

        this.destination = destination;
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        final String text = new String(buffer, offset, length);
        SwingUtilities.invokeLater(() -> destination.append(text));
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte) b}, 0, 1);
    }
}
