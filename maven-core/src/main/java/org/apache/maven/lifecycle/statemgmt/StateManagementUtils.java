package org.apache.maven.lifecycle.statemgmt;

import org.apache.maven.lifecycle.model.MojoBinding;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Constructs and matches MojoBinding instances that refer to the forked-execution context manager
 * mojos.
 * 
 * @author jdcasey
 *
 */
public final class StateManagementUtils
{

    public static final String GROUP_ID = "org.apache.maven.plugins.internal";

    public static final String ARTIFACT_ID = "maven-state-management";

    public static final String ORIGIN = "Maven build-state management";

    public static final String END_FORKED_EXECUTION_GOAL = "end-fork";

    public static final String START_FORKED_EXECUTION_GOAL = "start-fork";

    public static final String VERSION = "2.1";

    public static final String CLEAR_FORKED_EXECUTION_GOAL = "clear-fork-context";

    private static int CURRENT_FORK_ID = 0;

    private StateManagementUtils()
    {
    }

    /**
     * Create a new MojoBinding instance that refers to the internal mojo used to setup a new 
     * forked-execution context. Also, set the configuration to contain the forkId for this new
     * context.
     */
    public static MojoBinding createStartForkedExecutionMojoBinding()
    {
        MojoBinding binding = new MojoBinding();

        binding.setGroupId( GROUP_ID );
        binding.setArtifactId( ARTIFACT_ID );
        binding.setVersion( VERSION );
        binding.setGoal( START_FORKED_EXECUTION_GOAL );
        binding.setOrigin( ORIGIN );

        CURRENT_FORK_ID = (int) System.currentTimeMillis();
        
        Xpp3Dom config = new Xpp3Dom( "configuration" );
        Xpp3Dom forkId = new Xpp3Dom( "forkId" );
        forkId.setValue( "" + CURRENT_FORK_ID );
        
        config.addChild( forkId );
        
        binding.setConfiguration( config );

        return binding;
    }

    /**
     * Create a new MojoBinding instance that refers to the internal mojo used to end a 
     * forked-execution context. Also, set the configuration to contain the forkId for this new
     * context.
     */
    public static MojoBinding createEndForkedExecutionMojoBinding()
    {
        MojoBinding binding = new MojoBinding();

        binding.setGroupId( GROUP_ID );
        binding.setArtifactId( ARTIFACT_ID );
        binding.setVersion( VERSION );
        binding.setGoal( END_FORKED_EXECUTION_GOAL );
        binding.setOrigin( ORIGIN );

        Xpp3Dom config = new Xpp3Dom( "configuration" );
        Xpp3Dom forkId = new Xpp3Dom( "forkId" );
        forkId.setValue( "" + CURRENT_FORK_ID );
        
        config.addChild( forkId );
        
        binding.setConfiguration( config );

        return binding;
    }

    /**
     * Create a new MojoBinding instance that refers to the internal mojo used to cleanup a completed 
     * forked-execution context. Also, set the configuration to contain the forkId for this new
     * context.
     */
    public static MojoBinding createClearForkedExecutionMojoBinding()
    {
        MojoBinding binding = new MojoBinding();

        binding.setGroupId( GROUP_ID );
        binding.setArtifactId( ARTIFACT_ID );
        binding.setVersion( VERSION );
        binding.setGoal( CLEAR_FORKED_EXECUTION_GOAL );
        binding.setOrigin( ORIGIN );

        Xpp3Dom config = new Xpp3Dom( "configuration" );
        Xpp3Dom forkId = new Xpp3Dom( "forkId" );
        forkId.setValue( "" + CURRENT_FORK_ID );
        
        config.addChild( forkId );
        
        binding.setConfiguration( config );

        return binding;
    }
    
    /**
     * Return true if the specified MojoBinding refers to the internal mojo used to setup a new
     * forked-execution context. This is useful for formatting when listing the build plan, when
     * expression of these actual mojo names isn't necessarily useful, and can be confusing.
     */
    public static boolean isForkedExecutionStartMarker( MojoBinding binding )
    {
        return GROUP_ID.equals( binding.getGroupId() ) && ARTIFACT_ID.equals( binding.getArtifactId() )
            && START_FORKED_EXECUTION_GOAL.equals( binding.getGoal() );
    }

    /**
     * Return true if the specified MojoBinding refers to the internal mojo used to end a
     * forked-execution context. This is useful for formatting when listing the build plan, when
     * expression of these actual mojo names isn't necessarily useful, and can be confusing.
     */
    public static boolean isForkedExecutionEndMarker( MojoBinding binding )
    {
        return GROUP_ID.equals( binding.getGroupId() ) && ARTIFACT_ID.equals( binding.getArtifactId() )
            && END_FORKED_EXECUTION_GOAL.equals( binding.getGoal() );
    }

    /**
     * Return true if the specified MojoBinding refers to the internal mojo used to clean up a completed
     * forked-execution context. This is useful for formatting when listing the build plan, when
     * expression of these actual mojo names isn't necessarily useful, and can be confusing.
     */
    public static boolean isForkedExecutionClearMarker( MojoBinding binding )
    {
        return GROUP_ID.equals( binding.getGroupId() ) && ARTIFACT_ID.equals( binding.getArtifactId() )
            && CLEAR_FORKED_EXECUTION_GOAL.equals( binding.getGoal() );
    }

}
