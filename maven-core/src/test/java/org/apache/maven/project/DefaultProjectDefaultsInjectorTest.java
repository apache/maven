// TODO Attach license header here.
package org.apache.maven.project;

import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;

import junit.framework.TestCase;

/**
 * @author jdcasey
 *
 * Created on Feb 1, 2005
 */
public class DefaultProjectDefaultsInjectorTest
    extends TestCase
{
    
    public void testShouldConstructWithNoParams()
    {
        new DefaultProjectDefaultsInjector();
    }
    
    public void testShouldSucceedInMergingDependencyWithDependency()
    {
        Model model = new Model();
        
        Dependency dep = new Dependency();
        dep.setGroupId("myGroup");
        dep.setArtifactId("myArtifact");
        
        model.addDependency(dep);
        
        Dependency def = new Dependency();
        def.setGroupId(dep.getGroupId());
        def.setArtifactId(dep.getArtifactId());
        def.setVersion("1.0.1");
        
        DependencyManagement depMgmt = new DependencyManagement();
        
        depMgmt.addDependency(def);
        
        model.setDependencyManagement(depMgmt);
        
        MavenProject project = new MavenProject(model);
        
        new DefaultProjectDefaultsInjector().injectDefaults(project);
        
        List deps = project.getDependencies();
        assertEquals(1, deps.size());
        
        Dependency result = (Dependency)deps.get(0);
        assertEquals(def.getVersion(), result.getVersion());
    }

    public void testShouldMergeDefaultUrlAndArtifactWhenDependencyDoesntSupplyVersion()
    {
        Model model = new Model();
        
        Dependency dep = new Dependency();
        dep.setGroupId("myGroup");
        dep.setArtifactId("myArtifact");
        
        model.addDependency(dep);
        
        Dependency def = new Dependency();
        def.setGroupId(dep.getGroupId());
        def.setArtifactId(dep.getArtifactId());
        def.setVersion("1.0.1");
        
        DependencyManagement depMgmt = new DependencyManagement();
        
        depMgmt.addDependency(def);
        
        model.setDependencyManagement(depMgmt);
        
        MavenProject project = new MavenProject(model);
        
        new DefaultProjectDefaultsInjector().injectDefaults(project);
        
        List deps = project.getDependencies();
        assertEquals(1, deps.size());
        
        Dependency result = (Dependency)deps.get(0);
        assertEquals(def.getVersion(), result.getVersion());
    }

    public void testShouldNotMergeDefaultUrlOrArtifactWhenDependencySuppliesVersion()
    {
        Model model = new Model();
        
        Dependency dep = new Dependency();
        dep.setGroupId("myGroup");
        dep.setArtifactId("myArtifact");
        dep.setVersion("1.0.1");
        
        model.addDependency(dep);
        
        Dependency def = new Dependency();
        def.setGroupId(dep.getGroupId());
        def.setArtifactId(dep.getArtifactId());
        
        DependencyManagement depMgmt = new DependencyManagement();
        
        depMgmt.addDependency(def);
        
        model.setDependencyManagement(depMgmt);
        
        MavenProject project = new MavenProject(model);
        
        new DefaultProjectDefaultsInjector().injectDefaults(project);
        
        List deps = project.getDependencies();
        assertEquals(1, deps.size());
        
        Dependency result = (Dependency)deps.get(0);
        assertEquals(dep.getVersion(), result.getVersion());
    }

    public void testShouldMergeDefaultPropertiesWhenDependencyDoesntSupplyProperties()
    {
        Model model = new Model();
        
        Dependency dep = new Dependency();
        dep.setGroupId("myGroup");
        dep.setArtifactId("myArtifact");
        dep.setVersion("1.0.1");
        
        model.addDependency(dep);
        
        Dependency def = new Dependency();
        def.setGroupId(dep.getGroupId());
        def.setArtifactId(dep.getArtifactId());
        
        Properties props = new Properties();
        props.setProperty("test", "value");
        
        def.setProperties(props);
        
        DependencyManagement depMgmt = new DependencyManagement();
        
        depMgmt.addDependency(def);
        
        model.setDependencyManagement(depMgmt);
        
        MavenProject project = new MavenProject(model);
        
        new DefaultProjectDefaultsInjector().injectDefaults(project);
        
        List deps = project.getDependencies();
        assertEquals(1, deps.size());
        
        Dependency result = (Dependency)deps.get(0);
        
        assertEquals("value", result.getProperties().getProperty("test"));
    }

    public void testShouldNotMergeDefaultPropertiesWhenDependencySuppliesProperties()
    {
        Model model = new Model();
        
        Dependency dep = new Dependency();
        dep.setGroupId("myGroup");
        dep.setArtifactId("myArtifact");
        dep.setVersion("1.0.1");
        
        Properties props = new Properties();
        props.setProperty("test", "value");
        
        dep.setProperties(props);
        
        model.addDependency(dep);
        
        Dependency def = new Dependency();
        def.setGroupId(dep.getGroupId());
        def.setArtifactId(dep.getArtifactId());
        
        Properties props2 = new Properties();
        props2.setProperty("test", "value2");
        
        def.setProperties(props2);
        
        DependencyManagement depMgmt = new DependencyManagement();
        
        depMgmt.addDependency(def);
        
        model.setDependencyManagement(depMgmt);
        
        MavenProject project = new MavenProject(model);
        
        new DefaultProjectDefaultsInjector().injectDefaults(project);
        
        List deps = project.getDependencies();
        assertEquals(1, deps.size());
        
        Dependency result = (Dependency)deps.get(0);
        assertEquals("value", result.getProperties().getProperty("test"));
    }
    
    public void testShouldRejectDependencyWhereNoVersionIsFoundAfterDefaultsInjection()
    {
        Model model = new Model();
        
        Dependency dep = new Dependency();
        dep.setGroupId("myGroup");
        dep.setArtifactId("myArtifact");
        
        model.addDependency(dep);
        
        Dependency def = new Dependency();
        def.setGroupId(dep.getGroupId());
        def.setArtifactId(dep.getArtifactId());
        
        DependencyManagement depMgmt = new DependencyManagement();
        
        depMgmt.addDependency(def);
        
        model.setDependencyManagement(depMgmt);
        
        MavenProject project = new MavenProject(model);
        
        try
        {
            new DefaultProjectDefaultsInjector().injectDefaults( project );
            fail("Should fail to validate dependency without a version.");
        }
        catch ( IllegalStateException e )
        {
            // should throw when it detects a missing version in the test dependency.
        }
    }

}
