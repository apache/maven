package org.apache.maven.it;

import org.apache.maven.it.cli.StreamConsumer;

import java.io.Writer;
import java.io.PrintWriter;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class WriterStreamConsumer
    implements StreamConsumer
{
    private PrintWriter writer;

    public WriterStreamConsumer( Writer writer )
    {
        this.writer = new PrintWriter( writer );
    }

    public void consumeLine( String s )
    {
        writer.println( s );
    }
}
