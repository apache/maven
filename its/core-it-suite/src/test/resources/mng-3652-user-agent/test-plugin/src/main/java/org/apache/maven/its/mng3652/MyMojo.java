package org.apache.maven.its.mng3652;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.execution.RuntimeInformation;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.util.IOUtil;

/**
 * Goal which attempts to download a dummy artifact from a repository on localhost
 * at the specified port. This is used to allow the unit test class to record the
 * User-Agent HTTP header in use. It will also write the maven version in use to
 * a file in the output directory, for comparison in the unit tests assertions.
 *
 * @goal touch
 * 
 * @phase validate
 */
public class MyMojo
    extends AbstractMojo
{
    
    private static final String LS = System.getProperty( "line.separator" );
    
    /**
     * @parameter default-value="${project.build.directory}/touch.txt"
     */
    private File touchFile;
    
    /**
     * @component
     */
    private WagonManager wagonManager;
    
    /**
     * @component
     */
    private ArtifactFactory artifactFactory;
    
    /**
     * @component
     */
    private ArtifactRepositoryFactory repositoryFactory;
    
    /**
     * @component
     */
    private ArtifactRepositoryLayout layout;
    
    /**
     * @component
     */
    private RuntimeInformation runtimeInformation;
    
    /**
     * @parameter expression="${testProtocol}" default-value="http"
     * @required
     */
    private String testProtocol;
    
    /**
     * @parameter expression="${testPort}"
     * @required
     */
    private String testPort;
    
    public void execute()
        throws MojoExecutionException
    {
        ArtifactRepository remote =
            repositoryFactory.createArtifactRepository( "test", testProtocol + "://127.0.0.1:" + testPort, layout,
                                                        new ArtifactRepositoryPolicy(), new ArtifactRepositoryPolicy() );
        
        Artifact artifact = artifactFactory.createArtifact( "bad.group", "missing-artifact", "1", null, "jar" );
        
        File tempArtifactFile;
        try
        {
            tempArtifactFile = File.createTempFile( "artifact-temp.", ".jar" );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to create temp file for artifact transfer attempt.", e );
        }
        
        tempArtifactFile.deleteOnExit();
        
        artifact.setFile( tempArtifactFile );
        
        try
        {
            wagonManager.getArtifact( artifact, remote );
        }
        catch ( TransferFailedException e )
        {
            // ignore on purpose - it doesn't actually send any content
        }
        catch ( ResourceDoesNotExistException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }

        String artifactVersion;
        InputStream resourceAsStream = null;
        try
        {
            Properties properties = new Properties();
            resourceAsStream = getClass().getClassLoader().getResourceAsStream( "META-INF/maven/org.apache.maven/maven-artifact/pom.properties" );
            if ( resourceAsStream == null )
            {
                resourceAsStream = getClass().getClassLoader().getResourceAsStream( "META-INF/maven/org.apache.maven.artifact/maven-artifact/pom.properties" );
            }
            properties.load( resourceAsStream );

            artifactVersion = properties.getProperty( "version" );
            if ( artifactVersion == null )
            {
                throw new MojoExecutionException( "Artifact version not found" );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Unable to read properties file from maven-core", e );
        }
        finally
        {
            IOUtil.close( resourceAsStream );
        }
        
        FileWriter w = null;
        try
        {
            touchFile.getParentFile().mkdirs();
            w = new FileWriter( touchFile );
            
            w.write( runtimeInformation.getApplicationVersion().toString() );
            w.write( LS );
            w.write( System.getProperty( "java.version" ) );
            w.write( LS );
            w.write( System.getProperty( "os.name" ) );
            w.write( LS );
            w.write( System.getProperty( "os.version" ) );
            w.write( LS );
            w.write( artifactVersion );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Failed to write touch-file: " + touchFile, e );
        }
        finally
        {
            IOUtil.close( w );
        }
    }
}
