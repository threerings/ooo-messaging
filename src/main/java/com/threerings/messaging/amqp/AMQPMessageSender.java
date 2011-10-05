//
// $Id$

package com.threerings.messaging.amqp;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.ShutdownSignalException;
import com.samskivert.util.Logger;
import com.threerings.messaging.DestinationAddress;
import com.threerings.messaging.MessageSender;
import com.threerings.messaging.OutMessage;
import com.threerings.messaging.ReplyingDestination;

/**
 * Implementation of {@link MessageSender} for AMQP services.
 */
public class AMQPMessageSender
    implements MessageSender
{
    /**
     * Creates a new message sender using the given channel factory to get channels from a
     * connection.
     */
    public AMQPMessageSender (ChannelFactory channelFactory)
    {
        _channelFactory = channelFactory;
        _declaredExchanges = new HashSet<String>();
    }

    public void sendMessage (OutMessage msg, DestinationAddress addr)
        throws IOException
    {
        logger.info("Sending AMQP message", "msg", msg, "addr", addr);
        // We want to retry in case the connection is closed.
        int retries = 1;
        do {
            // This will reconnect if needed.
            Channel channel = _channelFactory.createChannel();
            try {

                // Ensure the exchange exists before continuing
                synchronized(this) {
                    if (!_declaredExchanges.contains(addr.exchange)) {
                        logger.info("Declaring AMQP exchange", "exchange", addr.exchange);
                        channel.exchangeDeclare(addr.exchange, "direct", true);
                        _declaredExchanges.add(addr.exchange);
                    }
                }
                channel.basicPublish(addr.exchange, addr.getRoutingKey(),
                    MessageProperties.PERSISTENT_BASIC, msg.encodeMessage());
                return;
            } catch(ShutdownSignalException sse) {
                // Already retried, just throw the exception.
                if (retries == 0) {
                    throw sse;
                }
                // Reconnect and try again.
                retries--;
            } finally {
                try {
                    channel.close(AMQP.REPLY_SUCCESS, "Message sent.");
                } catch (ShutdownSignalException sse) {
                    // Do nothing, it's already closed.
                }
            }
        } while (true);
    }

    public ReplyingDestination createReplyingDestination (DestinationAddress addr)
        throws IOException
    {
        AMQPReplyingDestination dest = new AMQPReplyingDestination(_channelFactory, addr);
        _destinations.add(dest);
        return dest;
    }

    /**
     * Closes underlying replying destinations.
     */
    public void close ()
        throws IOException
    {
        for (AMQPReplyingDestination destination : _destinations) {
            if (!destination.isClosed()) {
                destination.close();
            }
        }
    }

    protected final ChannelFactory _channelFactory;
    protected final Set<String> _declaredExchanges;
    protected final Set<AMQPReplyingDestination> _destinations =
        new CopyOnWriteArraySet<AMQPReplyingDestination>();

    protected static final Logger logger = Logger.getLogger(AMQPMessageSender.class);
}
