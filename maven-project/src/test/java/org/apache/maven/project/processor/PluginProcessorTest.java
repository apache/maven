package org.apache.maven.project.processor;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomUtils;

import junit.framework.TestCase;

public class PluginProcessorTest
    extends TestCase
{
    public void testPluginDependencyChildCopy_DependencyGroupId()
    {        
        Dependency dependency = new Dependency();
        dependency.setGroupId( "gid" );
        
        List<Plugin> target = new ArrayList<Plugin>();
        Plugin child = new Plugin();
        child.addDependency( dependency );
        
        PluginProcessor proc = new PluginProcessor();
        proc.process( null, child, target, false );
        
        assertEquals(1, target.size());
        assertEquals(1, target.get( 0 ).getDependencies().size());
        assertEquals("gid", target.get( 0 ).getDependencies().get( 0 ).getGroupId());
    }
    
    public void testPluginDependencyJoin()
    {        
        Dependency dependency = new Dependency();
        dependency.setGroupId( "gid" );
        
        List<Plugin> target = new ArrayList<Plugin>();
        
        Plugin child = new Plugin();
        child.setArtifactId( "aid" );
        child.setGroupId( "gid" );
        child.setVersion( "1.0" );
        child.addDependency( dependency );
        
        Plugin parent = new Plugin();
        parent.setGroupId( "gid" );
        parent.setArtifactId( "aid" );
        parent.setVersion( "1.0" );
        
        Dependency dependency1 = new Dependency();
        dependency1.setGroupId( "gid1" );
        parent.addDependency( dependency1 );
        
        PluginProcessor proc = new PluginProcessor();
        proc.process( parent, child, target, false );
        
        assertEquals(1, target.size());
        assertEquals(2, target.get( 0 ).getDependencies().size());
        assertEquals("gid", target.get( 0 ).getDependencies().get( 0 ).getGroupId());
        assertEquals("gid1", target.get( 0 ).getDependencies().get( 1 ).getGroupId());
    }  
    /*
    public void testMergeOfPluginConfiguration()
    {

        List<Plugin> target = new ArrayList<Plugin>();
        
        Xpp3Dom dom = new Xpp3Dom("a");
        Xpp3Dom dom2 = new Xpp3Dom("b");
        dom2.setValue( "test3" );
        dom.addChild( dom2 );
        
        Plugin child = new Plugin();
        child.setArtifactId( "aid" );
        child.setGroupId( "gid" );
        child.setVersion( "1.0" );
        child.setConfiguration( dom );
        
        Plugin parent = new Plugin();
        parent.setGroupId( "gid" );
        parent.setArtifactId( "aid" );
        parent.setVersion( "1.0" );
        
        Xpp3Dom dom3 = new Xpp3Dom("a");
        Xpp3Dom dom4 = new Xpp3Dom("b");
        dom4.setValue( "test2" );
        dom.addChild( dom4 );
        
        parent.setConfiguration( dom3 );

        PluginProcessor proc = new PluginProcessor();
        proc.process( parent, child, target, false );     
        
        assertNotNull(target.get( 0 ).getConfiguration() );
        assertEquals( 2, ((Xpp3Dom) target.get( 0 ).getConfiguration()).getChildren( "b" ).length );
    }
    */
}
