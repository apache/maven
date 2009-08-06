/**
 * 
 */
package org.apache.maven.project;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.component.annotations.Component;

@Component(role=ProjectBuilder.class,hint="classpath")
public class TestProjectBuilder
    extends DefaultProjectBuilder
{    
    
    @Override
    public MavenProject build( Artifact artifact, ProjectBuildingRequest request )
        throws ProjectBuildingException
    {                       
        if ( "maven-test".equals( artifact.getGroupId() ) )
        {
            String scope = artifact.getArtifactId().substring( "scope-".length() );
            
            try
            {
                artifact.setFile( ProjectClasspathTest.getFileForClasspathResource( ProjectClasspathTest.dir + "transitive-" + scope + "-dep.xml" ) );
            }
            catch ( FileNotFoundException e )
            {
                throw new IllegalStateException( "Missing test POM for " + artifact );
            }
        }
        if ( artifact.getFile() == null )
        {
            MavenProject project = new MavenProject();
            project.setArtifact( artifact );
            return project;
        }
        return build( artifact.getFile(), request );
    }

    @Override
    public MavenProject build( File pomFile, ProjectBuildingRequest configuration )
        throws ProjectBuildingException
    {
        MavenProject project = super.build( pomFile, configuration );

        project.setRemoteArtifactRepositories( Collections.<ArtifactRepository> emptyList() );

        return project;
    }

}