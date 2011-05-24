package org.apache.maven.repository.automirror;

import java.io.StringWriter;

import junit.framework.TestCase;

public class RouterMirrorSerializerTest
    extends TestCase
{

    public void testSerializeOneMirror()
        throws Exception
    {
        final MirrorRoutingTable mirrorMap =
            new MirrorRoutingTable().addMirror( new MirrorRoute( "central", "http://repo1.maven.org/maven2",
                                                             "http://localhost:8081/nexus", 99, true ) );

        final StringWriter sw = new StringWriter();
        MirrorRouteSerializer.serialize( mirrorMap, sw );

        System.out.println( sw );
    }

    public void testSerializeToStringOneMirror()
        throws Exception
    {
        final MirrorRoutingTable mirrorMap =
            new MirrorRoutingTable().addMirror( new MirrorRoute( "central", "http://repo1.maven.org/maven2",
                                                             "http://localhost:8081/nexus", 99, true ) );

        System.out.println( MirrorRouteSerializer.serializeToString( mirrorMap ) );
    }

    public void testRoundTripOneMirror()
        throws Exception
    {
        final MirrorRoutingTable mirrorMap =
            new MirrorRoutingTable().addMirror( new MirrorRoute( "central", "http://repo1.maven.org/maven2",
                                                             "http://localhost:8081/nexus", 99, true ) );

        final String ser = MirrorRouteSerializer.serializeToString( mirrorMap );
        final MirrorRoutingTable result = MirrorRouteSerializer.deserialize( ser );

        assertEquals( mirrorMap, result );
        assertTrue( result.getHighestPriorityMirror( "http://repo1.maven.org/maven2" ).isEnabled() );
    }

}
