package org.apache.maven.context;

import org.codehaus.plexus.PlexusTestCase;

public abstract class AbstractBuildContextManagerTest
    extends PlexusTestCase
{
    private BuildContextManager mgr;
    
    protected abstract String getRoleHintBeforeSetUp();
    
    protected BuildContextManager getBuildContextManager()
    {
        return mgr;
    }
    
    public void setUp() throws Exception
    {
        super.setUp();
        
        mgr = (BuildContextManager) lookup( BuildContextManager.ROLE, getRoleHintBeforeSetUp() );
    }
    
    public void testNewUnstoredInstance_ShouldReturnValidContextInstance()
    {
        BuildContext context = mgr.newUnstoredInstance();
        
        assertNotNull( context );
        
        String key = "key";
        String value = "value";
        
        context.put( key, value );
        
        assertEquals( value, context.get( key ) );
        
        context.delete( key );
        
        assertNull( context.get( key ) );
    }

    public void testNewUnstoredInstance_SuccessiveCallsShouldReturnDistinctContextInstances()
    {
        BuildContext context = mgr.newUnstoredInstance();
        BuildContext context2 = mgr.newUnstoredInstance();
        
        assertNotNull( context );
        assertNotNull( context2 );
        assertNotSame( context, context2 );
    }
    
    public void testStoreAndRead_ShouldRetrieveStoredValueAfterRead()
    {
        BuildContext ctx = mgr.newUnstoredInstance();
        
        String key = "key";
        String value = "value";
        
        ctx.put( key, value );
        
        mgr.storeBuildContext( ctx );
        
        BuildContext ctx2 = mgr.readBuildContext( false );
        
        assertNotNull( ctx2 );
        assertEquals( value, ctx2.get( key ) );
    }

    public void testStoreAndClear_ShouldNotRetrieveStoredValueAfterClear()
    {
        BuildContext ctx = mgr.newUnstoredInstance();
        
        String key = "key";
        String value = "value";
        
        ctx.put( key, value );
        
        mgr.storeBuildContext( ctx );
        
        // verify that we can get the value back out.
        BuildContext ctx2 = mgr.readBuildContext( false );
        
        assertNotNull( ctx2 );
        assertEquals( value, ctx2.get( key ) );
        
        mgr.clearBuildContext();
        
        BuildContext ctx3 = mgr.readBuildContext( false );
        
        assertNull( ctx3.get( key ) );
    }

}
