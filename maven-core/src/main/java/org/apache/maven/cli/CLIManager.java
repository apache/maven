package org.apache.maven.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.ArrayList;
import java.util.List;

public class CLIManager
{

    public static final char ALTERNATE_POM_FILE = 'f';

    public static final char BATCH_MODE = 'B';

    public static final char SET_SYSTEM_PROPERTY = 'D';

    public static final char OFFLINE = 'o';

    public static final char REACTOR = 'r';

    public static final char QUIET = 'q';

    public static final char DEBUG = 'X';

    public static final char ERRORS = 'e';

    public static final char HELP = 'h';

    public static final char VERSION = 'v';

    public static final char SHOW_VERSION = 'V';

    private Options options;

    public static final char NON_RECURSIVE = 'N';

    public static final char UPDATE_SNAPSHOTS = 'U';

    public static final char ACTIVATE_PROFILES = 'P';

    public static final String FORCE_PLUGIN_UPDATES = "cpu";

    public static final String FORCE_PLUGIN_UPDATES2 = "up";

    public static final String SUPPRESS_PLUGIN_UPDATES = "npu";

    public static final String SUPPRESS_PLUGIN_REGISTRY = "npr";

    public static final char CHECKSUM_FAILURE_POLICY = 'C';

    public static final char CHECKSUM_WARNING_POLICY = 'c';

    public static final char ALTERNATE_USER_SETTINGS = 's';

    public static final String ALTERNATE_GLOBAL_SETTINGS = "gs";

    public static final String FAIL_FAST = "ff";

    public static final String FAIL_AT_END = "fae";

    public static final String FAIL_NEVER = "fn";
    
    public static final String RESUME_FROM = "rf";
    
    public static final String PROJECT_LIST = "pl";
    
    public static final String ALSO_MAKE = "am";
    
    public static final String ALSO_MAKE_DEPENDENTS = "amd";

    public static final String ENCRYPT_MASTER_PASSWORD = "emp";

    public static final String ENCRYPT_PASSWORD = "ep";

