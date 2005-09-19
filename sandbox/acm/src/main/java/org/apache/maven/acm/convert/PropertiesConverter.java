package org.apache.maven.acm.convert;

import org.apache.maven.acm.model.Model;
import org.apache.maven.acm.model.Environment;

import java.io.FileInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Properties;
import java.util.Iterator;

/**
 * We want to take a set of properties files, where one properties file represents
 * the configuration for a particular environment, and combine them into a
 * single application configuration management model.
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id:$
 */
public class PropertiesConverter
{
    public Model convert( String directory )
        throws Exception
    {
        Model m = new Model();

        File f = new File( directory );

        File[] files = f.listFiles( new PropertiesFilter() );

        for ( int i = 0; i < files.length; i++ )
        {
            File p = files[i];

            String env = p.getName();

            env = env.substring( 0, env.indexOf( "." ) );

            processProperties( env, p.getPath(), m );
        }

        return m;
    }

    class PropertiesFilter
        implements FilenameFilter
    {
        public boolean accept( File directory, String filename )
        {
            if ( filename.endsWith( ".properties" ) )
            {
                return true;
            }

            return false;
        }
    }

    protected void processProperties( String env, String properties, Model model )
        throws Exception
    {
        System.out.println( "Processing " + env );

        Environment e = new Environment();

        e.setId( env );

        e.setDescription( "Environment for " + env );

        FileInputStream is = new FileInputStream( properties );

        Properties p = new Properties();

        p.load( is );

        for ( Iterator i = p.keySet().iterator(); i.hasNext(); )
        {
            String propertyKey = (String) i.next();

            model.addPropertyKey( propertyKey );

            String value = p.getProperty( propertyKey );

            if ( value == null )
            {
                value = "NOT_SET for " + env;
            }

            e.addProperty( propertyKey, value );
        }

        model.addEnvironment( e );
    }
}
