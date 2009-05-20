/**
 * 
 */
package org.apache.maven.project;

import java.io.FileNotFoundException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Component(role=MavenProjectBuilder.class,hint="classpath")
public class TestMavenProjectBuilder
    extends DefaultMavenProjectBuilder
{    
    @Requirement(hint="classpath")
    private RepositorySystem repositorySystem;
    
    @Override
    public MavenProject buildFromRepository( Artifact artifact, ProjectBuilderConfiguration configuration )
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
            return new MavenProject();
        }
        return build( artifact.getFile(), configuration );
    }
}