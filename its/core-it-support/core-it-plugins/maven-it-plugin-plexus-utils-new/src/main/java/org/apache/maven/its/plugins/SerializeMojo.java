package org.apache.maven.its.plugins;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.MXSerializer;
import org.codehaus.plexus.util.xml.pull.XmlSerializer;

/**
 * @goal serialize
 * @phase validate
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
        Writer writer = null;
        XmlSerializer s = new MXSerializer();
        try
        {
            writer = new OutputStreamWriter( new FileOutputStream( filename ), "UTF-8" );
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
