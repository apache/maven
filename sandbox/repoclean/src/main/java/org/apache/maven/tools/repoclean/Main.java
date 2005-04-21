package org.apache.maven.tools.repoclean;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.codehaus.classworlds.ClassWorld;
import org.codehaus.plexus.embed.Embedder;
import org.codehaus.plexus.util.IOUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author jdcasey
 */
public class Main
{
    
    public static final String FORCE_ARG = "--force";

    public static void main( String[] args )
    {
        boolean force = false;
        String configFile = null;
        
        if ( args.length < 1 )
        {
            printUsage();
            System.exit( 0 );
        }
        else if ( "-h".equals( args[0].toLowerCase() ) )
        {
            printHelp();
            System.exit( 0 );
        }
        else if ( "-template".equals( args[0] ) )
        {
            printTemplate();
            System.exit( 0 );
        }
        // up the ante, and let's try to see if there's a --force option.
        else if ( args.length == 2 )
        {
            if(FORCE_ARG.equals(args[0]))
            {
                force = true;
                configFile = args[1];
            }
            else if(FORCE_ARG.equals(args[1]))
            {
                force = true;
                configFile = args[0];
            }
            else
            {
                System.out.println("Invalid argument list: \'" + args[0] + " " + args[1]);
                printUsage();
                System.exit(1);
            }
        }
        else
        {
            configFile = args[0];
        }

        try
        {
            RepositoryCleanerConfiguration config = buildConfig( configFile );
            
            config.setForce(force);

            launch( config );
            
            System.exit( 0 );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            
            System.exit( 1 );
        }
    }

    public static void launch( RepositoryCleanerConfiguration config ) throws Exception
    {
        Embedder embedder = new Embedder();
        embedder.start( new ClassWorld() );

        RepositoryCleaner cleaner = null;
        try
        {
            cleaner = (RepositoryCleaner) embedder.lookup( RepositoryCleaner.ROLE );

            cleaner.cleanRepository( config );
        }
        finally
        {
            if ( cleaner != null )
            {
                embedder.release( cleaner );
            }
        }
    }

    private static RepositoryCleanerConfiguration buildConfig( String configPath )
        throws IOException
    {
        Properties props = new Properties();
        FileInputStream input = null;
        try
        {
            input = new FileInputStream( configPath );
            props.load( input );
        }
        finally
        {
            IOUtil.close( input );
        }

        RepositoryCleanerConfiguration config = new RepositoryCleanerConfiguration();
        config.setSourceRepositoryPath( props.getProperty( "sourceRepositoryPath" ) );
        config.setSourceRepositoryLayout( props.getProperty( "sourceRepositoryLayout", "legacy" ) );
        config.setSourcePomVersion( props.getProperty( "sourcePomVersion", "v3" ) );
        config.setTargetRepositoryPath( props.getProperty( "targetRepositoryPath" ) );
        config.setTargetRepositoryLayout( props.getProperty( "targetRepositoryLayout", "default" ) );
        config.setReportsPath( props.getProperty( "reportsPath" ) );
        config.setBlacklistedPatterns( props.getProperty( "blacklistedPatterns" ) );
        config.setReportOnly( Boolean.valueOf( props.getProperty( "reportOnly" ) ).booleanValue() );

        config.setMailErrorReport( Boolean.valueOf( props.getProperty( "errorReport.mailOnError", "false") ).booleanValue() );
        config.setErrorReportFromAddress( props.getProperty( "errorReport.fromAddress" ) );
        config.setErrorReportFromName( props.getProperty( "errorReport.fromName" ) );
        config.setErrorReportSubject( props.getProperty( "errorReport.subject" ) );
        config.setErrorReportToAddress( props.getProperty( "errorReport.toAddress" ) );
        config.setErrorReportToName( props.getProperty( "errorReport.toName" ) );

        return config;
    }

    private static void printHelp()
    {
        System.out.println( "repoclean: Repository Cleaner/Converter.\n\n"
            + "Usage: repoclean -h|-template|<configuration-properties-file>\n\n"
            + "---------------------------------------------------------------------------\n"
            + "  Use the '-template' option to see sample configuration file options.\n"
            + "\n" );
    }

    private static void printTemplate()
    {
        System.out.println( "# ---------------------------------------------------------------------------\n"
            + "# repoclean: Repository Cleaner/Converter.\n" 
            + "# This configuration auto-generated on: " + new java.util.Date() + "\n"
            + "# ---------------------------------------------------------------------------\n\n"
            + "# [REQUIRED OPTIONS]\n" 
            + "sourceRepositoryPath=/path/to/repository/root\n"
            + "targetRepositoryPath=/path/to/repository/root\n" 
            + "reportsPath=/path/to/reports/directory\n"
            + "reportOnly=[true|false]\n"
            + "\n" 
            + "errorReport.mailOnError=[true|false]\n"
            + "errorReport.fromAddress=admin@somewhere.com\n"
            + "errorReport.fromName=Admin\n"
            + "errorReport.subject=[REPOCLEAN] Error!\n"
            + "errorReport.toAddress=dev@somewhere.com\n"
            + "errorReport.toName=Developers List\n"
            + "\n"
            + "# [DEFAULT VALUE: legacy]\n"
            + "#sourceRepositoryLayout=[legacy|default]\n\n" 
            + "# [DEFAULT VALUE: v3]\n" 
            + "#sourcePomType=[v3|v4]\n\n"
            + "# [DEFAULT VALUE: default]\n" 
            + "#targetRepositoryLayout=[legacy|default]\n" 
            + "# [DEFAULT VALUE: localhost]\n"
            + "#errorReport.smtpHost=<hostname>\n"
            + "\n" );
    }

    private static void printUsage()
    {
        System.out.println( "Required input is missing.\n\n" + "Usage:\n"
            + "--------------------------------------------------\n\n"
            + "repoclean -h|-template|<configuration-properties-file>\n" );
    }

}