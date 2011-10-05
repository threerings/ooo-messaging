//
// $Id$

package com.threerings.messaging;

import java.io.IOException;

import com.samskivert.util.Logger;

/**
 * An implementation of {@link ConnectedListener} that is not actually connect and does nothing.
 */
public class NullConnectedListener
    implements ConnectedListener
{
    public void close ()
        throws IOException
    {
        logger.debug("Closing null connected listener.");
        _closed = true;
    }

    public boolean isClosed ()
    {
        return _closed;
    }

    protected final static Logger logger = Logger.getLogger(NullConnectedListener.class);

    protected boolean _closed;
}
