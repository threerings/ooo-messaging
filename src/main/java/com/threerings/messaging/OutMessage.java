//
// $Id$

package com.threerings.messaging;

/**
 * Interface for message objects that can be encoded into a byte array to be sent as the body of a
 * message.
 */
public interface OutMessage
{
    /**
     * Encodes this message as a byte array.
     */
    byte[] encodeMessage ();
}
