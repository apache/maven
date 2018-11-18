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
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

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

    public static final char DEBUG = 'X';

    public static final char ERRORS = 'e';

    public static final char HELP = 'h';

    public static final char VERSION = 'v';

    public static final char SHOW_VERSION = 'V';

    public static final char NON_RECURSIVE = 'N';

    public static final char UPDATE_SNAPSHOTS = 'U';

    public static final char ACTIVATE_PROFILES = 'P';

    public static final String SUPRESS_SNAPSHOT_UPDATES = "nsu";

    public static final char CHECKSUM_FAILURE_POLICY = 'C';

    public static final char CHECKSUM_WARNING_POLICY = 'c';

    public static final char ALTERNATE_USER_SETTINGS = 's';

    public static final String ALTERNATE_GLOBAL_SETTINGS = "gs";

    public static final char ALTERNATE_USER_TOOLCHAINS = 't';

    public static final String ALTERNATE_GLOBAL_TOOLCHAINS = "gt";

    public static final String FAIL_FAST = "ff";

    public static final String FAIL_AT_END = "fae";

    public static final String FAIL_NEVER = "fn";

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

    protected Options options;

    @SuppressWarnings( { "static-access", "checkstyle:linelength" } )
    public CLIManager()
    {
        options = new Options();
        options.addOption( OptionBuilder.withLongOpt( "help" ).withDescription( "Display help information" ).create( HELP ) );
        options.addOption( OptionBuilder.withLongOpt( "file" ).hasArg().withDescription( "Force the use of an alternate POM file (or directory with pom.xml)" ).create( ALTERNATE_POM_FILE ) );
        options.addOption( OptionBuilder.withLongOpt( "define" ).hasArg().withDescription( "Define a system property" ).create( SET_SYSTEM_PROPERTY ) );
        options.addOption( OptionBuilder.withLongOpt( "offline" ).withDescription( "Work offline" ).create( OFFLINE ) );
        options.addOption( OptionBuilder.withLongOpt( "version" ).withDescription( "Display version information" ).create( VERSION ) );
        options.addOption( OptionBuilder.withLongOpt( "quiet" ).withDescription( "Quiet output - only show errors" ).create( QUIET ) );
        options.addOption( OptionBuilder.withLongOpt( "debug" ).withDescription( "Produce execution debug output" ).create( DEBUG ) );
        options.addOption( OptionBuilder.withLongOpt( "errors" ).withDescription( "Produce execution error messages" ).create( ERRORS ) );
        options.addOption( OptionBuilder.withLongOpt( "non-recursive" ).withDescription( "Do not recurse into sub-projects" ).create( NON_RECURSIVE ) );
        options.addOption( OptionBuilder.withLongOpt( "update-snapshots" ).withDescription( "Forces a check for missing releases and updated snapshots on remote repositories" ).create( UPDATE_SNAPSHOTS ) );
        options.addOption( OptionBuilder.withLongOpt( "activate-profiles" ).withDescription( "Comma-delimited list of profiles to activate" ).hasArg().create( ACTIVATE_PROFILES ) );
        options.addOption( OptionBuilder.withLongOpt( "batch-mode" ).withDescription( "Run in non-interactive (batch) mode (disables output color)" ).create( BATCH_MODE ) );
        options.addOption( OptionBuilder.withLongOpt( "no-snapshot-updates" ).withDescription( "Suppress SNAPSHOT updates" ).create( SUPRESS_SNAPSHOT_UPDATES ) );
        options.addOption( OptionBuilder.withLongOpt( "strict-checksums" ).withDescription( "Fail the build if checksums don't match" ).create( CHECKSUM_FAILURE_POLICY ) );
        options.addOption( OptionBuilder.withLongOpt( "lax-checksums" ).withDescription( "Warn if checksums don't match" ).create( CHECKSUM_WARNING_POLICY ) );
        options.addOption( OptionBuilder.withLongOpt( "settings" ).withDescription( "Alternate path for the user settings file" ).hasArg().create( ALTERNATE_USER_SETTINGS ) );
        options.addOption( OptionBuilder.withLongOpt( "global-settings" ).withDescription( "Alternate path for the global settings file" ).hasArg().create( ALTERNATE_GLOBAL_SETTINGS ) );
        options.addOption( OptionBuilder.withLongOpt( "toolchains" ).withDescription( "Alternate path for the user toolchains file" ).hasArg().create( ALTERNATE_USER_TOOLCHAINS ) );
        options.addOption( OptionBuilder.withLongOpt( "global-toolchains" ).withDescription( "Alternate path for the global toolchains file" ).hasArg().create( ALTERNATE_GLOBAL_TOOLCHAINS ) );
        options.addOption( OptionBuilder.withLongOpt( "fail-fast" ).withDescription( "Stop at first failure in reactorized builds" ).create( FAIL_FAST ) );
        options.addOption( OptionBuilder.withLongOpt( "fail-at-end" ).withDescription( "Only fail the build afterwards; allow all non-impacted builds to continue" ).create( FAIL_AT_END ) );
        options.addOption( OptionBuilder.withLongOpt( "fail-never" ).withDescription( "NEVER fail the build, regardless of project result" ).create( FAIL_NEVER ) );
        options.addOption( OptionBuilder.withLongOpt( "resume-from" ).hasArg().withDescription( "Resume reactor from specified project" ).create( RESUME_FROM ) );
        options.addOption( OptionBuilder.withLongOpt( "projects" ).withDescription( "Comma-delimited list of specified reactor projects to build instead of all projects. A project can be specified by [groupId]:artifactId or by its relative path" ).hasArg().create( PROJECT_LIST ) );
        options.addOption( OptionBuilder.withLongOpt( "also-make" ).withDescription( "If project list is specified, also build projects required by the list" ).create( ALSO_MAKE ) );
        options.addOption( OptionBuilder.withLongOpt( "also-make-dependents" ).withDescription( "If project list is specified, also build projects that depend on projects on the list" ).create( ALSO_MAKE_DEPENDENTS ) );
        options.addOption( OptionBuilder.withLongOpt( "log-file" ).hasArg().withDescription( "Log file where all build output will go (disables output color)" ).create( LOG_FILE ) );
        options.addOption( OptionBuilder.withLongOpt( "show-version" ).withDescription( "Display version information WITHOUT stopping build" ).create( SHOW_VERSION ) );
        options.addOption( OptionBuilder.withLongOpt( "encrypt-master-password" ).hasOptionalArg().withDescription( "Encrypt master security password" ).create( ENCRYPT_MASTER_PASSWORD ) );
        options.addOption( OptionBuilder.withLongOpt( "encrypt-password" ).hasOptionalArg().withDescription( "Encrypt server password" ).create( ENCRYPT_PASSWORD ) );
        options.addOption( OptionBuilder.withLongOpt( "threads" ).hasArg().withDescription( "Thread count, for instance 2.0C where C is core multiplied" ).create( THREADS ) );
        options.addOption( OptionBuilder.withLongOpt( "legacy-local-repository" ).withDescription( "Use Maven 2 Legacy Local Repository behaviour, ie no use of _remote.repositories. Can also be activated by using -Dmaven.legacyLocalRepo=true" ).create( LEGACY_LOCAL_REPOSITORY ) );
        options.addOption( OptionBuilder.withLongOpt( "builder" ).hasArg().withDescription( "The id of the build strategy to use" ).create( BUILDER ) );

        // Adding this back in for compatibility with the verifier that hard codes this option.
        options.addOption( OptionBuilder.withLongOpt( "no-plugin-registry" ).withDescription( "Ineffective, only kept for backward compatibility" ).create( "npr" ) );
        options.addOption( OptionBuilder.withLongOpt( "check-plugin-updates" ).withDescription( "Ineffective, only kept for backward compatibility" ).create( "cpu" ) );
        options.addOption( OptionBuilder.withLongOpt( "update-plugins" ).withDescription( "Ineffective, only kept for backward compatibility" ).create( "up" ) );
        options.addOption( OptionBuilder.withLongOpt( "no-plugin-updates" ).withDescription( "Ineffective, only kept for backward compatibility" ).create( "npu" ) );
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

        formatter.printHelp( pw, HelpFormatter.DEFAULT_WIDTH, "mvn [options] [<goal(s)>] [<phase(s)>]", "\nOptions:",
                             options, HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, "\n", false );

        pw.flush();
    }
}
