//
// $Id$

/**
 *
 */
package com.threerings.messaging;

import java.nio.ByteBuffer;

/**
 * A message containing a single integer.
 *
 * This class is immutable.
 */
public final class IntMessage implements OutMessage
{
    /** Value of the message. */
    public final int value;

    /**
     * Creates a new integer message with the given value.
     */
    public IntMessage (final int value)
    {
        this.value = value;
    }

    /**
     * Creates a new integer message from an encoded byte stream.  This and {@link #encodeMessage()}
     * are reciprocal operations.
     */
    public IntMessage (final byte[] bytes)
    {
        value = ByteBuffer.wrap(bytes).getInt();
    }

    /**
     * Encodes the integer message into a byte stream.  This and {@link #IntMessage(byte[])} are
     * reciprocal operations.
     */
    public byte[] encodeMessage ()
    {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    @Override
    public String toString ()
    {
        return Integer.toString(value);
    }
}