package org.apache.maven.context;

import java.util.Collections;
import java.util.Map;

public class DefaultBuildContextTest
    extends AbstractBuildContextTest
{

    protected BuildContext newContext()
    {
        return new DefaultBuildContext();
    }
    
    public void testConstructor_ShouldThrowNPEWhenContextMapParameterIsNull()
    {
        try
        {
            new DefaultBuildContext( null );
            
            fail( "Should throw NPE when contextMap parameter is null." );
        }
        catch( NullPointerException e )
        {
            // should happen.
        }
    }

    public void testConstructor_ShouldRetrieveValueFromPreExistingContextMap()
    {
        String key = "key";
        String value = "value";
        
        Map contextMap = Collections.singletonMap( key, value );
        BuildContext ctx = new DefaultBuildContext( contextMap );
        
        assertEquals( value, ctx.get( key ) );
    }

}
