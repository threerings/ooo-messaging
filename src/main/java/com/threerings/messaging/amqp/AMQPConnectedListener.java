//
// $Id$

package com.threerings.messaging.amqp;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.IOException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

import com.samskivert.util.Logger;

import com.threerings.messaging.ConnectedListener;
import com.threerings.messaging.DestinationAddress;
import com.threerings.messaging.InMessage;
import com.threerings.messaging.MessageListener;
import com.threerings.messaging.OutMessage;

/**
 * Implementation of {@link ConnectedListener} for AMQP messages.
 *
 * This class is thread-safe.
 */
public class AMQPConnectedListener
    implements ConnectedListener
{
    /** A null-ish sentinel, used by {@link AMQPMessageConnection}. */
    public static final AMQPConnectedListener NULL = new AMQPConnectedListener();

    /**
     * Creates a new connected listener and begins listening on the queue for messages.
     *
     * @param queueName Name of the queue to retrieve messages from.
     * @param addr Address the queue can be reached.
     * @param listener Listener to be called whenever a new message arrives.
     * @param channelFactory Factory for creating channels on a connection.
     */
    public AMQPConnectedListener (String queueName, DestinationAddress addr,
            MessageListener listener, ChannelFactory channelFactory)
    {
        _queueName = queueName;
        _addr = addr;
        _listener = listener;
        _channelFactory = channelFactory;
        _shutdown = false;

        // Attempt to connect.  If we cannot connect on construction, leave it to the connection
        // to attempt to reconnect automatically.
        try {
            connect();
        } catch (IOException ioe) {
            logger.warning("Could not listen on queue.", "queueName", queueName,
                "address", addr.toString(), ioe);
        }
    }

    public synchronized void close ()
        throws IOException
    {
        _shutdown = true;

        if (_service != null) {
            _service.shutdown();
        }

        if (_channel != null && !isClosed() && _channel.isOpen()) {
            try {
                _channel.basicCancel(_consumerTag);
            } finally {
                // Try closing the channel even if the above failed.
                _channel.close(AMQP.REPLY_SUCCESS, "Consumer closed.");
            }
        }
    }

    public synchronized boolean isClosed()
    {
        // If it was explicitly shutdown, or if it never started.
        return _shutdown || _consumerTag == null;
    }

    /**
     * Begin consuming messages from the queue.  This method assumes the current channel has already
     * been closed, either because it has never connected or because the connection had previously
     * been terminated.
     *
     * @throws IOException An error occurred while attempting to connect.
     */
    public synchronized void connect ()
        throws IOException
    {
        // make sure that the existing channel and service are shutdown in the case we are
        // reconnecting
        close();
        _shutdown = false;

        _channel = _channelFactory.createChannel();
        _channel.exchangeDeclare(_addr.exchange, "direct", true);
        _channel.queueDeclare(_queueName, true);
        _channel.queueBind(_queueName, _addr.exchange, _addr.getRoutingKey());
        final QueueingConsumer consumer = new QueueingConsumer(_channel) {
            @Override
            public void handleCancelOk (String consumerTag) {
                super.handleCancelOk(consumerTag);
                logger.info("Canceled consumer for queue: " + _queueName);
            }

            @Override
            public void handleConsumeOk(String consumerTag) {
                super.handleConsumeOk(consumerTag);
                _consumerTag = consumerTag;
                logger.info("Consume OK", "queue", _queueName, "consumerTag", consumerTag);
            }

            @Override
            public void handleShutdownSignal (String consumerTag,
                ShutdownSignalException ex)
            {
                super.handleShutdownSignal(consumerTag, ex);
                logger.info("Disconnected from queue: " + _queueName);
            }

        };
        _consumerTag = _channel.basicConsume(_queueName, false, consumer);

        _service = Executors.newSingleThreadExecutor();
        _service.execute(new Runnable() {
            public void run () {
                while(!_shutdown) {
                    QueueingConsumer.Delivery delivery = null;
                    try {
                        delivery = consumer.nextDelivery();
                        logger.info("Message received from RabbitMQ", "queue", _queueName);

                        // forward the received message to the listener for processing
                        _listener.received(new AMQPInMessage(delivery.getBody(),
                            delivery.getProperties(), delivery.getEnvelope().getDeliveryTag()));
                    } catch (InterruptedException iex) {
                        if (!_shutdown) {
                            logger.warning("Interrupted while a waiting for messages from RabbitMQ "
                                + "message.", "queueName", _queueName, iex);
                        }
                    } catch (ShutdownSignalException sse) {
                        // no problem, we're shutting down
                    } catch (Throwable ex) {
                        logger.warning("Something nasty happened while processing a RabbitMQ " +
                            "message.", "queueName", _queueName, "delivery", delivery, ex);
                    }
                }
            }
        });
    }

    // used only for null-ish singleton
    private AMQPConnectedListener () {
        _queueName = null;
        _addr = null;
        _listener = null;
        _channelFactory = null;
        _shutdown = true; // appear closed
    }

    protected class AMQPInMessage implements InMessage
    {
        public AMQPInMessage (byte[] body, AMQP.BasicProperties props, long deliveryTag)
        {
            _body = body;
            _props = props;
            _deliveryTag = deliveryTag;

        }

        public void ack ()
            throws IOException
        {
            // Ensure this doesn't trip up with connect(), shutdown(), etc.
            synchronized (AMQPConnectedListener.this) {
                // Acknowledge the message only after it has been successfully processed.
                _channel.basicAck(_deliveryTag, false);
            }

        }

        public byte[] getBody ()
        {
            return _body;
        }

        public void reply (OutMessage message)
            throws IOException
        {
            // Ensure this doesn't trip up with connect(), shutdown(), etc.
            synchronized (AMQPConnectedListener.this) {
                _channel.basicPublish(_addr.exchange, _props.replyTo, _props,
                    message.encodeMessage());
            }
        }

        protected final AMQP.BasicProperties _props;
        protected final long _deliveryTag;
        protected final byte[] _body;
    }

    private static final Logger logger = Logger.getLogger(AMQPMessageConnection.class);

    protected final String _queueName;
    protected final DestinationAddress _addr;
    protected final MessageListener _listener;
    protected final ChannelFactory _channelFactory;
    protected ExecutorService _service;
    protected Channel _channel;
    protected String _consumerTag;
    protected boolean _shutdown;
}
