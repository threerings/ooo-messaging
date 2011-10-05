//
// $Id$

package com.threerings.messaging;

import java.io.IOException;

/**
 * Listens for messages and processes them.
 *
 * Implementations are expected to be stateless since this will be used on multiple threads.
 */
public interface MessageListener
{
    /**
     * Called when a message has been received. If the message is processed successfully,
     * {@link InMessage#ack} must be called to remove the message from the queue. If the listener
     * always processes messages synchronously in this method, {@link AckingMessageListener} may
     * be used to take care of acking.
     */
    void received (InMessage message)
        throws IOException;
}
