package org.apache.maven.its.it0121.B;

import java.io.PrintWriter;
import java.util.Locale;

import org.apache.maven.its.it0121.D.Foo;

public class Out
{
    private PrintWriter writer;

    public Out( PrintWriter writer )
    {
        this.writer = writer;
    }

    public void println( String msg )
    {
        writer.println( "[Out] " + msg );
    }

    public String getTimestamp()
    {
        Foo foo = new Foo();
        return foo.getTimestamp( Locale.getDefault() );
    }
}
