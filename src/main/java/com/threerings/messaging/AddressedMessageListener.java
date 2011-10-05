//
// $Id$

package com.threerings.messaging;

import java.io.IOException;

import com.samskivert.util.StringUtil;

/**
 * A listener that knows which queue and address it is interested in.
 */
public class AddressedMessageListener implements MessageListener
{
    public final String queueName;

    public final DestinationAddress address;

    public AddressedMessageListener (String queueName, DestinationAddress address,
        MessageListener listener)
    {
        this.queueName = queueName;
        this.address = address;
        _listener = listener;
    }

    public void received (InMessage message) throws IOException
    {
        _listener.received(message);
    }

    @Override
    public int hashCode ()
    {
        int prime = 31;
        int result = 1;
        result = prime * result + queueName.hashCode();
        result = prime * result + address.hashCode();
        result = prime * result + _listener.hashCode();
        return result;
    }

    @Override
    public boolean equals (Object other)
    {
        if (other instanceof AddressedMessageListener) {
            AddressedMessageListener otherListener = (AddressedMessageListener) other;
            return queueName.equals(otherListener.queueName) &&
                   address.equals(otherListener.address) &&
                   _listener.equals(otherListener._listener);
        }
        return false;
    }

    @Override
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    /**
     * The delegate that implements the actual functionality to perform when a message is received.
     */
    protected final MessageListener _listener;
}
