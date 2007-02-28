package org.apache.maven.context;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractBuildContextTest
    extends TestCase
{
    
    protected abstract BuildContext newContext();

    public void testPutAndGet_ShouldStoreAndRetrieveKeyValuePairOfStrings()
    {
        BuildContext ctx = newContext();
        
        String key = "key";
        String value = "value";
        
        ctx.put( key, value );
        
        assertEquals( value, ctx.get( key ) );
    }
    
    public void testPutAndGet_ShouldStoreAndRetrieveStringKeyWithMapValue()
    {
        BuildContext ctx = newContext();
        
        String key = "key";
        Map value = new HashMap();
        
        String key2 = "key2";
        String value2 = "value";
        
        value.put( key2, value2 );
        
        ctx.put( key, value );
        
        assertSame( value, ctx.get( key ) );
        
        assertEquals( value2, ((Map) ctx.get( key )).get( key2 ) );
    }
    
    public void testPutDeleteAndGet_ShouldStoreKeyValuePairDeleteThemAndRetrieveNull()
    {
        BuildContext ctx = newContext();
        
        String key = "key";
        String value = "value";
        
        ctx.put( key, value );
        
        assertEquals( value, ctx.get( key ) );
        
        ctx.delete( key );
        
        assertNull( ctx.get( key ) );
    }
    
}
