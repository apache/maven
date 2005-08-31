package org.apache.maven.project.artifact;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.injection.ModelDefaultsInjector;
import org.codehaus.plexus.PlexusTestCase;

import java.util.Map;

public class MavenMetadataSourceTest
    extends PlexusTestCase
{
    
    public void testShouldUseCompileScopeIfDependencyScopeEmpty() throws Exception
    {
        String groupId = "org.apache.maven";
        String artifactId = "maven-model";
        
        Dependency dep = new Dependency();
        
        dep.setGroupId(groupId);
        dep.setArtifactId(artifactId);
        dep.setVersion("2.0-alpha-3");
        
        Model model = new Model();
        
        model.addDependency(dep);
        
        MavenProject project = new MavenProject( model );
        
        ArtifactFactory factory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        
        project.setArtifacts( project.createArtifacts(factory, null, null) );
        
        String key = ArtifactUtils.versionlessKey(groupId, artifactId );
        
        Map artifactMap = project.getArtifactMap();
        
        assertNotNull( "artifact-map should not be null.", artifactMap );
        assertEquals( "artifact-map should contain 1 element.", 1, artifactMap.size() );
        
        Artifact artifact = (Artifact) artifactMap.get( key );
        
        assertNotNull( "dependency artifact not found in map.", artifact );
        assertEquals( "dependency artifact has wrong scope.", Artifact.SCOPE_COMPILE, artifact.getScope() );
        
        //check for back-propagation of default scope.
        assertEquals( "default scope NOT back-propagated to dependency.", Artifact.SCOPE_COMPILE, dep.getScope() );
    }

    public void testShouldUseInjectedTestScopeFromDependencyManagement() throws Exception
    {
        String groupId = "org.apache.maven";
        String artifactId = "maven-model";
        
        Dependency dep = new Dependency();
        
        dep.setGroupId(groupId);
        dep.setArtifactId(artifactId);
        dep.setVersion("2.0-alpha-3");
        
        Model model = new Model();
        
        model.addDependency(dep);
        
        Dependency mgd = new Dependency();
        mgd.setGroupId( groupId);
        mgd.setArtifactId( artifactId );
        mgd.setScope( Artifact.SCOPE_TEST);
        
        DependencyManagement depMgmt = new DependencyManagement();
        
        depMgmt.addDependency(mgd);
        
        model.setDependencyManagement(depMgmt);
        
        MavenProject project = new MavenProject( model );
        
        ModelDefaultsInjector injector = (ModelDefaultsInjector) lookup( ModelDefaultsInjector.ROLE );
        
        injector.injectDefaults( model );
        
        ArtifactFactory factory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );
        
        project.setArtifacts( project.createArtifacts(factory, null, null) );
        
        String key = ArtifactUtils.versionlessKey(groupId, artifactId );
        
        Map artifactMap = project.getArtifactMap();
        
        assertNotNull( "artifact-map should not be null.", artifactMap );
        assertEquals( "artifact-map should contain 1 element.", 1, artifactMap.size() );
        
        Artifact artifact = (Artifact) artifactMap.get( key );
        
        assertNotNull( "dependency artifact not found in map.", artifact );
        assertEquals( "dependency artifact has wrong scope.", Artifact.SCOPE_TEST, artifact.getScope() );
        
        //check for back-propagation of default scope.
        assertEquals( "default scope NOT back-propagated to dependency.", Artifact.SCOPE_TEST, dep.getScope() );
    }

}
