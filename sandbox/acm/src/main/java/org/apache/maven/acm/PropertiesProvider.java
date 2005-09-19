package org.apache.maven.acm;

import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id:$
 */
public class PropertiesProvider
    extends AbstractProvider
{
    private ClassLoader classLoader;

    private static final String EXTENSION = ".properties";

    public PropertiesProvider()
    {
        this( Thread.currentThread().getContextClassLoader() );
    }

    public PropertiesProvider( ClassLoader classLoader )
    {
        this.classLoader = classLoader;
    }

    public String getParameter( String systemId, String key )
        throws SystemProviderSourceException
    {
        InputStream is = classLoader.getResourceAsStream( systemId + EXTENSION );

        Properties p = new Properties();

        try
        {
            p.load( is );
        }
        catch ( IOException e )
        {
            throw new SystemProviderSourceException( "Cannot find the source for parameters: ", e  );
        }

        return p.getProperty( key );
    }
}
