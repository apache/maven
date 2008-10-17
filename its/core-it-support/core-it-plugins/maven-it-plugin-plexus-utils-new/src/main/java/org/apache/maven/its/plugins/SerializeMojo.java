package org.apache.maven.its.plugins;

import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.MXSerializer;
import org.codehaus.plexus.util.xml.pull.XmlSerializer;

/**
 * @goal serialize
 * @phase package
 */
public class SerializeMojo
    extends AbstractMojo
{

    /**
     * @parameter default-value="${project.build.directory}/serialized.xml"
     */
    private String filename;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        FileWriter writer = null;
        XmlSerializer s = new MXSerializer();
        try
        {
            writer = new FileWriter( filename );
            s.setOutput( writer );

            Xpp3Dom dom = new Xpp3Dom( "root" );

            dom.writeToSerializer( "", s );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        finally
        {
            IOUtils.closeQuietly( writer );
        }
    }
}
