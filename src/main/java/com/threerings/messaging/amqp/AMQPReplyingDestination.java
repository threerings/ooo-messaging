//
// $Id$

package com.threerings.messaging.amqp;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.RpcClient;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.utility.BlockingCell;
import com.threerings.messaging.DestinationAddress;
import com.threerings.messaging.OutMessage;
import com.threerings.messaging.ReplyingDestination;

/**
 * Implementation of {@link ReplyingDestination} for AMQP.
 */
public class AMQPReplyingDestination
    implements ReplyingDestination
{
    /**
     * Creates a new replying destination for the given address.
     *
     * @param channelFactory Factory for creating AMQP channels.
     * @param addr Address of destination.
     * @throws IOException An error occurred while attempting to connect the destination.
     */
    public AMQPReplyingDestination (ChannelFactory channelFactory, DestinationAddress addr)
        throws IOException
    {
        _channelFactory = channelFactory;
        _destAddress = addr;
        createClient();
    }

    public byte[] sendMessage (OutMessage msg, long timeout)
        throws IOException, TimeoutException
    {
        // Sending the message can be retried once, if the current client is closed.
        int retries = 1;
        BlockingCell<Object> k;
        do {
            try {
                _client.checkConsumer();
                k = new BlockingCell<Object>();
                BasicProperties props;

                // Synchronize on continuation map since the client synchronizes on it as well.
                synchronized (_client.getContinuationMap()) {
                    String replyId = Integer.toString(_correlationId++);
                    props = new BasicProperties(null, null, null, null,
                                                 null, replyId,
                                                 _client.getReplyQueue(), null, null, null,
                                                 null, null, null, null);
                    _client.getContinuationMap().put(replyId, k);
                }

                // We don't want to run this at the same time the client is being closed or created.
                synchronized (this) {
                    _client.publish(props, msg.encodeMessage());
                }
                break;
            } catch (ShutdownSignalException sse) {
                // If we've already retried, just let the exception go.
                if (retries == 0) {
                    throw sse;
                }
                // Recreate the client and try again.
                createClient();
                retries--;
            } catch (IOException ioe) {
                // Same as above
                if (retries == 0) {
                    throw ioe;
                }
                createClient();
                retries--;
            }
        } while(true);

        // Wait for a response, with some time out.  If we receive a shutdown signal here, just
        // throw an exception up to the user.
        Object reply = k.uninterruptibleGet((int)timeout);
        if (reply instanceof ShutdownSignalException) {
            ShutdownSignalException sig = (ShutdownSignalException) reply;
            // Love the missing cause in the constructor in Java < 6...
            IOException ioe = new IOException("Channel shutdown.");
            ioe.initCause(sig);
            throw ioe;
        }
        return (byte[])reply;
    }

    public synchronized void close ()
        throws IOException
    {
        _client.close();
        if (_channel.isOpen()) {
            _channel.close(AMQP.REPLY_SUCCESS, "Replying destination closed.");
        }
        _client = null;
    }

    public synchronized boolean isClosed ()
    {
        return _client == null;
    }

    private synchronized void createClient ()
        throws IOException
    {
        _channel = _channelFactory.createChannel();
        _channel.exchangeDeclare(_destAddress.exchange, "direct", true);
        _client = new RpcClient(_channel, _destAddress.exchange, _destAddress.getRoutingKey());
        _channel.queueBind(_client.getReplyQueue(), _destAddress.exchange, _client.getReplyQueue());
    }

    protected int _correlationId;
    protected RpcClient _client;
    protected Channel _channel;
    protected final ChannelFactory _channelFactory;
    protected final DestinationAddress _destAddress;
}
