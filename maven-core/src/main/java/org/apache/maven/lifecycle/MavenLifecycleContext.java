package org.apache.maven.lifecycle;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.decoration.GoalDecoratorBindings;
import org.apache.maven.plugin.FailureResponse;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositoryUtils;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenLifecycleContext
{
    private String failedGoal;

    private FailureResponse failureResponse;

    private PlexusContainer container;

    private MavenProject project;

    private MojoDescriptor mojoDescriptor;

    private List resolvedGoals;

    private PluginManager pluginManager;

    private Set pluginDependencies;

    private GoalDecoratorBindings goalDecoratorBindings;

    private ArtifactRepository localRepository;

    private Set remoteRepositories;

    private String goalName;

    public MavenLifecycleContext( PlexusContainer container,
                                  MavenProject project,
                                  MojoDescriptor goal,
                                  ArtifactRepository localRepository )
        throws Exception
    {
        this.container = container;

        this.project = project;

        this.mojoDescriptor = goal;

        this.localRepository = localRepository;

        pluginManager = (PluginManager) lookup( PluginManager.ROLE );

        pluginDependencies = new HashSet();
    }

    // ----------------------------------------------------------------------
    // Provide an easy way to lookup plexus components
    // ----------------------------------------------------------------------
    public Object lookup( String role )
        throws ComponentLookupException
    {
        return container.lookup( role );
    }

    public Object lookup( String role, String roleHint )
        throws ComponentLookupException
    {
        return container.lookup( role, roleHint );
    }

    public void release( Object component )
    {
        if ( component != null )
        {
            try
            {
                container.release( component );
            }
            catch ( Exception e )
            {
                //@todo what to do here?
            }
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------
    public boolean requiresDependencyResolution()
    {
        return mojoDescriptor.requiresDependencyResolution();
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------
    public PlexusContainer getContainer()
    {
        return container;
    }

    public MavenProject getProject()
    {
        return project;
    }

    public MojoDescriptor getMojoDescriptor()
    {
        return mojoDescriptor;
    }

    public void setMojoDescriptor( MojoDescriptor mojoDescriptor )
    {
        this.mojoDescriptor = mojoDescriptor;
    }

    // ----------------------------------------------------------------------
    // Resolved MojoDescriptors
    // ----------------------------------------------------------------------
    public List getResolvedGoals()
    {
        return resolvedGoals;
    }

    public void setResolvedGoals( List resolvedGoals )
    {
        this.resolvedGoals = resolvedGoals;
    }

    // ----------------------------------------------------------------------
    // Delegation to the plugin manager
    // ----------------------------------------------------------------------
    public MojoDescriptor getMojoDescriptor( String mojoDescriptorName )
    {
        return pluginManager.getMojoDescriptor( mojoDescriptorName );
    }

    public String getPluginId( MojoDescriptor mojoDescriptor )
    {
        return mojoDescriptor.getId();
    }

    // ----------------------------------------------------------------------
    // Execution failure
    // ----------------------------------------------------------------------
    public void setExecutionFailure( String failedGoal, FailureResponse response )
    {
        this.failedGoal = failedGoal;

        this.failureResponse = response;
    }

    public boolean isExecutionFailure()
    {
        return ( failedGoal != null );
    }

    public String getFailedGoal()
    {
        return failedGoal;
    }

    public void setFailedGoal( String failedGoal )
    {
        this.failedGoal = failedGoal;
    }

    public FailureResponse getFailureResponse()
    {
        return failureResponse;
    }

    public void setFailureResponse( FailureResponse failureResponse )
    {
        this.failureResponse = failureResponse;
    }

    public Set getPluginDependencies()
    {
        return pluginDependencies;
    }

    public void setPluginDependencies( Set pluginDependencies )
    {
        this.pluginDependencies = pluginDependencies;
    }

    public GoalDecoratorBindings getGoalDecoratorBindings()
    {
        return goalDecoratorBindings;
    }

    public void setGoalDecoratorBindings( GoalDecoratorBindings goalDecoratorBindings )
    {
        this.goalDecoratorBindings = goalDecoratorBindings;
    }

    // ----------------------------------------------------------------------
    // Local repository
    // ----------------------------------------------------------------------

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public Set getRemoteRepositories()
    {
        if ( remoteRepositories == null )
        {
            remoteRepositories = RepositoryUtils.mavenToWagon( project.getRepositories() );
        }

        return remoteRepositories;
    }

    public String getGoalName()
    {
        return goalName;
    }

    public void setGoalName( String goalName )
    {
        this.goalName = goalName;
    }
}
