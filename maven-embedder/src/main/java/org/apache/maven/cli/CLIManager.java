package org.apache.maven.cli;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.PrintStream;
import java.io.PrintWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.maven.shared.utils.logging.MessageUtils;

/**
 * @author Jason van Zyl
 */
public class CLIManager
{
    public static final char ALTERNATE_POM_FILE = 'f';

    public static final char BATCH_MODE = 'B';

    public static final char SET_SYSTEM_PROPERTY = 'D';

    public static final char OFFLINE = 'o';

    public static final char QUIET = 'q';

    public static final char VERBOSE = 'X';

    public static final char ERRORS = 'e';

    public static final char HELP = 'h';

    public static final char VERSION = 'v';

    public static final char SHOW_VERSION = 'V';

    public static final char NON_RECURSIVE = 'N';

    public static final char UPDATE_SNAPSHOTS = 'U';

    public static final char ACTIVATE_PROFILES = 'P';

    public static final String SUPPRESS_SNAPSHOT_UPDATES = "nsu";

    public static final char CHECKSUM_FAILURE_POLICY = 'C';

    public static final char CHECKSUM_WARNING_POLICY = 'c';

    public static final char ALTERNATE_USER_SETTINGS = 's';

    public static final String ALTERNATE_GLOBAL_SETTINGS = "gs";

    public static final char ALTERNATE_USER_TOOLCHAINS = 't';

    public static final String ALTERNATE_GLOBAL_TOOLCHAINS = "gt";

    public static final String FAIL_FAST = "ff";

    public static final String FAIL_ON_SEVERITY = "fos";

    public static final String FAIL_AT_END = "fae";

    public static final String FAIL_NEVER = "fn";

    public static final String RESUME = "r";

    public static final String RESUME_FROM = "rf";

    public static final String PROJECT_LIST = "pl";

    public static final String ALSO_MAKE = "am";

    public static final String ALSO_MAKE_DEPENDENTS = "amd";

    public static final String LOG_FILE = "l";

    public static final String ENCRYPT_MASTER_PASSWORD = "emp";

    public static final String ENCRYPT_PASSWORD = "ep";

    public static final String THREADS = "T";

    public static final String LEGACY_LOCAL_REPOSITORY = "llr";

    public static final String BUILDER = "b";

    public static final String NO_TRANSFER_PROGRESS = "ntp";

    public static final String COLOR = "color";

    /** This option is deprecated and may be repurposed as Java debug in a future version.
     * Use {@code -X/--verbose} instead. */
    @Deprecated
    public static final String DEBUG = "debug";

    protected Options options;

