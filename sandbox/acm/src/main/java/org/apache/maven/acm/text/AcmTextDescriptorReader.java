package org.apache.maven.acm.text;

import org.apache.maven.acm.model.Environment;
import org.apache.maven.acm.model.Model;
import org.apache.maven.acm.StringInputStream;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.Properties;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id: SinkDescriptorReader.java,v 1.1 2004/09/22 00:01:42 jvanzyl Exp $
 */
public class AcmTextDescriptorReader
{
    public Model read( Reader reader )
        throws Exception
    {
        Model model = new Model();

        BufferedReader br = new BufferedReader( reader );

        String line;

        String propertyKey = null;

        StringBuffer propertiesText = new StringBuffer();

        while ( ( line = br.readLine() ) != null )
        {
            if ( line.startsWith( "//" ) || line.startsWith( "{" ) || line.trim().length() == 0 )
            {
                continue;
            }
            else if ( line.startsWith( "+" ) )
            {
                // Process an environment definition

                Environment e = new Environment();

                String environment = line.substring( 1 ).trim();

                int i = environment.indexOf( "=" );

                String id = environment.substring( 0, i - 1 ).trim();

                String description = environment.substring( i + 1 ).trim();

                e.setId( id );

                e.setDescription( description );

                model.addEnvironment( e );
            }
            else if ( line.startsWith( "*" ) )
            {
                propertyKey = line.substring( 1 ).trim();

                model.addPropertyKey( propertyKey );

                if ( propertyKey.endsWith( "{" ) )
                {
                    propertyKey = propertyKey.substring( 0, propertyKey.length() - 1 ).trim();
                }
            }
            else if ( line.endsWith( "}" ) )
            {
                // This is what we're parsing:
                //
                // dev = devValue
                // test = testValue
                // qa = qaValue
                //

                Properties p = new Properties();

                p.load( new StringInputStream( propertiesText.toString() ) );

                processProperties( model, propertyKey, p );

                propertiesText = new StringBuffer();
            }
            else
            {
                propertiesText.append( line ).append( "\n" );
            }
        }

        return model;
    }

    private void processProperties( Model model, String propertyKey, Properties properties )
    {
        // Iterate though the properties and assign the value for the property key
        // for each environment specified.
        //
        // dev = devValue
        // test = testValue
        // qa = qaValue
        //
        // So for a given propertyKey we need to assign values for the dev, test, and qa
        // environments.

        for ( Iterator i = properties.keySet().iterator(); i.hasNext(); )
        {
            String environmentKey = (String) i.next();

            Environment environment = model.getEnvironment( environmentKey );

            environment.addProperty( propertyKey, properties.getProperty( environmentKey ) );
        }
    }
}
