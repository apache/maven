package org.apache.maven.lifecycle;

import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.io.xpp3.LifecycleBindingsXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * @author jdcasey
 */
public class ClassLoaderXmlBindingLoader
    implements LifecycleBindingLoader
{

    // configuration.
    private String path;
    
    public ClassLoaderXmlBindingLoader()
    {
        // for plexus init.
    }
    
    public ClassLoaderXmlBindingLoader( String path )
    {
        this.path = path;
    }

    public LifecycleBindings getBindings()
        throws LifecycleLoaderException, LifecycleSpecificationException
    {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource( getPath() );

        if ( url == null )
        {
            throw new LifecycleLoaderException( "Classpath resource: " + getPath() + " could not be found." );
        }

        InputStreamReader reader;
        try
        {
            reader = new InputStreamReader( url.openStream() );
        }
        catch ( IOException e )
        {
            throw new LifecycleLoaderException( "Failed to open stream for classpath resource: " + getPath() + ". Reason: "
                + e.getMessage(), e );
        }

        LifecycleBindings bindings;
        try
        {
            bindings = new LifecycleBindingsXpp3Reader().read( reader );
        }
        catch ( IOException e )
        {
            throw new LifecycleLoaderException( "Classpath resource: " + getPath() + " could not be read. Reason: "
                + e.getMessage(), e );
        }
        catch ( XmlPullParserException e )
        {
            throw new LifecycleLoaderException( "Classpath resource: " + getPath() + " could not be parsed. Reason: "
                + e.getMessage(), e );
        }
        
        LifecycleUtils.setOrigin( bindings, url.toExternalForm() );
        
        return bindings;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath( String path )
    {
        this.path = path;
    }

}
