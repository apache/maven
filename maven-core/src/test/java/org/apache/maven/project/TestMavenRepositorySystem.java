package org.apache.maven.project;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ResolutionGroup;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.repository.LegacyRepositorySystem;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.repository.VersionNotFoundException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

@Component(role = RepositorySystem.class, hint = "test")
public class TestMavenRepositorySystem
    extends LegacyRepositorySystem
{
    public ResolutionGroup retrieve( Artifact artifact, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
        throws ArtifactMetadataRetrievalException
    {
        Model model = null;
        InputStreamReader r = null;
        try
        {
            String scope = artifact.getArtifactId().substring( "scope-".length() );
            if ( "maven-test".equals( artifact.getGroupId() ) )
            {
                String name = "/projects/scope/transitive-" + scope + "-dep.xml";
                r = new InputStreamReader( getClass().getResourceAsStream( name ) );
                MavenXpp3Reader reader = new MavenXpp3Reader();
                model = reader.read( r );
            }
            else
            {
                model = new Model();
            }
            model.setGroupId( artifact.getGroupId() );
            model.setArtifactId( artifact.getArtifactId() );
        }
        catch ( IOException e )
        {
            throw new ArtifactMetadataRetrievalException( e );
        }
        catch ( XmlPullParserException e )
        {
            throw new ArtifactMetadataRetrievalException( e );
        }
        finally
        {
            IOUtil.close( r );
        }

        Set artifacts;
        try
        {
            artifacts = createArtifacts( model.getDependencies(), artifact.getScope(), null, null );
        }
        catch ( VersionNotFoundException e )
        {
            InvalidDependencyVersionException ee = new InvalidDependencyVersionException(e.getProjectId(), e.getDependency(),e.getPomFile(), e.getCauseException() );
            
            throw new ArtifactMetadataRetrievalException( ee );
        }

        return new ResolutionGroup( artifact, artifacts, remoteRepositories );
    }
}
