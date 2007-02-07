package org.apache.maven.context;

import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of the BuildContextManager, which handles instances of DefaultBuildContext.
 * 
 * @plexus.component role="org.apache.maven.context.BuildContextManager" role-hint="default"
 * @author jdcasey
 */
public class DefaultBuildContextManager
    implements BuildContextManager, Contextualizable
{
    public static final String ROLE_HINT = "default";
    
    protected static final String BUILD_CONTEXT_MAP_KEY = ROLE + ":" + ROLE_HINT + ":contextMap";
    
    private Context context;
    
    public DefaultBuildContextManager()
    {
        // used for plexus initialization
    }
    
    public DefaultBuildContextManager( Context context )
    {
        this.context = context;
    }
    
    /**
     * Clear the contents of the build context inside the container context.
     */
    public void clearBuildContext()
    {
        clearContextContainerMap();
    }

    /**
     * Retrieve the current BuildContext out of the container context.
     * 
     * @param create Whether to create the BuildContext if it doesn't exist in the container
     */
    public BuildContext readBuildContext( boolean create )
    {
        Map contextMap = getContextContainerMap( create );
        
        if ( !create && contextMap == null )
        {
            return null;
        }
        else
        {
            return new DefaultBuildContext( contextMap );
        }
    }

    /**
     * Store the given BuildContext inside the container.
     */
    public void storeBuildContext( BuildContext context )
    {
        if ( context instanceof DefaultBuildContext )
        {
            this.context.put( BUILD_CONTEXT_MAP_KEY, ((DefaultBuildContext)context).getContextMap() );
        }
        else
        {
            throw new IllegalArgumentException( this.getClass().getName() + " does not know how to store a context of type: " + context.getClass().getName() );
        }
    }

    protected Map getContextContainerMap( boolean create )
    {
        Map containerMap = null;

        if ( context.contains( BUILD_CONTEXT_MAP_KEY ) )
        {
            try
            {
                containerMap = (Map) context.get( BUILD_CONTEXT_MAP_KEY );
            }
            catch ( ContextException e )
            {
                throw new IllegalStateException( "Failed to retrieve BuildAdvisor "
                                + "serialization map from context, though the context claims it exists. Error: "
                                + e.getMessage() );
            }
        }
        else if ( create )
        {
            containerMap = new HashMap();
            context.put( BUILD_CONTEXT_MAP_KEY, containerMap );
        }

        return containerMap;
    }

    protected void clearContextContainerMap()
    {
        Map containerMap = getContextContainerMap( false );

        if ( containerMap != null )
        {
            containerMap.clear();
        }
    }

    /**
     * Retrieve the container context for storing the BuildContext data.
     */
    public void contextualize( Context context )
        throws ContextException
    {
        this.context = context;
    }

    public Context reorientToContext( Context context )
    {
        Context oldContext = this.context;
        this.context = context;
        
        return oldContext;
    }

}
