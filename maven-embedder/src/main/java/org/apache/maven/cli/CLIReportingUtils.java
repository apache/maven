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

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.Os;
import org.slf4j.Logger;

/**
 * Utility class used to report errors, statistics, application version info, etc.
 *
 * @author jdcasey
 *
 */
public final class CLIReportingUtils
{

    public static final long MB = 1024 * 1024;

    public static final int MS_PER_SEC = 1000;

    public static final int SEC_PER_MIN = 60;
    
    public static final String BUILD_VERSION_PROPERTY = "version";

    public static String showVersion()
    {
        final String LS = System.getProperty( "line.separator" );
        Properties properties = getBuildProperties();
        StringBuffer version = new StringBuffer();
        version.append( createMavenVersionString( properties ) ).append( LS );
        version.append( reduce( properties.getProperty( "distributionShortName" ) + " home: " + System.getProperty( "maven.home", "<unknown maven home>" ) ) ).append( LS );
        version.append( "Java version: " + System.getProperty( "java.version", "<unknown java version>" ) + ", vendor: " + System.getProperty( "java.vendor", "<unknown vendor>" ) ).append( LS );
        version.append( "Java home: " + System.getProperty( "java.home", "<unknown java home>" ) ).append( LS );
        version.append( "Default locale: " + Locale.getDefault() + ", platform encoding: " + System.getProperty( "file.encoding", "<unknown encoding>" ) ).append( LS );
        version.append( "OS name: \"" + Os.OS_NAME + "\", version: \"" + Os.OS_VERSION + "\", arch: \"" + Os.OS_ARCH + "\", family: \"" + Os.OS_FAMILY + "\"" );
        return version.toString();
    }

    /**
     * Create a human readable string containing the Maven version, buildnumber, and time of build
     * 
     * @param buildProperties The build properties
     * @return Readable build info
     */
    static String createMavenVersionString( Properties buildProperties )
    {
        String timestamp = reduce( buildProperties.getProperty( "timestamp" ) );
        String version = reduce( buildProperties.getProperty( BUILD_VERSION_PROPERTY ) );
        String rev = reduce( buildProperties.getProperty( "buildNumber" ) );
        String distributionName = reduce( buildProperties.getProperty( "distributionName" ) );

        String msg = distributionName + " ";
        msg += ( version != null ? version : "<version unknown>" );
        if ( rev != null || timestamp != null )
        {
            msg += " (";
            msg += ( rev != null ? "r" + rev : "" );
            if ( timestamp != null )
            {
                SimpleDateFormat fmt = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ssZ" );
                String ts = fmt.format( new Date( Long.valueOf( timestamp ).longValue() ) );
                msg += ( rev != null ? "; " : "" ) + ts;
            }
            msg += ")";
        }
        return msg;
    }

    private static String reduce( String s )
    {
        return ( s != null ? ( s.startsWith( "${" ) && s.endsWith( "}" ) ? null : s ) : null );
    }

    static Properties getBuildProperties()
    {
        Properties properties = new Properties();
        InputStream resourceAsStream = null;
        try
        {
            resourceAsStream = MavenCli.class.getResourceAsStream( "/org/apache/maven/messages/build.properties" );

            if ( resourceAsStream != null )
            {
                properties.load( resourceAsStream );
            }
        }
        catch ( IOException e )
        {
            System.err.println( "Unable determine version from JAR file: " + e.getMessage() );
        }
        finally
        {
            IOUtil.close( resourceAsStream );
        }

        return properties;
    }

    public static void showError( Logger logger, String message, Throwable e, boolean showStackTrace )
    {
        if ( showStackTrace )
        {
            logger.error( message, e );
        }
        else
        {
            logger.error( message );

            if ( e != null )
            {
                logger.error( e.getMessage() );

                for ( Throwable cause = e.getCause(); cause != null; cause = cause.getCause() )
                {
                    logger.error( "Caused by: " + cause.getMessage() );
                }
            }
        }
    }

}
