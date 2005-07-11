package org.apache.maven.plugin.plugin.metadata;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManagementException;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.wagon.ResourceDoesNotExistException;

import java.io.File;

public abstract class AbstractMetadataPublisherMojo
    extends AbstractMojo
{

    /**
     * @parameter
     */
    private String goalPrefix;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * @parameter expression="${project.build.directory}/repository-metadata"
     * @required
     * @readonly
     */
    private String outputDirectory;

    /**
     * @parameter expression="${component.org.apache.maven.artifact.repository.metadata.RepositoryMetadataManager}"
     * @required
     * @readonly
     */
    private RepositoryMetadataManager repositoryMetadataManager;

    protected String getGroupId()
    {
        return project.getGroupId();
    }

    protected String getArtifactId()
    {
        return project.getArtifactId();
    }

    protected String getGoalPrefix()
    {
        if ( goalPrefix == null )
        {
            goalPrefix = PluginDescriptor.getGoalPrefixFromArtifactId( getArtifactId() );
        }

        return goalPrefix;
    }

    protected void publish( boolean doDeploy )
        throws MojoExecutionException
    {
        ArtifactRepository distributionRepository = project.getDistributionManagementArtifactRepository();
        
        String remoteRepositoryId = "local";
        
        if( distributionRepository != null )
        {
            remoteRepositoryId = distributionRepository.getId();
        }
        else if ( doDeploy )
        {
            throw new MojoExecutionException( "You must provide a distribution repository for your plugin." );
        }
        
        RepositoryMetadata metadata = createMetadataInstance();

        try
        {
            repositoryMetadataManager.resolve( metadata, distributionRepository, localRepository, remoteRepositoryId );
        }
        catch ( RepositoryMetadataManagementException e )
        {
            Throwable cause = e.getCause();

            if ( cause == null || ( cause instanceof ResourceDoesNotExistException ) )
            {
                getLog().debug( "Cannot find remote repository metadata for: " + metadata.getRepositoryPath() + "; creating new metadata resource..." );
                
                metadata.setFile( null );
            }
            else
            {
                throw new MojoExecutionException( "Cannot retrieve plugin-mapping metadata for: "
                    + metadata.getRepositoryPath(), e );
            }
        }

        updateMetadata( metadata );
        
        if ( distributionRepository != null )
        {
            try
            {
                repositoryMetadataManager.install( metadata, localRepository, remoteRepositoryId );
            }
            catch ( RepositoryMetadataManagementException e )
            {
                throw new MojoExecutionException( "Error installing plugin-mapping metadata to local repository.", e );
            }

            if ( doDeploy )
            {
                try
                {
                    repositoryMetadataManager.deploy( metadata, distributionRepository );
                }
                catch ( RepositoryMetadataManagementException e )
                {
                    throw new MojoExecutionException(
                                                      "Error deploying plugin-mapping metadata to distribution repository: "
                                                          + distributionRepository.getId(), e );
                }
            }
        }
    }

    protected File getMetadataFile( String path )
    {
        File metadataFile = new File( outputDirectory, path ).getAbsoluteFile();

        File dir = metadataFile.getParentFile();

        if ( !dir.exists() )
        {
            dir.mkdirs();
        }
        
        return metadataFile;
    }

    protected abstract void updateMetadata( RepositoryMetadata metadata )
        throws MojoExecutionException;

    protected abstract RepositoryMetadata createMetadataInstance();

}
