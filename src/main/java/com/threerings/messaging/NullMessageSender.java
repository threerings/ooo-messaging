//
// $Id$

package com.threerings.messaging;

import java.io.IOException;

import com.samskivert.util.Logger;

/**
 * An implementation of {@link MessageSender} that does nothing.
 */
public class NullMessageSender
    implements MessageSender
{
    /**
     * Returns a {@link NullReplyingDestination}
     */
    public ReplyingDestination createReplyingDestination (DestinationAddress addr)
        throws IOException
    {
        return new NullReplyingDestination();
    }

    /**
     * A warning will be logged containing the message. The message itself will be dropped.
     */
    public void sendMessage (OutMessage msg, DestinationAddress addr)
        throws IOException
    {
        logger.info("Dropping message.", "msg", msg);
    }

    // from Closeable
    public void close ()
    {
        // nothing to do
    }

    private final static Logger logger = Logger.getLogger(NullMessageSender.class);
}
