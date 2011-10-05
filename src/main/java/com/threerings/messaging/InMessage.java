//
// $Id$

package com.threerings.messaging;

import java.io.IOException;

/**
 * A message received from the queue. If the message is processed, {@link #ack} must be called or
 * it will remain in the queue.
 */
public interface InMessage
{
    /** The contents of the message. */
    byte[] getBody ();

    /** Sends OutMessage back to the queue. */
    void reply (OutMessage message)
        throws IOException;

    /**
     * Acknowledges that the message was processed. This must be called or the message will remain
     * in the queue.
     */
    void ack ()
        throws IOException;
}
