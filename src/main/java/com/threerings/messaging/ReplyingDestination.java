//
// $Id$

package com.threerings.messaging;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * A destination that will reply to messages sent to it. This effects a request/response mechanism
 * using message queues. The destination will be connected when the object is created and last
 * until {@link #close()} is called.
 *
 * This class is thread-safe.
 */
public interface ReplyingDestination
    extends Closeable
{
    /**
     * Sends a message and synchronously blocks until a response has been received.
     *
     * @param msg Message to send to this destination.
     * @param timeout Maximum time to wait for a reply until a TimeoutException is thrown, in
     * milliseconds. A good value for this might be in the range of 1000-5000 ms.
     * @return The response received.
     * @throws IOException An error occurred while sending the message or receiving its reply.
     * @throws TimeoutException Waiting for a reply timed out.
     */
    byte[] sendMessage (OutMessage msg, long timeout)
        throws IOException, TimeoutException;

    /**
     * Closes this destination and all resources it depends on.
     *
     * @throws IOException An error occurred while closing the destination.
     */
    void close ()
        throws IOException;

    /**
     * Determines whether or not the destination has been closed.
     */
    boolean isClosed ();
}
