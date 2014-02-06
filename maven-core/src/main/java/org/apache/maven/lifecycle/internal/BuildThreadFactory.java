package org.apache.maven.lifecycle.internal;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple {@link ThreadFactory} implementation that ensures the corresponding threads have a meaningful name.
 */
public class BuildThreadFactory
    implements ThreadFactory
{
    private final AtomicInteger ID = new AtomicInteger();

    private String PREFIX = "BuilderThread";

    public Thread newThread( Runnable r )
    {
        return new Thread( r, String.format( "%s %d", PREFIX, ID.getAndIncrement() ) );
    }
}