package org.apache.maven.lifecycle.statemgmt;

import org.apache.maven.lifecycle.model.MojoBinding;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Constructs and matches MojoBinding instances that refer to the forked-execution context manager mojos.
 *
 * @author jdcasey
 *
 */
public final class StateManagementUtils
{

    public static final String GROUP_ID = "org.apache.maven.plugins.internal";

    public static final String ARTIFACT_ID = "maven-state-management";

    public static final String END_FORKED_EXECUTION_GOAL = "end-fork";

    public static final String START_FORKED_EXECUTION_GOAL = "start-fork";

    public static final String VERSION = "2.1";

    public static final String CLEAR_FORKED_EXECUTION_GOAL = "clear-fork-context";

    public static final String RESOLVE_LATE_BOUND_PLUGIN_GOAL = "resolve-late-bound-plugin";

    private static int CURRENT_FORK_ID = 0;

    private StateManagementUtils()
    {
    }

    /**
     * Create a new MojoBinding instance that refers to the internal mojo used to setup a new forked-execution context.
     * Also, set the configuration to contain the forkId for this new context.
     */
    public static MojoBinding createStartForkedExecutionMojoBinding()
    {
        MojoBinding binding = new MojoBinding();

        binding.setGroupId( GROUP_ID );
        binding.setArtifactId( ARTIFACT_ID );
        binding.setVersion( VERSION );
        binding.setGoal( START_FORKED_EXECUTION_GOAL );
        binding.setOrigin( MojoBinding.INTERNAL_ORIGIN );

        CURRENT_FORK_ID = (int) System.currentTimeMillis();

        Xpp3Dom config = new Xpp3Dom( "configuration" );
        Xpp3Dom forkId = new Xpp3Dom( "forkId" );
        forkId.setValue( "" + CURRENT_FORK_ID );

        config.addChild( forkId );

        binding.setConfiguration( config );

        return binding;
    }

    /**
     * Create a new MojoBinding instance that refers to the internal mojo used to end a forked-execution context. Also,
     * set the configuration to contain the forkId for this new context.
     */
    public static MojoBinding createEndForkedExecutionMojoBinding()
    {
        MojoBinding binding = new MojoBinding();

        binding.setGroupId( GROUP_ID );
        binding.setArtifactId( ARTIFACT_ID );
        binding.setVersion( VERSION );
        binding.setGoal( END_FORKED_EXECUTION_GOAL );
        binding.setOrigin( MojoBinding.INTERNAL_ORIGIN );

        Xpp3Dom config = new Xpp3Dom( "configuration" );
        Xpp3Dom forkId = new Xpp3Dom( "forkId" );
        forkId.setValue( "" + CURRENT_FORK_ID );

        config.addChild( forkId );

        binding.setConfiguration( config );

        return binding;
    }

    /**
     * Create a new MojoBinding instance that refers to the internal mojo used to cleanup a completed forked-execution
     * context. Also, set the configuration to contain the forkId for this new context.
     */
    public static MojoBinding createClearForkedExecutionMojoBinding()
    {
        MojoBinding binding = new MojoBinding();

        binding.setGroupId( GROUP_ID );
        binding.setArtifactId( ARTIFACT_ID );
        binding.setVersion( VERSION );
        binding.setGoal( CLEAR_FORKED_EXECUTION_GOAL );
        binding.setOrigin( MojoBinding.INTERNAL_ORIGIN );

        Xpp3Dom config = new Xpp3Dom( "configuration" );
        Xpp3Dom forkId = new Xpp3Dom( "forkId" );
        forkId.setValue( "" + CURRENT_FORK_ID );

        config.addChild( forkId );

        binding.setConfiguration( config );

        return binding;
    }

    /**
     * Return true if the specified MojoBinding refers to the internal mojo used to setup a new forked-execution
     * context. This is useful for formatting when listing the build plan, when expression of these actual mojo names
     * isn't necessarily useful, and can be confusing.
     */
    public static boolean isForkedExecutionStartMarker( final MojoBinding binding )
    {
        return GROUP_ID.equals( binding.getGroupId() ) && ARTIFACT_ID.equals( binding.getArtifactId() )
                        && START_FORKED_EXECUTION_GOAL.equals( binding.getGoal() );
    }

    /**
     * Return true if the specified MojoBinding refers to the internal mojo used to end a forked-execution context. This
     * is useful for formatting when listing the build plan, when expression of these actual mojo names isn't
     * necessarily useful, and can be confusing.
     */
    public static boolean isForkedExecutionEndMarker( final MojoBinding binding )
    {
        return GROUP_ID.equals( binding.getGroupId() ) && ARTIFACT_ID.equals( binding.getArtifactId() )
                        && END_FORKED_EXECUTION_GOAL.equals( binding.getGoal() );
    }

    /**
     * Return true if the specified MojoBinding refers to the internal mojo used to clean up a completed
     * forked-execution context. This is useful for formatting when listing the build plan, when expression of these
     * actual mojo names isn't necessarily useful, and can be confusing.
     */
    public static boolean isForkedExecutionClearMarker( final MojoBinding binding )
    {
        return GROUP_ID.equals( binding.getGroupId() ) && ARTIFACT_ID.equals( binding.getArtifactId() )
                        && CLEAR_FORKED_EXECUTION_GOAL.equals( binding.getGoal() );
    }

    /**
     * Create a new MojoBinding instance that refers to the internal mojo used to resolve a late-bound plugin just
     * before it is to be used. Also, set the configuration to contain the parameters necessary for this resolution.
     */
    public static MojoBinding createResolveLateBoundMojoBinding( final MojoBinding lateBound )
    {
        MojoBinding binding = new MojoBinding();

        binding.setGroupId( GROUP_ID );
        binding.setArtifactId( ARTIFACT_ID );
        binding.setVersion( VERSION );
        binding.setGoal( RESOLVE_LATE_BOUND_PLUGIN_GOAL );
        binding.setOrigin( MojoBinding.INTERNAL_ORIGIN );

        Xpp3Dom config = new Xpp3Dom( "configuration" );
        Xpp3Dom param = new Xpp3Dom( "groupId" );
        param.setValue( lateBound.getGroupId() );

        config.addChild( param );

        param = new Xpp3Dom( "artifactId" );
        param.setValue( lateBound.getArtifactId() );

        config.addChild( param );

        if ( lateBound.getVersion() != null )
        {
            param = new Xpp3Dom( "version" );
            param.setValue( lateBound.getVersion() );

            config.addChild( param );
        }

        param = new Xpp3Dom( "goal" );
        param.setValue( lateBound.getGoal() );

        config.addChild( param );

        binding.setConfiguration( config );

        return binding;
    }

    /**
     * Return true if the specified MojoBinding refers to the internal mojo used to resolve a late-bound mojo. This is
     * useful for formatting when listing the build plan, when expression of these actual mojo names isn't necessarily
     * useful, and can be confusing.
     */
    public static boolean isResolveLateBoundMojoBinding( final MojoBinding binding )
    {
        return GROUP_ID.equals( binding.getGroupId() ) && ARTIFACT_ID.equals( binding.getArtifactId() )
                        && RESOLVE_LATE_BOUND_PLUGIN_GOAL.equals( binding.getGoal() );
    }

}
