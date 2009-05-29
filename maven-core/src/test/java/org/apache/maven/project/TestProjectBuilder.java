/**
 * 
 */
package org.apache.maven.project;

import java.io.FileNotFoundException;

import org.apache.maven.artifact.Artifact;
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
            return new MavenProject();
        }
        return build( artifact.getFile(), request );
    }
}