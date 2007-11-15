package org.apache.maven.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.maven.MavenTransferListener;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

public final class CLIRequestUtils
{

    private CLIRequestUtils()
    {
    }

    public static MavenExecutionRequest buildRequest( CommandLine commandLine, boolean debug, boolean quiet, boolean showErrors )
    {
        // ----------------------------------------------------------------------
        // Now that we have everything that we need we will fire up plexus and
        // bring the maven component to life for use.
        // ----------------------------------------------------------------------

        boolean interactive = true;

        if ( commandLine.hasOption( CLIManager.BATCH_MODE ) )
        {
            interactive = false;
        }

        boolean pluginUpdateOverride = false;

        if ( commandLine.hasOption( CLIManager.FORCE_PLUGIN_UPDATES ) ||
            commandLine.hasOption( CLIManager.FORCE_PLUGIN_UPDATES2 ) )
        {
            pluginUpdateOverride = true;
        }
        else if ( commandLine.hasOption( CLIManager.SUPPRESS_PLUGIN_UPDATES ) )
        {
            pluginUpdateOverride = false;
        }

        boolean noSnapshotUpdates = false;
        if ( commandLine.hasOption( CLIManager.SUPRESS_SNAPSHOT_UPDATES ) )
        {
            noSnapshotUpdates = true;
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        List goals = commandLine.getArgList();

        boolean recursive = true;

        // this is the default behavior.
        String reactorFailureBehaviour = MavenExecutionRequest.REACTOR_FAIL_FAST;

        if ( commandLine.hasOption( CLIManager.NON_RECURSIVE ) )
        {
            recursive = false;
        }

        if ( commandLine.hasOption( CLIManager.FAIL_FAST ) )
        {
            reactorFailureBehaviour = MavenExecutionRequest.REACTOR_FAIL_FAST;
        }
        else if ( commandLine.hasOption( CLIManager.FAIL_AT_END ) )
        {
            reactorFailureBehaviour = MavenExecutionRequest.REACTOR_FAIL_AT_END;
        }
        else if ( commandLine.hasOption( CLIManager.FAIL_NEVER ) )
        {
            reactorFailureBehaviour = MavenExecutionRequest.REACTOR_FAIL_NEVER;
        }

        boolean offline = false;

        if ( commandLine.hasOption( CLIManager.OFFLINE ) )
        {
            offline = true;
        }

        boolean updateSnapshots = false;

        if ( commandLine.hasOption( CLIManager.UPDATE_SNAPSHOTS ) )
        {
            updateSnapshots = true;
        }

        String globalChecksumPolicy = null;

        if ( commandLine.hasOption( CLIManager.CHECKSUM_FAILURE_POLICY ) )
        {
            // todo; log
            System.out.println( "+ Enabling strict checksum verification on all artifact downloads." );

            globalChecksumPolicy = MavenExecutionRequest.CHECKSUM_POLICY_FAIL;
        }
        else if ( commandLine.hasOption( CLIManager.CHECKSUM_WARNING_POLICY ) )
        {
            // todo: log
            System.out.println( "+ Disabling strict checksum verification on all artifact downloads." );

            globalChecksumPolicy = MavenExecutionRequest.CHECKSUM_POLICY_WARN;
        }

        File baseDirectory = new File( System.getProperty( "user.dir" ) );

        // ----------------------------------------------------------------------
        // Profile Activation
        // ----------------------------------------------------------------------

        List activeProfiles = new ArrayList();

        List inactiveProfiles = new ArrayList();

        if ( commandLine.hasOption( CLIManager.ACTIVATE_PROFILES ) )
        {
            String profilesLine = commandLine.getOptionValue( CLIManager.ACTIVATE_PROFILES );

            StringTokenizer profileTokens = new StringTokenizer( profilesLine, "," );

            while ( profileTokens.hasMoreTokens() )
            {
                String profileAction = profileTokens.nextToken().trim();

                if ( profileAction.startsWith( "-" ) )
                {
                    activeProfiles.add( profileAction.substring( 1 ) );
                }
                else if ( profileAction.startsWith( "+" ) )
                {
                    inactiveProfiles.add( profileAction.substring( 1 ) );
                }
                else
                {
                    // TODO: deprecate this eventually!
                    activeProfiles.add( profileAction );
                }
            }
        }

        MavenTransferListener transferListener;

        if ( interactive )
        {
            transferListener = new ConsoleDownloadMonitor();
        }
        else
        {
            transferListener = new BatchModeDownloadMonitor();
        }

        transferListener.setShowChecksumEvents( false );

        // This means to scan a directory structure for POMs and process them.
        boolean useReactor = false;

        if ( commandLine.hasOption( CLIManager.REACTOR ) )
        {
            useReactor = true;
        }

        String alternatePomFile = null;
        if ( commandLine.hasOption( CLIManager.ALTERNATE_POM_FILE ) )
        {
            alternatePomFile = commandLine.getOptionValue( CLIManager.ALTERNATE_POM_FILE );
        }

        int loggingLevel;

        if ( debug )
        {
            loggingLevel = MavenExecutionRequest.LOGGING_LEVEL_DEBUG;
        }
        else if ( quiet )
        {
            // TODO: we need to do some more work here. Some plugins use sys out or log errors at info level.
            // Ideally, we could use Warn across the board
            loggingLevel = MavenExecutionRequest.LOGGING_LEVEL_ERROR;
            // TODO:Additionally, we can't change the mojo level because the component key includes the version and it isn't known ahead of time. This seems worth changing.
        }
        else
        {
            loggingLevel = MavenExecutionRequest.LOGGING_LEVEL_INFO;
        }

        Properties executionProperties = getExecutionProperties( commandLine );

        MavenExecutionRequest request = new DefaultMavenExecutionRequest()
            .setBaseDirectory( baseDirectory )
            .setGoals( goals )
            .setProperties( executionProperties ) // optional
            .setReactorFailureBehavior( reactorFailureBehaviour ) // default: fail fast
            .setRecursive( recursive ) // default: true
            .setUseReactor( useReactor ) // default: false
            .setShowErrors( showErrors ) // default: false
            .setInteractiveMode( interactive ) // default: false
            .setOffline( offline ) // default: false
            .setUsePluginUpdateOverride( pluginUpdateOverride )
            .addActiveProfiles( activeProfiles ) // optional
            .addInactiveProfiles( inactiveProfiles ) // optional
            .setLoggingLevel( loggingLevel ) // default: info
            .setTransferListener( transferListener ) // default: batch mode which goes along with interactive
            .setUpdateSnapshots( updateSnapshots ) // default: false
            .setNoSnapshotUpdates( noSnapshotUpdates ) // default: false
            .setGlobalChecksumPolicy( globalChecksumPolicy ); // default: warn

        if ( alternatePomFile != null )
        {
            request.setPom( new File( alternatePomFile ) );
            System.out.println( "Request pom set to: " + request.getPom() );
        }

        return request;
    }

    // ----------------------------------------------------------------------
    // System properties handling
    // ----------------------------------------------------------------------

    private static Properties getExecutionProperties( CommandLine commandLine )
    {
        Properties executionProperties = new Properties();

        // ----------------------------------------------------------------------
        // Options that are set on the command line become system properties
        // and therefore are set in the session properties. System properties
        // are most dominant.
        // ----------------------------------------------------------------------

        if ( commandLine.hasOption( CLIManager.SET_SYSTEM_PROPERTY ) )
        {
            String[] defStrs = commandLine.getOptionValues( CLIManager.SET_SYSTEM_PROPERTY );

            if ( defStrs != null )
            {
                for ( int i = 0; i < defStrs.length; ++i )
                {
                    setCliProperty( defStrs[i], executionProperties );
                }
            }
        }

        executionProperties.putAll( System.getProperties() );

        return executionProperties;
    }

    private static void setCliProperty( String property,
                                        Properties executionProperties )
    {
        String name;

        String value;

        int i = property.indexOf( "=" );

        if ( i <= 0 )
        {
            name = property.trim();

            value = "true";
        }
        else
        {
            name = property.substring( 0, i ).trim();

            value = property.substring( i + 1 ).trim();
        }

        executionProperties.setProperty( name, value );

        // ----------------------------------------------------------------------
        // I'm leaving the setting of system properties here as not to break
        // the SystemPropertyProfileActivator. This won't harm embedding. jvz.
        // ----------------------------------------------------------------------

        System.setProperty( name, value );
    }

}
