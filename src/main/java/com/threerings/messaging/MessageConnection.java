//
// $Id$

package com.threerings.messaging;

import java.io.Closeable;
import java.io.IOException;

/**
 * Fault-tolerant connection to a message server.  The connection will be created when the object
 * is created and will last until {@link #close()} is called.  While the server is up, messages
 * are sent synchronously (i.e., they complete by the time the send is complete), while processing
 * incoming messages is handled asynchronously in a thread pool.
 *
 * If the server goes down this this connection is open, operations should fast-fail.  However, if
 * the server comes back up, implementations will automatically reconnect as needed.
 */
public interface MessageConnection
    extends Closeable
{
    /**
     * Listens on a queue for incoming messages and uses the given listener to perform some task
     * as messages arrive.  This will continue to listen while the messaging server is up, until
     * {@link ConnectedListener#close()} is called.
     *
     * @param listener Listener that will process messages as they come in.
     */
    void listen (AddressedMessageListener listener);

    /**
     * Removes a listener that is not longer interested in receiving messages.
     *
     * @param listener the listener to remove.
     */
    void removeListener (AddressedMessageListener listener);

    /**
     * Closes the connection and any {@link ReplyingDestination}s and {@link ConnectedListener}s
     * currently associated with the connection. All resources used by the connection should be
     * cleaned up, and the connection itself will become unusable.
     *
     * @throws IOException An error occurred while attempting to close the connection.
     */
    void close ()
        throws IOException;

    /**
     * Gets a {@link MessageSender} to use with this connection.
     */
    MessageSender getSender ();

}
