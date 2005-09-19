/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.acm.convert;

import org.apache.maven.acm.util.InterpolationFilterWriter;
import org.apache.maven.acm.util.MapInterpolationHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Iterator;

/**
 * This tool works in the case where you have a set of properties files that
 * control your application configuration and a set of environmental properties files
 * that are merged against your properties files used for your application
 * configuration.
 * <p/>
 * So you might have:
 * <p/>
 * brioreport.properties
 * config.properties
 * portal.properties
 * <p/>
 * These control your properties files for your application configurations.
 * Then you might have the following:
 * <p/>
 * dev.properties
 * test.properties
 * qa.properties
 * prod.properties
 * <p/>
 * These are properties for the environment the application is
 * running in.
 * <p/>
 * Our goal here is to unify these into a coherent ACM so that
 * we can manage these.
 * <p/>
 * In this particular case the environmental properties are used for
 * swizzling the application properties above in addition to being
 * used to swizzle other resources like weblogic config.xml files.
 * <p/>
 * So first we will take all the application configuration properties
 * and concatenate them together
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id:$
 */
public class PropertiesMunger
{
    private static String APP = "application.props";

    public void munge( File applicationSourceDirectory,
                       File environmentSourceDirectory,
                       File outputDirectory )
        throws Exception
    {
        if ( !outputDirectory.exists() )
        {
            outputDirectory.mkdirs();
        }

        FileScanner scanner = new FileScanner();

        File[] appProperties = scanner.scan( applicationSourceDirectory, "properties" );

        File[] envProperties = scanner.scan( environmentSourceDirectory, "properties" );

        // ----------------------------------------------------------------------
        // Combine all the application properties into a single file
        // ----------------------------------------------------------------------

        Properties app = new Properties();

        Map m = new HashMap();

        for ( int i = 0; i < appProperties.length; i++ )
        {
            Properties p = new Properties();

            p.load( new FileInputStream( appProperties[i] ) );

            app.putAll( p );
        }


        File aggregate = new File( outputDirectory, APP );

        FileOutputStream os = new FileOutputStream( aggregate );

        app.store( os, APP );

        os.close();

        // ----------------------------------------------------------------------
        // Now write out the template for the application configurations
        // ----------------------------------------------------------------------

        for ( int i = 0; i < envProperties.length; i++ )
        {
            String envName = envProperties[i].getName();

            envName = envName.substring( 0, envName.indexOf( "." ) );

            Properties p = new Properties();

            p.load( new FileInputStream( envProperties[i] ) );

            // ------------------------------------------------------------------------
            // Now use these properties and merge them with the application.properties
            // to produce a properties file targeted at a particular environment.
            // ------------------------------------------------------------------------

            Reader reader = new FileReader( aggregate );

            File output = new File( outputDirectory, envName + ".properties" );

            MapInterpolationHandler handler = new MapInterpolationHandler( p );

            InterpolationFilterWriter writer = new InterpolationFilterWriter( new FileWriter( output ), handler );

            copy( reader, writer );

            reader.close();

            writer.close();

            // ------------------------------------------------------------------------
            // Now look at the difference in keys between the application configuration
            // and the environment configuration and push the difference into
            // application configuration so we have a self contained set of
            // properties for an environment. We are basically making one
            // configuration for an environment.
            // ------------------------------------------------------------------------

            Properties appProps = new Properties();

            FileInputStream is = new FileInputStream( output );

            appProps.load( is );

            is.close();

            for ( Iterator j = p.keySet().iterator(); j.hasNext(); )
            {
                String key = (String) j.next();

                if ( ! appProps.containsKey( key ) )
                {
                    appProps.setProperty( key, p.getProperty( key ) );
                }
            }

            os = new FileOutputStream( output );

            appProps.store( os, "props for " + envName );

            os.close();
        }
    }

    public static void copy( Reader input, Writer output )
        throws IOException
    {
        char[] buffer = new char[1024 * 4];

        int n = 0;

        while ( -1 != ( n = input.read( buffer ) ) )
        {
            output.write( buffer, 0, n );
        }
    }

    public static void main( String[] args )
        throws Exception
    {
        File appSource = new File( args[0] );

        File envSource = new File( args[1] );

        File output = new File( args[2] );

        PropertiesMunger munger = new PropertiesMunger();

        munger.munge( appSource, envSource, output );
    }
}
