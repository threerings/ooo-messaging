//
// $Id$

package com.threerings.messaging.amqp;

import java.io.IOException;

import com.rabbitmq.client.Channel;

/**
 * Creates and manages channels for a connection.
 */
interface ChannelFactory
{
    /**
     * Creates a new AMQP channel to use.
     *
     * @return The new AMQP channel.
     * @throws IOException An error occurred while creating the channel.
     */
    Channel createChannel ()
        throws IOException;

}
