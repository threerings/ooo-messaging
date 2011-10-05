//
// $Id$

package com.threerings.messaging;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.samskivert.util.Logger;

/**
 * An implementation of {@link ReplyingDestination} that does nothing. This will not listen for
 * replies, and attempts to send messages will be met with an exception.
 */
public class NullReplyingDestination
    implements ReplyingDestination
{
    public void close ()
        throws IOException
    {
        logger.debug("Closing null replying destination.");
        _closed = true;
    }

    public boolean isClosed ()
    {
        return _closed;
    }

    /**
     * This will always throw an IOException, indicating the message cannot be sent.
     */
    public byte[] sendMessage (OutMessage msg, long timeout)
        throws IOException, TimeoutException
    {
        // The caller is expecting an actual return value, which we can't provide a valid one
        // for. So throw an exception.
        throw new IOException("Cannot send message from a null replying destination.");
    }

    protected boolean _closed;

    protected final static Logger logger = Logger.getLogger(NullReplyingDestination.class);
}
