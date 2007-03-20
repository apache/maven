package org.apache.maven.lifecycle;

import org.apache.maven.context.BuildContext;
import org.apache.maven.context.BuildContextManager;
import org.apache.maven.context.ManagedBuildData;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Build context that contains the current project used by the executing mojo, plus any parent
 * project instances (not inheritance-wise, but fork-wise) in a stack that will be restored once the
 * current forked-execution is completed. This class also tracks the reports executed for a project,
 * for future reference by other mojos using the ${reports} expression.
 * 
 * @author jdcasey
 *
 */
public class LifecycleExecutionContext
    implements ManagedBuildData
{
    
    public static final String BUILD_CONTEXT_KEY = LifecycleExecutionContext.class.getName();
    
    private static final String CURRENT_PROJECT_KEY = "current-project";
    private static final String PROJECT_STACK_KEY = "fork-project-stack";
    private static final String REPORTS_KEY = "reports";
    
    private MavenProject currentProject;
    private Stack forkedProjectStack = new Stack();

    private Map reports = new HashMap();
    
    public LifecycleExecutionContext( MavenProject project )
    {
        this.currentProject = project;
    }
    
    private LifecycleExecutionContext()
    {
        // used for retrieval.
    }

    /**
     * Serialize the data in this context for storage. Any class in maven-core or the bootclasspath is legal
     * here as a datatype in the data map.
     */
    public Map getData()
    {
        Map data = new HashMap();
        data.put( CURRENT_PROJECT_KEY, currentProject );
        data.put( PROJECT_STACK_KEY, forkedProjectStack );
        data.put( REPORTS_KEY, reports );
        
        return data;
    }

    /**
     * Retrieve the master key under which the serialized data map for this context will be stored
     * in the main {@link BuildContext}.
     */
    public String getStorageKey()
    {
        return BUILD_CONTEXT_KEY;
    }

    /**
     * Deserialize the data for this context. Any class in maven-core or the bootclasspath is legal
     * here as a datatype in the data map.
     */
    public void setData( Map data )
    {
        this.currentProject = (MavenProject) data.get( CURRENT_PROJECT_KEY );
        this.forkedProjectStack = (Stack) data.get( PROJECT_STACK_KEY );
        this.reports = (Map) data.get( REPORTS_KEY );
    }
    
    /**
     * Push the existing currentProject onto the forked-project stack, and set the specified project
     * as the new current project. This signifies the beginning of a new forked-execution context.
     */
    public void addForkedProject( MavenProject project )
    {
        forkedProjectStack.push( currentProject );
        currentProject = project;
    }
    
    /**
     * Peel off the last forked project from the stack, and restore it as the currentProject. This
     * signifies the cleanup of a completed forked-execution context.
     */
    public MavenProject removeForkedProject()
    {
        if ( !forkedProjectStack.isEmpty() )
        {
            MavenProject lastCurrent = currentProject;
            currentProject = (MavenProject) forkedProjectStack.pop();
            
            return lastCurrent;
        }
        
        return null;
    }
    
    /**
     * Return the current project for use in a mojo execution.
     */
    public MavenProject getCurrentProject()
    {
        return currentProject;
    }
    
    /**
     * Convenience method to read the current context instance out of the main {@link BuildContext} provided
     * by the specified {@link BuildContextManager}. If no current context exist, return null.
     */
    public static LifecycleExecutionContext read( BuildContextManager buildContextManager )
    {
        BuildContext buildContext = buildContextManager.readBuildContext( true );
        
        LifecycleExecutionContext ctx = new LifecycleExecutionContext();
        if ( buildContext.retrieve( ctx ) )
        {
            return ctx;
        }
        
        return null;
    }
    
    /**
     * Remove the current lifecycle context from the main {@link BuildContext} provided by the
     * specified {@link BuildContextManager}.
     */
    public static void delete( BuildContextManager buildContextManager )
    {
        BuildContext buildContext = buildContextManager.readBuildContext( false );
        
        if ( buildContext != null )
        {
            buildContext.delete( BUILD_CONTEXT_KEY );
        }
    }
    
    /**
     * Store this lifecycle context in the main {@link BuildContext} provided by the specified
     * {@link BuildContextManager}.
     */
    public void store( BuildContextManager buildContextManager )
    {
        BuildContext buildContext = buildContextManager.readBuildContext( true );
        buildContext.store( this );
        buildContextManager.storeBuildContext( buildContext );
    }

    /**
     * Retrieve the list of reports ({@link MavenReport} instances) that have been executed against
     * this project, for use in another mojo's execution.
     */
    public List getReports()
    {
        return new ArrayList( reports.values() );
    }
    
    /**
     * Clear the reports for this project
     */
    public void clearReports()
    {
        reports.clear();
    }

    /**
     * Add a newly-executed report ({@link MavenReport} instance) to the reports collection, for 
     * future reference.
     */
    public void addReport( MojoDescriptor mojoDescriptor, MavenReport report )
    {
        reports.put( mojoDescriptor.getId(), report );
    }
    
}
