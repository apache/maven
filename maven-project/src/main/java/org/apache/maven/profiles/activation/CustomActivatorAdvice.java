package org.apache.maven.profiles.activation;

import org.apache.maven.context.BuildContext;
import org.apache.maven.context.BuildContextManager;
import org.apache.maven.context.ManagedBuildData;

import java.util.Collections;
import java.util.Map;

/**
 * Advice for the custom profile activator, which tells how to handle cases where custom activators
 * cannot be found or configured. This is used to suppress missing activators when pre-scanning for
 * build extensions (which may contain the custom activator).
 * 
 * @author jdcasey
 */
public class CustomActivatorAdvice
    implements ManagedBuildData
{
    
    public static final String BUILD_CONTEXT_KEY = CustomActivatorAdvice.class.getName();
    
    private static final String FAIL_QUIETLY_KEY = "fail-quietly";
    
    private static final boolean DEFAULT_FAIL_QUIETLY = false;
    
    /**
     * If set to false, this tells the CustomProfileActivator to fail quietly when the specified 
     * custom profile activator cannot be found or configured correctly. Default behavior is to throw
     * a new ProfileActivationException.
     */
    private boolean failQuietly = DEFAULT_FAIL_QUIETLY;
    
    public void reset()
    {
        failQuietly = DEFAULT_FAIL_QUIETLY;
    }
    
    public void setFailQuietly( boolean ignoreMissingActivator )
    {
        this.failQuietly = ignoreMissingActivator;
    }
    
    public boolean failQuietly()
    {
        return failQuietly;
    }

    public String getStorageKey()
    {
        return BUILD_CONTEXT_KEY;
    }
    
    /**
     * Read the custom profile activator advice from the build context. If missing or the build
     * context has not been initialized, create a new instance of the advice and return that.
     */
    public static CustomActivatorAdvice getCustomActivatorAdvice( BuildContextManager buildContextManager )
    {
        BuildContext buildContext = buildContextManager.readBuildContext( false );
        
        CustomActivatorAdvice advice = new CustomActivatorAdvice();
        
        if ( buildContext != null )
        {
            buildContext.retrieve( advice );
        }
        
        return advice;
    }
    
    public void store( BuildContextManager buildContextManager )
    {
        BuildContext buildContext = buildContextManager.readBuildContext( true );
        
        buildContext.store( this );
        
        buildContextManager.storeBuildContext( buildContext );
    }

    public Map getData()
    {
        return Collections.singletonMap( FAIL_QUIETLY_KEY, Boolean.valueOf( failQuietly ) );
    }

    public void setData( Map data )
    {
        this.failQuietly = ((Boolean) data.get( FAIL_QUIETLY_KEY )).booleanValue();
    }
}
