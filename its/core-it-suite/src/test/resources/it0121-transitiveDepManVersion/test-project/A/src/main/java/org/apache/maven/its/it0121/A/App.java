package org.apache.maven.its.it0121.A;

import java.io.PrintWriter;

import org.apache.maven.its.it0121.B.Out;

public class App
{
    public static void main(String args[])
    {
        (new App()).output( new PrintWriter( System.out ) );
    }

    public void output( PrintWriter writer )
    {
        Out out = new Out( writer );

        out.println( "Welcome to the org.apache.maven.its.it0121.A App." );
        out.println( "The time is now: " + out.getTimestamp() );
    }
}