    public CLIManager()
    {
        options = new Options();

        options.addOption( OptionBuilder.withLongOpt( "file" )
                                        .hasArg()
                                        .withDescription( "Force the use of an alternate POM file." )
                                        .create( ALTERNATE_POM_FILE ) );

        options.addOption( OptionBuilder.withLongOpt( "define" )
                                        .hasArg()
                                        .withDescription( "Define a system property" )
                                        .create( SET_SYSTEM_PROPERTY ) );

        options.addOption( OptionBuilder.withLongOpt( "offline" ).withDescription( "Work offline" ).create( OFFLINE ) );

        options.addOption( OptionBuilder.withLongOpt( "help" )
                                        .withDescription( "Display help information" )
                                        .create( HELP ) );

        options.addOption( OptionBuilder.withLongOpt( "version" )
                                        .withDescription( "Display version information" )
                                        .create( VERSION ) );

        options.addOption( OptionBuilder.withLongOpt( "encrypt-master-password" )
                           .hasArg()
                           .withDescription( "Encrypt master security password" )
                           .create( ENCRYPT_MASTER_PASSWORD ) );
        options.addOption( OptionBuilder.withLongOpt( "encrypt-password" )
                           .hasArg()
                           .withDescription( "Encrypt server password" )
                           .create( ENCRYPT_PASSWORD ) );

        options.addOption( OptionBuilder.withLongOpt( "quiet" )
                                        .withDescription( "Quiet output - only show errors" )
                                        .create( QUIET ) );

        options.addOption( OptionBuilder.withLongOpt( "debug" )
                                        .withDescription( "Produce execution debug output" )
                                        .create( DEBUG ) );

        options.addOption( OptionBuilder.withLongOpt( "errors" )
                                        .withDescription( "Produce execution error messages" )
                                        .create( ERRORS ) );

        options.addOption( OptionBuilder.withLongOpt( "reactor" )
                                        .withDescription( "Dynamically build reactor from subdirectories" )
                                        .create( REACTOR ) );

        options.addOption( OptionBuilder.withLongOpt( "non-recursive" )
                                        .withDescription( "Do not recurse into sub-projects" )
                                        .create( NON_RECURSIVE ) );

        options.addOption( OptionBuilder.withLongOpt( "update-snapshots" )
                                        .withDescription(
                                                          "Forces a check for updated releases and snapshots on remote repositories" )
                                        .create( UPDATE_SNAPSHOTS ) );

        options.addOption( OptionBuilder.withLongOpt( "activate-profiles" )
                                        .withDescription( "Comma-delimited list of profiles to activate" )
                                        .hasArg()
                                        .create( ACTIVATE_PROFILES ) );

        options.addOption( OptionBuilder.withLongOpt( "batch-mode" )
                                        .withDescription( "Run in non-interactive (batch) mode" )
                                        .create( BATCH_MODE ) );

        options.addOption( OptionBuilder.withLongOpt( "check-plugin-updates" )
                                        .withDescription( "Force upToDate check for any relevant registered plugins" )
                                        .create( FORCE_PLUGIN_UPDATES ) );

        options.addOption( OptionBuilder.withLongOpt( "update-plugins" )
                                        .withDescription( "Synonym for " + FORCE_PLUGIN_UPDATES )
                                        .create( FORCE_PLUGIN_UPDATES2 ) );

        options.addOption( OptionBuilder.withLongOpt( "no-plugin-updates" )
                                        .withDescription( "Suppress upToDate check for any relevant registered plugins" )
                                        .create( SUPPRESS_PLUGIN_UPDATES ) );

        options.addOption( OptionBuilder.withLongOpt( "no-plugin-registry" )
                                        .withDescription( "Don't use ~/.m2/plugin-registry.xml for plugin versions" )
                                        .create( SUPPRESS_PLUGIN_REGISTRY ) );

        options.addOption( OptionBuilder.withLongOpt( "strict-checksums" )
                                        .withDescription( "Fail the build if checksums don't match" )
                                        .create( CHECKSUM_FAILURE_POLICY ) );

        options.addOption( OptionBuilder.withLongOpt( "lax-checksums" )
                                        .withDescription( "Warn if checksums don't match" )
                                        .create( CHECKSUM_WARNING_POLICY ) );

        options.addOption( OptionBuilder.withLongOpt( "settings" )
                                        .withDescription( "Alternate path for the user settings file" )
                                        .hasArg()
                                        .create( ALTERNATE_USER_SETTINGS ) );

        options.addOption( OptionBuilder.withLongOpt( "global-settings" )
                                        .withDescription( "Alternate path for the global settings file" )
                                        .hasArg()
                                        .create( ALTERNATE_GLOBAL_SETTINGS ) );

        options.addOption( OptionBuilder.withLongOpt( "fail-fast" )
                                        .withDescription( "Stop at first failure in reactorized builds" )
                                        .create( FAIL_FAST ) );

        options.addOption( OptionBuilder.withLongOpt( "fail-at-end" )
                                        .withDescription(
                                                          "Only fail the build afterwards; allow all non-impacted builds to continue" )
                                        .create( FAIL_AT_END ) );

        options.addOption( OptionBuilder.withLongOpt( "fail-never" )
                                        .withDescription( "NEVER fail the build, regardless of project result" )
                                        .create( FAIL_NEVER ) );

        options.addOption( OptionBuilder.withLongOpt( "show-version" )
                                        .withDescription( "Display version information WITHOUT stopping build" )
                                        .create( SHOW_VERSION ) );

        options.addOption( OptionBuilder.withLongOpt( "resume-from" )
                                        .hasArg()
                                        .withDescription( "Resume reactor from specified project" )
                                        .create( RESUME_FROM ) );

        options.addOption( OptionBuilder.withLongOpt( "projects" )
                                        .withDescription( "Build specified reactor projects instead of all projects" )
                                        .hasArg()
                                        .create( PROJECT_LIST ) );

        options.addOption( OptionBuilder.withLongOpt( "also-make" )
                                        .withDescription(
                                                          "If project list is specified, also build projects required by the list" )
                                        .create( ALSO_MAKE ) );

        options.addOption( OptionBuilder.withLongOpt( "also-make-dependents" )
                                        .withDescription(
                                                          "If project list is specified, also build projects that depend on projects on the list" )
                                        .create( ALSO_MAKE_DEPENDENTS ) );
    }

