//
// $Id$

package com.threerings.messaging;

/**
 * Represents an address to send messages to.  Addresses contain the following information:
 *
 * <ul>
 *     <li>exchange: A logical grouping of queues for all applications that comprise a system.</li>
 *     <li>application: An application wanting to send messages on an exchange.</li>
 *     <li>service: Service or component of the application.</li>
 *     <li>command: Individual command (or method) in the service.</li>
 * </ul>
 *
 * Generally every unique operation should have a corresponding address to send messages to.  This
 * will correspond to a queue (mailbox) that will contain the messages sent to the address.  More
 * than one address can potentially point to the same queue.
 *
 * This class is immutable.
 */
public class DestinationAddress
{
    /** The exchange component of the address. */
    public final String exchange;

    /** The application component of the address. */
    public final String application;

    /** The service component of the address. */
    public final String service;

    /** The command component of the address. */
    public final String command;

    /**
     * Creates a destination address from a string formatted like:
     * "application.service.command@exchange".
     *
     * @param src String to parse the address from.
     */
    public DestinationAddress (String src)
    {
        String[] destinationAndExchange = src.split("@");
        String[] routingKey = destinationAndExchange[0].split("\\.");
        exchange = destinationAndExchange[1];
        application = routingKey[0];
        service = routingKey[1];
        command = routingKey[2];
    }

    /**
     * Creates a destination address from the given application, service, command, and exchange
     * components.
     */
    public DestinationAddress (String application, String service, String command, String exchange)
    {
        this.exchange = exchange;
        this.application = application;
        this.service = service;
        this.command = command;
    }

    /**
     * Creates a routing key which identifies a command within an exchange.  This is formatted like:
     * "application.service.command".
     */
    public String getRoutingKey ()
    {
        return application + '.' + service + '.' + command;
    }

    /**
     * Returns the destination address formatted like "application.service.command@exchange".
     * This can be passed into {@link #DestinationAddress(String)} to recreate the
     * DestinationAddress.
     */
    @Override
    public String toString()
    {
        return getRoutingKey() + '@' + exchange;
    }

    @Override
    public boolean equals (Object other)
    {
        if (other instanceof DestinationAddress) {
            DestinationAddress otherAddress = (DestinationAddress) other;
            return exchange.equals(otherAddress.exchange) &&
                   application.equals(otherAddress.application) &&
                   service.equals(otherAddress.service) &&
                   command.equals(otherAddress.command);
        }
        return false;
    }

    @Override
    public int hashCode ()
    {
        return toString().hashCode();
    }
}
