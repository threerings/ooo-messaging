//
// $Id$

package com.threerings.messaging;

import java.io.Closeable;
import java.io.IOException;

/**
 * Responsible for sending messages to various destinations.
 *
 * This class is thread-safe.
 */
public interface MessageSender
    extends Closeable
{
    /**
     * Sends a one-shot message to the specified address. This method will return when the message
     * has been sent, but not necessarily after it has been received.
     *
     * @param msg Message to be sent.
     * @param addr Address to send messages to.
     * @throws IOException An error occurred while sending the message.
     */
    void sendMessage (OutMessage msg, DestinationAddress addr)
        throws IOException;

    /**
     * Creates a replying destination, which can send messages and wait for replies to those
     * messages.
     *
     * @param addr Address to create a replying destination for.
     * @return New replying destination.
     * @throws IOException An error occurred while establishing the replying destination.
     */
    ReplyingDestination createReplyingDestination (DestinationAddress addr)
        throws IOException;

}