    @SuppressWarnings( "checkstyle:linelength" )
    public CLIManager()
    {
        options = new Options();
        options.addOption( Option.builder( Character.toString( HELP ) ).longOpt( "help" ).desc( "Display help information" ).build() );
        options.addOption( Option.builder( Character.toString( ALTERNATE_POM_FILE ) ).longOpt( "file" ).hasArg().desc( "Force the use of an alternate POM file (or directory with pom.xml)" ).build() );
        options.addOption( Option.builder( Character.toString( SET_SYSTEM_PROPERTY ) ).longOpt( "define" ).hasArg().desc( "Define a system property" ).build() );
        options.addOption( Option.builder( Character.toString( OFFLINE ) ).longOpt( "offline" ).desc( "Work offline" ).build() );
        options.addOption( Option.builder( Character.toString( VERSION ) ).longOpt( "version" ).desc( "Display version information" ).build() );
        options.addOption( Option.builder( Character.toString( QUIET ) ).longOpt( "quiet" ).desc( "Quiet output - only show errors" ).build() );
        options.addOption( Option.builder( Character.toString( VERBOSE ) ).longOpt( "verbose" ).longOpt( "verbose" ).desc( "Produce execution verbose output" ).build() );
        options.addOption( Option.builder( Character.toString( ERRORS ) ).longOpt( "errors" ).desc( "Produce execution error messages" ).build() );
        options.addOption( Option.builder( Character.toString( NON_RECURSIVE ) ).longOpt( "non-recursive" ).desc( "Do not recurse into sub-projects. When used together with -pl, do not recurse into sub-projects of selected aggregators" ).build() );
        options.addOption( Option.builder( Character.toString( UPDATE_SNAPSHOTS ) ).longOpt( "update-snapshots" ).desc( "Forces a check for missing releases and updated snapshots on remote repositories" ).build() );
        options.addOption( Option.builder( Character.toString( ACTIVATE_PROFILES ) ).longOpt( "activate-profiles" ).desc( "Comma-delimited list of profiles to activate. Prefixing a profile with ! excludes it, and ? marks it as optional" ).hasArg().build() );
        options.addOption( Option.builder( Character.toString( BATCH_MODE ) ).longOpt( "batch-mode" ).desc( "Run in non-interactive (batch) mode (disables output color)" ).build() );
        options.addOption( Option.builder( SUPPRESS_SNAPSHOT_UPDATES ).longOpt( "no-snapshot-updates" ).desc( "Suppress SNAPSHOT updates" ).build() );
        options.addOption( Option.builder( Character.toString( CHECKSUM_FAILURE_POLICY ) ).longOpt( "strict-checksums" ).desc( "Fail the build if checksums don't match" ).build() );
        options.addOption( Option.builder( Character.toString( CHECKSUM_WARNING_POLICY ) ).longOpt( "lax-checksums" ).desc( "Warn if checksums don't match" ).build() );
        options.addOption( Option.builder( Character.toString( ALTERNATE_USER_SETTINGS ) ).longOpt( "settings" ).desc( "Alternate path for the user settings file" ).hasArg().build() );
        options.addOption( Option.builder( ALTERNATE_GLOBAL_SETTINGS ).longOpt( "global-settings" ).desc( "Alternate path for the global settings file" ).hasArg().build() );
        options.addOption( Option.builder( Character.toString( ALTERNATE_USER_TOOLCHAINS ) ).longOpt( "toolchains" ).desc( "Alternate path for the user toolchains file" ).hasArg().build() );
        options.addOption( Option.builder( ALTERNATE_GLOBAL_TOOLCHAINS ).longOpt( "global-toolchains" ).desc( "Alternate path for the global toolchains file" ).hasArg().build() );
        options.addOption( Option.builder( FAIL_ON_SEVERITY ).longOpt( "fail-on-severity" ).desc( "Configure which severity of logging should cause the build to fail" ).hasArgs().build() );
        options.addOption( Option.builder( FAIL_FAST ).longOpt( "fail-fast" ).desc( "Stop at first failure in reactorized builds" ).build() );
        options.addOption( Option.builder( FAIL_AT_END ).longOpt( "fail-at-end" ).desc( "Only fail the build afterwards; allow all non-impacted builds to continue" ).build() );
        options.addOption( Option.builder( FAIL_NEVER ).longOpt( "fail-never" ).desc( "NEVER fail the build, regardless of project result" ).build() );
        options.addOption( Option.builder( RESUME ).longOpt( "resume" ).desc( "Resume reactor from the last failed project, using the resume.properties file in the build directory" ).build() );
        options.addOption( Option.builder( RESUME_FROM ).longOpt( "resume-from" ).hasArg().desc( "Resume reactor from specified project" ).build() );
        options.addOption( Option.builder( PROJECT_LIST ).longOpt( "projects" ).desc( "Comma-delimited list of specified reactor projects to build instead of all projects. A project can be specified by [groupId]:artifactId or by its relative path. Prefixing a project with ! excludes it, and ? marks it as optional" ).hasArg().build() );
        options.addOption( Option.builder( ALSO_MAKE ).longOpt( "also-make" ).desc( "If project list is specified, also build projects required by the list" ).build() );
        options.addOption( Option.builder( ALSO_MAKE_DEPENDENTS ).longOpt( "also-make-dependents" ).desc( "If project list is specified, also build projects that depend on projects on the list" ).build() );
        options.addOption( Option.builder( LOG_FILE ).longOpt( "log-file" ).hasArg().desc( "Log file where all build output will go (disables output color)" ).build() );
        options.addOption( Option.builder( Character.toString( SHOW_VERSION ) ).longOpt( "show-version" ).desc( "Display version information WITHOUT stopping build" ).build() );
        options.addOption( Option.builder( ENCRYPT_MASTER_PASSWORD ).longOpt( "encrypt-master-password" ).hasArg().optionalArg( true ).desc( "Encrypt master security password" ).build() );
        options.addOption( Option.builder( ENCRYPT_PASSWORD ).longOpt( "encrypt-password" ).hasArg().optionalArg( true ).desc( "Encrypt server password" ).build() );
        options.addOption( Option.builder( THREADS ).longOpt( "threads" ).hasArg().desc( "Thread count, for instance 2.0C where C is core multiplied" ).build() );
        options.addOption( Option.builder( LEGACY_LOCAL_REPOSITORY ).longOpt( "legacy-local-repository" ).desc( "Use Maven 2 Legacy Local Repository behaviour, ie no use of _remote.repositories. Can also be activated by using -Dmaven.legacyLocalRepo=true" ).build() );
        options.addOption( Option.builder( BUILDER ).longOpt( "builder" ).hasArg().desc( "The id of the build strategy to use" ).build() );
        options.addOption( Option.builder( NO_TRANSFER_PROGRESS ).longOpt( "no-transfer-progress" ).desc( "Do not display transfer progress when downloading or uploading" ).build() );
        options.addOption( Option.builder().longOpt( COLOR ).hasArg().optionalArg( true ).desc( "Defines the color mode of the output. Supported are 'auto', 'always', 'never'." ).build() );

        // Deprecated
        options.addOption( Option.builder().longOpt( DEBUG ).desc( "Produce execution verbose output (deprecated; only kept for backward compatibility)" ).build() );
    }

    public CommandLine parse( String[] args )
        throws ParseException
    {
        // We need to eat any quotes surrounding arguments...
        String[] cleanArgs = CleanArgument.cleanArgs( args );

        CommandLineParser parser = new GnuParser();

        return parser.parse( options, cleanArgs );
    }

    public void displayHelp( PrintStream stdout )
    {
        stdout.println();

        PrintWriter pw = new PrintWriter( stdout );

        HelpFormatter formatter = new HelpFormatter();

        int width = MessageUtils.getTerminalWidth();
        if ( width <= 0 )
        {
            width = HelpFormatter.DEFAULT_WIDTH;
        }

        formatter.printHelp( pw, width, "mvn [options] [<goal(s)>] [<phase(s)>]",
                             System.lineSeparator() + "Options:", options,
                             HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD,
                             System.lineSeparator(), false );

        pw.flush();
    }
}
