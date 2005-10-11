package org.apache.maven.plugins.projecthelp;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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
import java.util.List;

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
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    private MavenSession session;

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
        throws MojoExecutionException, MojoFailureException
    {
        PluginInfo pi = new PluginInfo();
        
        parsePluginLookupInfo( pi );
        
        PluginDescriptor descriptor = lookupPluginDescriptor( pi );

        StringBuffer descriptionBuffer = new StringBuffer();

        if ( mojo != null && mojo.length() > 0 )
        {
            describeMojo( descriptor.getMojo( mojo ), descriptionBuffer );
        }
        else
        {
            describePlugin( descriptor, descriptionBuffer );
        }

        writeDescription( descriptionBuffer );
    }

    private void writeDescription( StringBuffer descriptionBuffer ) throws MojoExecutionException
    {
        if ( output != null )
        {
            Writer out = null;
            try
            {
                output.getParentFile().mkdirs();
                
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
            getLog().info( descriptionBuffer.toString() );
        }
    }

    private PluginDescriptor lookupPluginDescriptor( PluginInfo pi ) throws MojoExecutionException, MojoFailureException
    {
        PluginDescriptor descriptor = null;
        
        Plugin forLookup = null;
        
        if ( pi.prefix != null )
        {
            descriptor = pluginManager.getPluginDescriptorForPrefix( pi.prefix );
            
            if ( descriptor == null )
            {
                try
                {
                    forLookup = pluginManager.getPluginDefinitionForPrefix( pi.prefix, session, project );
                }
                catch ( PluginManagerException e )
                {
                    throw new MojoExecutionException(
                        "Cannot resolve plugin-prefix: \'" + pi.prefix + "\' from plugin mappings metadata.", e );
                }
            }
        }
        else if ( pi.groupId != null && pi.artifactId != null )
        {
            forLookup = new Plugin();
            
            forLookup.setGroupId( pi.groupId );
            forLookup.setArtifactId( pi.artifactId );

            if ( pi.version != null )
            {
                forLookup.setVersion( pi.version );
            }
        }
        else
        {
            throw new MojoFailureException("You must either specify \'groupId\' and \'artifactId\', or a valid \'plugin\' parameter." );
        }
        
        if ( descriptor == null && forLookup != null )
        {
            try
            {
                descriptor = pluginManager.verifyPlugin( forLookup, project, settings, localRepository );
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
            catch ( ArtifactNotFoundException e )
            {
                throw new MojoExecutionException( "Error retrieving plugin descriptor for:\n\ngroupId: \'" + groupId
                                                  + "\'\nartifactId: \'" + artifactId + "\'\nversion: \'" + version + "\'\n\n", e );
            }
        }
        
        return descriptor;
    }

    private void parsePluginLookupInfo( PluginInfo pi ) throws MojoFailureException
    {
        if ( plugin != null && plugin.length() > 0 )
        {
            if ( plugin.indexOf( ":" ) > -1 )
            {
                String[] pluginParts = plugin.split( ":" );

                switch ( pluginParts.length )
                {
                    case ( 1 ):
                    {
                        pi.prefix = pluginParts[0];
                        break;
                    }
                    case ( 2 ):
                    {
                        pi.groupId = pluginParts[0];
                        pi.artifactId = pluginParts[1];
                        break;
                    }
                    case ( 3 ):
                    {
                        pi.groupId = pluginParts[0];
                        pi.artifactId = pluginParts[1];
                        pi.version = pluginParts[2];
                        break;
                    }
                    default:
                    {
                        throw new MojoFailureException("plugin parameter must be a plugin prefix, or conform to: 'groupId:artifactId[:version]." );
                    }
                }
            }
            else
            {
                pi.prefix = plugin;
            }
        }
        else
        {
            pi.groupId = groupId;
            pi.artifactId = artifactId;
            pi.version = version;
        }
    }

    private void describePlugin( PluginDescriptor pd, StringBuffer buffer )
    {
        buffer.append( "Plugin: \'" ).append( pd.getName() ).append( '\'' );
        buffer.append( "\n-----------------------------------------------" );
        buffer.append( "\nGroup Id:  " ).append( pd.getGroupId() );
        buffer.append( "\nArtifact Id: " ).append( pd.getArtifactId() );
        buffer.append( "\nVersion:     " ).append( pd.getVersion() );
        buffer.append( "\nGoal Prefix: " ).append( pd.getGoalPrefix() );
        buffer.append( "\nDescription:\n\n" ).append( pd.getDescription() ).append( "\n" );
        buffer.append( "\nMojos:\n" );

        for ( Iterator it = pd.getMojos().iterator(); it.hasNext(); )
        {
            MojoDescriptor md = (MojoDescriptor) it.next();

            buffer.append( "\nGoal: \'" ).append( md.getGoal() ).append( '\'' );
            buffer.append( "\n========================================" );
            
            describeMojoGuts( md, buffer );
            
            buffer.append( "\n\n" );
        }
    }

    private void describeMojo( MojoDescriptor md, StringBuffer buffer )
    {
        buffer.append( "Mojo: \'" ).append( md.getFullGoalName() ).append( '\'' );
        buffer.append( "\n-----------------------------------------------" );
        buffer.append( "\nGoal: \'" ).append( md.getGoal() ).append( "\'" );

        describeMojoGuts( md, buffer );
        
        buffer.append( "\n\n" );
    }

    private void describeMojoGuts( MojoDescriptor md, StringBuffer buffer )
    {
        buffer.append( "\nDescription:\n\n" ).append( md.getDescription() ).append( "\n" );
        
        String deprecation = md.getDeprecated();
        
        if ( deprecation != null )
        {
            buffer.append( "\n\nNOTE: This mojo is deprecated.\n" ).append( deprecation ).append( "\n" );
        }
        
        buffer.append( "\nImplementation: " ).append( md.getImplementation() );
        buffer.append( "\nLanguage: " ).append( md.getLanguage() );
        
        String phase = md.getPhase();
        if ( phase != null )
        {
            buffer.append( "\nBound to Phase: " ).append( phase );
        }
        
        String eGoal = md.getExecuteGoal();
        String eLife = md.getExecuteLifecycle();
        String ePhase = md.getExecutePhase();
        
        if ( eGoal != null || ePhase != null )
        {
            buffer.append( "\n\nBefore this mojo executes, it will call:\n" );
            
            if ( eGoal != null )
            {
                buffer.append( "\nSingle mojo: \'" ).append( eGoal ).append( "\'" );
            }
            
            if ( ePhase != null )
            {
                buffer.append( "\nPhase: \'" ).append( ePhase ).append( "\'" );
                
                if ( eLife != null )
                {
                    buffer.append( " in Lifecycle Overlay: \'" ).append( eLife ).append( "\'" );
                }
            }
        }
        
        List parameters = md.getParameters();
        
        List requirements = md.getRequirements();
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
    
    private static class PluginInfo
    {
        String prefix;
        String groupId;
        String artifactId;
        String version;
        String mojo;
        
        Plugin plugin;
        PluginDescriptor pluginDescriptor;
    }

}
