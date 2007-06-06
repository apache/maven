package org.apache.maven.its.it0121.A;

import java.io.*;
import junit.framework.*;

public class AppTest
    extends TestCase
{
    public void testOutput()
    {
        App app = new App();
        StringWriter actual = new StringWriter();
        PrintWriter writer = new PrintWriter( actual );
        app.output( writer );

        assertTrue( actual.getBuffer().length() > 10 );
    }
}
