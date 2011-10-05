//
// $Id$

package com.threerings.messaging;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.samskivert.util.Logger;

/**
 * Implementation of {@link MessageConnection} that delegates to another message connection after
 * it has been initialized by calling {@link #init(MessageConnection)}.  Until that time, most
 * operations will succeed with warnings printed to the logs.  This is useful if a connection
 * should only start up at a particular time, or if it's possible to start up without any
 * connection to the messaging server.
 *
 * This class is thread-safe.
 */
public class DelayedMessageConnection
    implements MessageConnection
{
    /**
     * Initializes this message connection with the given delegate.  This method must only be
     * called at most once (i.e., it is not idempotent).  Until this method is called, operations
     * using this connection will do nothing, though some log messages will be output.
     *
     * @param delegate The actual message connection to use.
     * @throws IllegalStateException init has already been called.
     */
    public void init (MessageConnection delegate)
    {
        Preconditions.checkState(_delegate.compareAndSet(null, delegate),
            "The DelayedMessageConnection was already initialized.");
        // add any listeners that were early to this party
        for (AddressedMessageListener listener : _impatientListeners) {
            _delegate.get().listen(listener);
        }
    }

    /**
     * Logs a message if not yet initialized.
     */
    public void close ()
        throws IOException
    {
        MessageConnection delegate = _delegate.get();
        if (delegate == null) {
            logger.debug("Closing message connection that was never initialized.");
        } else {
            delegate.close();
        }
    }

    /**
     * Returns a {@link NullMessageSender} if not yet initialized.
     */
    public MessageSender getSender ()
    {
        MessageConnection delegate = _delegate.get();
        if (delegate == null) {
            return new NullMessageSender();
        }
        return delegate.getSender();
    }

    /**
     * Logs a warning and returns {@link NullConnectedListener} if not yet initialized.
     */
    public void listen (AddressedMessageListener listener)
    {
        MessageConnection delegate = _delegate.get();
        if (delegate == null) {
            logger.info("Message connection not yet initialized.", "listener", listener);
            _impatientListeners.add(listener);
        } else {
            delegate.listen(listener);
        }
    }

    public void removeListener (AddressedMessageListener listener)
    {
        MessageConnection delegate = _delegate.get();
        if (delegate != null) {
            delegate.removeListener(listener);
        }
    }

    /**
     * Any listeners that got added before the connection is initialized.
     */
    protected final List<AddressedMessageListener> _impatientListeners = Lists.newArrayList();

    private final static Logger logger = Logger.getLogger(DelayedMessageConnection.class);

    protected final AtomicReference<MessageConnection> _delegate =
        new AtomicReference<MessageConnection>(null);

}