    public CommandLine parse( String[] args )
        throws ParseException
    {
        // We need to eat any quotes surrounding arguments...
        String[] cleanArgs = cleanArgs( args );

        CommandLineParser parser = new GnuParser();
        return parser.parse( options, cleanArgs );
    }

    private String[] cleanArgs( String[] args )
    {
        List cleaned = new ArrayList();

        StringBuffer currentArg = null;

        for ( int i = 0; i < args.length; i++ )
        {
            String arg = args[i];

//            System.out.println( "Processing raw arg: " + arg );

            boolean addedToBuffer = false;

            if ( arg.startsWith( "\"" ) )
            {
                // if we're in the process of building up another arg, push it and start over.
                // this is for the case: "-Dfoo=bar "-Dfoo2=bar two" (note the first unterminated quote)
                if ( currentArg != null )
                {
//                    System.out.println( "Flushing last arg buffer: \'" + currentArg + "\' to cleaned list." );
                    cleaned.add( currentArg.toString() );
                }

                // start building an argument here.
                currentArg = new StringBuffer( arg.substring( 1 ) );
                addedToBuffer = true;
            }

            // this has to be a separate "if" statement, to capture the case of: "-Dfoo=bar"
            if ( arg.endsWith( "\"" ) )
            {
                String cleanArgPart = arg.substring( 0, arg.length() - 1 );

                // if we're building an argument, keep doing so.
                if ( currentArg != null )
                {
                    // if this is the case of "-Dfoo=bar", then we need to adjust the buffer.
                    if ( addedToBuffer )
                    {
//                        System.out.println( "Adjusting argument already appended to the arg buffer." );
                        currentArg.setLength( currentArg.length() - 1 );
                    }
                    // otherwise, we trim the trailing " and append to the buffer.
                    else
                    {
//                        System.out.println( "Appending arg part: \'" + cleanArgPart + "\' with preceding space to arg buffer." );
                        // TODO: introducing a space here...not sure what else to do but collapse whitespace
                        currentArg.append( ' ' ).append( cleanArgPart );
                    }

//                    System.out.println( "Flushing completed arg buffer: \'" + currentArg + "\' to cleaned list." );

                    // we're done with this argument, so add it.
                    cleaned.add( currentArg.toString() );
                }
                else
                {
//                    System.out.println( "appending cleaned arg: \'" + cleanArgPart + "\' directly to cleaned list." );
                    // this is a simple argument...just add it.
                    cleaned.add( cleanArgPart );
                }

//                System.out.println( "Clearing arg buffer." );
                // the currentArg MUST be finished when this completes.
                currentArg = null;
                continue;
            }

            // if we haven't added this arg to the buffer, and we ARE building an argument
            // buffer, then append it with a preceding space...again, not sure what else to
            // do other than collapse whitespace.
            // NOTE: The case of a trailing quote is handled by nullifying the arg buffer.
            if ( !addedToBuffer )
            {
                // append to the argument we're building, collapsing whitespace to a single space.
                if ( currentArg != null )
                {
//                    System.out.println( "Append unquoted arg part: \'" + arg + "\' to arg buffer." );
                    currentArg.append( ' ' ).append( arg );
                }
                // this is a loner, just add it directly.
                else
                {
//                    System.out.println( "Append unquoted arg part: \'" + arg + "\' directly to cleaned list." );
                    cleaned.add( arg );
                }
            }
        }

        // clean up.
        if ( currentArg != null )
        {
//            System.out.println( "Adding unterminated arg buffer: \'" + currentArg + "\' to cleaned list." );
            cleaned.add( currentArg.toString() );
        }

        int cleanedSz = cleaned.size();
        String[] cleanArgs = null;

        if ( cleanedSz == 0 )
        {
            // if we didn't have any arguments to clean, simply pass the original array through
            cleanArgs = args;
        }
        else
        {
//            System.out.println( "Cleaned argument list:\n" + cleaned );
            cleanArgs = (String[]) cleaned.toArray( new String[cleanedSz] );
        }

        return cleanArgs;
    }

    public void displayHelp()
    {
        System.out.println();

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "mvn [options] [<goal(s)>] [<phase(s)>]", "\nOptions:", options, "\n" );
    }
    
}
