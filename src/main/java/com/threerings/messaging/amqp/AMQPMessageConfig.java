//
// $Id$

package com.threerings.messaging.amqp;

import com.rabbitmq.client.Address;

/**
 * Configuration information for connecting to an AMQP server for messaging.
 *
 * This class is immutable.
 */
public final class AMQPMessageConfig
{
    /** List of addresses at which to reach an AMQP server cluster. */
    public final Address[] hostAddresses;

    /** Virtual host to connect to. */
    public final String virtualHost;

    /** User name to connect as. */
    public final String username;

    /** Password for the user. */
    public final String password;

    /** Realm to access. */
    public final String realm;

    /** Seconds between heartbeats between client and server. */
    public final int heartBeat;

    /**
     * Constructs a new AMQP configuration.
     *
     * @param hostAddresses List of addresses at which to reach an AMQP server cluster. This
     * should be formatted like "address1:port1,address2:port2,..."
     * @param virtualHost Virtual host to connect to.
     * @param username User name to connect as.
     * @param password Password for the user.
     * @param realm Realm to access.
     * @param heartBeat Seconds between heartbeats between client and server.
     */
    public AMQPMessageConfig (String hostAddresses, String virtualHost, String username,
            String password, String realm, int heartBeat)
    {
        this.hostAddresses = Address.parseAddresses(hostAddresses);
        this.virtualHost = virtualHost;
        this.username = username;
        this.password = password;
        this.realm = realm;
        this.heartBeat = heartBeat;
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("hostAddresses: {");
        for (final Address address : hostAddresses) {
            sb.append(address.toString()).append(", ");
        }
        sb.append("}, virtualHost: ").append(virtualHost);
        sb.append(", username: ").append(username);
        sb.append(", realm: ").append(realm);
        sb.append(", heartBeat: ").append(heartBeat);
        return sb.toString();
    }
}
