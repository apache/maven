package org.apache.maven.lifecycle.goal;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.decoration.GoalDecoratorBindings;
import org.apache.maven.lifecycle.session.MavenSession;
import org.apache.maven.plugin.FailureResponse;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class MavenGoalExecutionContext
{
    private MavenSession session;

    private String failedGoal;

    private FailureResponse failureResponse;

    private MojoDescriptor mojoDescriptor;

    private List resolvedGoals;

    private GoalDecoratorBindings goalDecoratorBindings;

    private String goalName;

    public MavenGoalExecutionContext( MavenSession session, MojoDescriptor goal )
    {
        this.session  = session;

        this.mojoDescriptor = goal;
    }

    public MavenSession getSession()
    {
        return session;
    }

    // ----------------------------------------------------------------------
    // Delegation to the session
    // ----------------------------------------------------------------------

    public MavenProject getProject()
    {
        return session.getProject();
    }

    public ArtifactRepository getLocalRepository()
    {
        return session.getLocalRepository();
    }

    public Set getRemoteRepositories()
    {
        return session.getRemoteRepositories();
    }

    public Object lookup( String role )
        throws ComponentLookupException
    {
        return session.lookup( role );
    }

    public Object lookup( String role, String hint )
        throws ComponentLookupException
    {
        return session.lookup( role, hint );
    }

    public void release( Object component )
    {
        session.release( component );
    }

    // ----------------------------------------------------------------------

    public boolean requiresDependencyResolution()
    {
        return mojoDescriptor.requiresDependencyResolution();
    }

    public MojoDescriptor getMojoDescriptor()
    {
        return mojoDescriptor;
    }

    public void setMojoDescriptor( MojoDescriptor mojoDescriptor )
    {
        this.mojoDescriptor = mojoDescriptor;
    }

    public List getResolvedGoals()
    {
        return resolvedGoals;
    }

    public void setResolvedGoals( List resolvedGoals )
    {
        this.resolvedGoals = resolvedGoals;
    }

    public MojoDescriptor getMojoDescriptor( String mojoDescriptorName )
    {
        return session.getPluginManager().getMojoDescriptor( mojoDescriptorName );
    }

    public String getPluginId( MojoDescriptor mojoDescriptor )
    {
        return mojoDescriptor.getId();
    }

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

    public GoalDecoratorBindings getGoalDecoratorBindings()
    {
        return goalDecoratorBindings;
    }

    public void setGoalDecoratorBindings( GoalDecoratorBindings goalDecoratorBindings )
    {
        this.goalDecoratorBindings = goalDecoratorBindings;
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
