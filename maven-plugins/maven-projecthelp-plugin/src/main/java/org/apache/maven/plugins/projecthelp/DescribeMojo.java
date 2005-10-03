package org.apache.maven.plugins.projecthelp;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

/**
 * @goal describe
 * @aggregator
 */
public class DescribeMojo
    extends AbstractMojo
{

    /**
     * @parameter expression="${plugin}"
     */
    private String plugin;

    /**
     * @parameter
     */
    private String groupId;

    /**
     * @parameter
     */
    private String artifactId;

    /**
     * @parameter
     */
    private String version;

    /**
     * @parameter
     */
    private String mojo;

    /**
     * @component role="org.apache.maven.plugin.PluginManager"
     */
    private PluginManager pluginManager;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    private Settings settings;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * @parameter
     */
    private File output;

    public void execute()
        throws MojoExecutionException
    {
        String pluginPrefix = null;

        if ( plugin != null && plugin.length() > 0 )
        {
            if ( plugin.indexOf( ":" ) > -1 )
            {
                String[] pluginParts = plugin.split( ":" );

                switch ( pluginParts.length )
                {
                    case ( 1 ):
                    {
                        pluginPrefix = pluginParts[0];
                        break;
                    }
                    case ( 2 ):
                    {
                        groupId = pluginParts[0];
                        artifactId = pluginParts[1];
                        break;
                    }
                    case ( 3 ):
                    {
                        groupId = pluginParts[0];
                        artifactId = pluginParts[1];
                        version = pluginParts[2];
                        break;
                    }
                    default:
                    {
                        throw new MojoExecutionException(
                                                          "plugin parameter must be a plugin prefix, or conform to: 'groupId:artifactId[:version]." );
                    }
                }
            }
            else
            {
                pluginPrefix = plugin;
            }
        }

        PluginDescriptor descriptor;

        if ( pluginPrefix != null )
        {
//            try
//            {
                descriptor = pluginManager.getPluginDescriptorForPrefix( pluginPrefix );
//            }
//            catch ( PluginManagerException e )
//            {
//                throw new MojoExecutionException( "Error retrieving plugin descriptor for prefix: \'" + pluginPrefix
//                    + "\'.", e );
//            }
        }
        else if ( groupId != null && artifactId != null )
        {
            Plugin plugin = new Plugin();

            plugin.setGroupId( groupId );
            plugin.setArtifactId( artifactId );

            if ( version != null )
            {
                plugin.setVersion( version );
            }

            try
            {
                descriptor = pluginManager.verifyPlugin( plugin, project, settings, localRepository );
            }
            catch ( ArtifactResolutionException e )
            {
                throw new MojoExecutionException( "Error retrieving plugin descriptor for:\n\ngroupId: \'" + groupId
                    + "\'\nartifactId: \'" + artifactId + "\'\nversion: \'" + version + "\'\n\n", e );
            }
            catch ( PluginManagerException e )
            {
                throw new MojoExecutionException( "Error retrieving plugin descriptor for:\n\ngroupId: \'" + groupId
                    + "\'\nartifactId: \'" + artifactId + "\'\nversion: \'" + version + "\'\n\n", e );
            }
            catch ( PluginVersionResolutionException e )
            {
                throw new MojoExecutionException( "Error retrieving plugin descriptor for:\n\ngroupId: \'" + groupId
                    + "\'\nartifactId: \'" + artifactId + "\'\nversion: \'" + version + "\'\n\n", e );
            }
        }
        else
        {
            throw new MojoExecutionException(
                                              "You must either specify \'groupId\' and \'artifactId\', or a valid \'plugin\' parameter." );
        }

        StringBuffer descriptionBuffer = new StringBuffer();

        if ( mojo != null && mojo.length() > 0 )
        {
            describeMojo( descriptor.getMojo( mojo ), descriptionBuffer );
        }
        else
        {
            describePlugin( descriptor, descriptionBuffer );
        }

        if ( output != null )
        {
            Writer out = null;
            try
            {
                out = new FileWriter( output );

                out.write( descriptionBuffer.toString() );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Cannot write plugin/mojo description.", e );
            }
            finally
            {
                if ( out != null )
                {
                    try
                    {
                        out.close();
                    }
                    catch ( IOException e )
                    {
                        getLog().debug( "Error closing file output.", e );
                    }
                }
            }
        }
        else
        {
            System.out.println( descriptionBuffer.toString() );
        }
    }

    private void describePlugin( PluginDescriptor pd, StringBuffer buffer )
    {
        buffer.append( "Description of Plugin" ).append( "\n-----------------------------------------------" )
            .append( "\n\nGroup Id:  " ).append( pd.getGroupId() ).append( "\nArtifact Id: " )
            .append( pd.getArtifactId() ).append( "\nVersion:     " ).append( pd.getVersion() )
            .append( "\nGoal Prefix: " ).append( pd.getGoalPrefix() ).append( "\n\nMojos:" )
            .append( "\n-----------------------------------------------" ).append( "\n\n" );

        for ( Iterator it = pd.getMojos().iterator(); it.hasNext(); )
        {
            MojoDescriptor md = (MojoDescriptor) it.next();

            describeMojoGuts( md, buffer );

            buffer.append( "\n-----------------------------------------------" ).append( "\n\n" );
        }
    }

    private void describeMojo( MojoDescriptor md, StringBuffer buffer )
    {
        buffer.append( "Description of Mojo" ).append( "\n-----------------------------------------------" )
            .append( "\n\n" );

        describeMojoGuts( md, buffer );
    }

    private void describeMojoGuts( MojoDescriptor md, StringBuffer buffer )
    {
        // TODO Complete mojo description dump.
        buffer.append( "TODO!" );
    }

    public final String getPlugin()
    {
        return plugin;
    }

    public final void setPlugin( String plugin )
    {
        this.plugin = plugin;
    }

    public final PluginManager getPluginManager()
    {
        return pluginManager;
    }

    public final void setPluginManager( PluginManager pluginManager )
    {
        this.pluginManager = pluginManager;
    }

    public final String getArtifactId()
    {
        return artifactId;
    }

    public final void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public final String getGroupId()
    {
        return groupId;
    }

    public final void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public final ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public final void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    public final String getMojo()
    {
        return mojo;
    }

    public final void setMojo( String mojo )
    {
        this.mojo = mojo;
    }

    public final File getOutput()
    {
        return output;
    }

    public final void setOutput( File output )
    {
        this.output = output;
    }

    public final MavenProject getProject()
    {
        return project;
    }

    public final void setProject( MavenProject project )
    {
        this.project = project;
    }

    public final Settings getSettings()
    {
        return settings;
    }

    public final void setSettings( Settings settings )
    {
        this.settings = settings;
    }

    public final String getVersion()
    {
        return version;
    }

    public final void setVersion( String version )
    {
        this.version = version;
    }

}
