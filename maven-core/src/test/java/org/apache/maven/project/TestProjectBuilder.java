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
    public ProjectBuildingResult build( Artifact artifact, ProjectBuildingRequest request )
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
            return new DefaultProjectBuildingResult( project, null, null );
        }
        return build( artifact.getFile(), request );
    }

    @Override
    public ProjectBuildingResult build( File pomFile, ProjectBuildingRequest configuration )
        throws ProjectBuildingException
    {
        ProjectBuildingResult result = super.build( pomFile, configuration );

        result.getProject().setRemoteArtifactRepositories( Collections.<ArtifactRepository> emptyList() );

        return result;
    }

}