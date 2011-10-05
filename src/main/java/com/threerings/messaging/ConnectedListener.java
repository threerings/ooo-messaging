//
// $Id$

package com.threerings.messaging;

import java.io.Closeable;
import java.io.IOException;

/**
 * A listener that accepts messages on queues. This listener will continue processing messages
 * until it is shutdown by calling {@link #close()}.
 *
 * This class is thread-safe.
 */
public interface ConnectedListener
    extends Closeable
{
    /**
     * Closes this listener. This will cause it to stop accepting new messages -- messages
     * currently being processed will continue until complete.
     *
     * @throws IOException An error occurred while trying to close.
     */
    public void close ()
        throws IOException;

    /**
     * Returns true if the listener has already been closed.
     */
    public boolean isClosed ();
}
