package org.apache.maven.repository.automirror;

import java.io.StringWriter;

import junit.framework.TestCase;

public class RouterMirrorSerializerTest
    extends TestCase
{

    public void testSerializeOneMirror()
        throws Exception
    {
        final RouterMirrors mirrorMap =
            new RouterMirrors().addMirror( new RouterMirror( "central", "http://repo1.maven.org/maven2",
                                                             "http://localhost:8081/nexus", 99, true ) );

        final StringWriter sw = new StringWriter();
        RouterMirrorSerializer.serialize( mirrorMap, sw );

        System.out.println( sw );
    }

    public void testSerializeToStringOneMirror()
        throws Exception
    {
        final RouterMirrors mirrorMap =
            new RouterMirrors().addMirror( new RouterMirror( "central", "http://repo1.maven.org/maven2",
                                                             "http://localhost:8081/nexus", 99, true ) );

        System.out.println( RouterMirrorSerializer.serializeToString( mirrorMap ) );
    }

    public void testRoundTripOneMirror()
        throws Exception
    {
        final RouterMirrors mirrorMap =
            new RouterMirrors().addMirror( new RouterMirror( "central", "http://repo1.maven.org/maven2",
                                                             "http://localhost:8081/nexus", 99, true ) );

        final String ser = RouterMirrorSerializer.serializeToString( mirrorMap );
        final RouterMirrors result = RouterMirrorSerializer.deserialize( ser );

        assertEquals( mirrorMap, result );
        assertTrue( result.getHighestPriorityMirror( "http://repo1.maven.org/maven2" ).isEnabled() );
    }

}
