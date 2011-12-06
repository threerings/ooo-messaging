//
// $Id$

package com.threerings.messaging.amqp;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConnectionParameters;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import com.samskivert.util.Logger;
import com.threerings.messaging.AddressedMessageListener;
import com.threerings.messaging.MessageConnection;
import com.threerings.messaging.MessageSender;

/**
 * Implementation of {@link MessageConnection} to use with AMQP messages.
 */
public class AMQPMessageConnection
    implements MessageConnection
{
    /**
     * Constructs and connects to an AMQP server specified by the given configuration.
     */
    public AMQPMessageConnection (AMQPMessageConfig config)
    {
        _config = config;

        // Create a message sender that uses this connection to create channels.
        _channelFactory = new ChannelFactory() {
            public Channel createChannel ()
                throws IOException
            {
                if (_conn != null) {
                    try {
                        return _conn.createChannel();
                    } catch (ShutdownSignalException sse) {
                        // Fall down
                    }
                }

                // Connection is closed.  Try reconnecting.  If this fails, just error out.
                connect();
                return _conn.createChannel();
            }

        };
        _sender = new AMQPMessageSender(_channelFactory);

        _reconnectService = Executors.newSingleThreadScheduledExecutor();
        // schedule a connection attempt immediately on the reconnect thread
        _reconnectService.schedule(new AttemptReconnect(), 1, TimeUnit.MILLISECONDS);
    }

    public boolean isConnected () {
        Connection conn = _conn;
        return (conn != null && conn.isOpen());
    }

    public void listen (AddressedMessageListener listener)
    {
        AMQPConnectedListener connectedListener;

        // check to see if this listener already exists, if so, we will reconnect
        if (_listeners.containsKey(listener)) {
            connectedListener = _listeners.get(listener);
            if (!connectedListener.isClosed()) {
                logger.warning("Reconnecting listener", "listener", listener);
                try {
                    connectedListener.close();
                } catch (IOException ex) {
                    logger.warning("Could not close old listener connection",
                                   "listener", listener, ex);
                }
            }
        }

        if (isConnected()) {
            logger.info("Connecting listener", "listener", listener);
            connectedListener = new AMQPConnectedListener(
                listener.queueName, listener.address, listener, _channelFactory);
            _listeners.put(listener, connectedListener);
        } else {
            // otherwise wait for reconnect and we'll connect this listener
            logger.info("Deferring listener until we reconnect", "listener", listener);
            _listeners.put(listener, AMQPConnectedListener.NULL);
        }
    }

    public void removeListener (AddressedMessageListener listener)
    {
        if (_listeners.containsKey(listener)) {
            logger.warning("Removing listener", "listener", listener);
            // remove the listener and disconnect it
            AMQPConnectedListener connectedListener = _listeners.remove(listener);
            if (!connectedListener.isClosed()) {
                try {
                    connectedListener.close();
                } catch (IOException ex) {
                    logger.warning("Barfed whiled trying to close a connected RabbitMQ listener",
                                   "listener", listener, "connectedListener", connectedListener, ex);
                }
            }
        } else {
            logger.warning("Tried to remove a deaf RabbitMQ message listener", "listener", listener);
        }
    }

    public synchronized void close ()
        throws IOException
    {
        logger.info("Closing connection to RabbitMQ server.");
        _reconnectService.shutdown();
        _sender.close();

        for (AddressedMessageListener listener : Lists.newArrayList(_listeners.keySet())) {
            removeListener(listener);
        }
        if (_conn != null) {
            _conn.close(CLOSE_TIMEOUT);
        }
    }

    public MessageSender getSender ()
    {
        return _sender;
    }

    /**
     * Connects or re-connects to the AMQP server, re-establishing any listeners.
     *
     * @throws IOException an error occurred while establishing a new connection.
     */
    private synchronized void connect ()
        throws IOException
    {
        if (isConnected()) {
            return;
        }

        ConnectionParameters params = new ConnectionParameters();
        params.setUsername(_config.username);
        params.setPassword(_config.password);
        params.setVirtualHost(_config.virtualHost);
        params.setRequestedHeartbeat(_config.heartBeat);
        ConnectionFactory factory = new ConnectionFactory(params);
        logger.debug("Establishing connection to RabbitMQ server: " + _config);
        _conn = factory.newConnection(_config.hostAddresses);
        _conn.addShutdownListener(new ShutdownListener() {
            public void shutdownCompleted (ShutdownSignalException ex)
            {
                // If the reconnectService has already been shutdown, this is from a normal close.
                if (!_reconnectService.isShutdown()) {
                    logger.warning("RabbitMQ connection closed unexpectedly.  Retrying every 5 " +
                        "seconds.");
                    _reconnectService.schedule(new AttemptReconnect(), 5, TimeUnit.SECONDS);
                } else {
                    logger.info("RabbitMQ connection closed.");
                }
            }
        });
        logger.info("Connection established to RabbitMQ server: " + _config);

        // Reconnect all of the added listeners. Retry if there's an error while reconnecting them.
        for (AddressedMessageListener listener : _listeners.keySet()) {
            _reconnectService.schedule(new ListenerReconnectAttempt(listener), 0, TimeUnit.SECONDS);
        }
    }

    protected class AttemptReconnect implements Runnable
    {
        public void run ()
        {
            try {
                connect();
            } catch (Throwable t) {
                // Could not reconnect, so try again later.
                logger.debug("Could not reconnect to RabbitMQ server.", t);
                _reconnectService.schedule(new AttemptReconnect(), 5, TimeUnit.SECONDS);
            }
        }
    }

    protected class ListenerReconnectAttempt implements Runnable
    {
        public ListenerReconnectAttempt (AddressedMessageListener listener) {
            _listener = listener;
        }

        public void run () {
            try {
                listen(_listener);
                // check to see if it connected successfully
                AMQPConnectedListener conn = _listeners.get(_listener);
                if (conn == AMQPConnectedListener.NULL) {
                    logger.info("Listener connection deferred.", "listener", _listener);
                } else if (conn.isClosed()) {
                    logger.warning("Failed to connect listener. Will retry.", "listener", _listener);
                    _reconnectService.schedule(this, 5, TimeUnit.SECONDS);
                } else {
                    logger.info("Listener connected", "listener", _listener);
                }
            } catch (ShutdownSignalException sse) {
                // no problem, we're shutting down
            } catch (Throwable ex) {
                logger.warning("Something nasty happened while trying to reconnect listener",
                               "listener", _listener, ex);
                _reconnectService.schedule(this, 60, TimeUnit.SECONDS);
            }
        }

        protected AddressedMessageListener _listener;
    }

    protected static final Logger logger = Logger.getLogger(AMQPMessageConnection.class);
    protected static final int CLOSE_TIMEOUT = 2000;

    protected volatile Connection _conn;
    protected final AMQPMessageConfig _config;
    protected final AMQPMessageSender _sender;
    protected final ChannelFactory _channelFactory;
    protected final ScheduledExecutorService _reconnectService;
    protected final Map<AddressedMessageListener, AMQPConnectedListener> _listeners =
        new ConcurrentHashMap<AddressedMessageListener, AMQPConnectedListener>();
}
