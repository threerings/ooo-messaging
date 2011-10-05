//
// $Id$

package com.threerings.messaging;

import java.io.IOException;

/**
 * Acknowledges the receipt of messages immediately as long as processing doesn't throw an
 * exception.
 */
public abstract class AckingMessageListener
    implements MessageListener
{

    /**
     * Calls {@link #processReceived} and acks afterwards as long as it doesn't throw an exception.
     */
    public final void received (InMessage message)
        throws IOException
    {
        processReceived(message);
        message.ack();
    }

    /**
     * Should be implemented in subclasses to process the message synchronously. If the message
     * should be left in the queue, an exception should be thrown.
     */
    public abstract void processReceived(InMessage message) throws IOException;
}
