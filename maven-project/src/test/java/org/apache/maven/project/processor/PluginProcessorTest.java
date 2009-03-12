package org.apache.maven.project.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;

import junit.framework.TestCase;

public class PluginProcessorTest
    extends TestCase
{   
    public void testPluginExecutionCopyOfGoals()
    {
        Plugin plugin = new Plugin();
        plugin.setArtifactId( "aid" );
        plugin.setGroupId( "gid" );
        plugin.setVersion( "1.0" );
        
        PluginExecution ex = new PluginExecution();
        ex.setId( "id" );
        ex.setInherited( "true" );
        ex.setGoals( Arrays.asList("a", "b") );
        plugin.addExecution( ex );
        
        Plugin plugin1 = new Plugin();
        plugin1.setArtifactId( "aid" );
        plugin1.setGroupId( "gid" );
        plugin1.setVersion( "1.0" );
        
        PluginExecution ex1 = new PluginExecution();
        ex1.setId( "id" );
        ex1.setInherited( "true" );
        ex1.setGoals( Arrays.asList("b", "c") );
        plugin1.addExecution( ex1 );    
        
        List<Plugin> plugins = new ArrayList<Plugin>();
        
        PluginProcessor proc = new PluginProcessor();
        proc.process( plugin1, plugin, plugins, false );
        
        assertEquals(1, plugins.size());
        assertEquals(1, plugins.get( 0 ).getExecutions().size());
        assertEquals(3, plugins.get( 0 ).getExecutions().get( 0 ).getGoals().size());
    }
    
    public void testPluginJoin_GroupId()
    {
        Plugin plugin = new Plugin();
        plugin.setArtifactId( "aid" );
        plugin.setGroupId( "gid" );
        plugin.setVersion( "1.0" );
     
        Plugin plugin1 = new Plugin();
        plugin1.setArtifactId( "aid" );
        plugin1.setGroupId( "gid" );
        plugin1.setVersion( "1.0" );

        
        List<Plugin> plugins = new ArrayList<Plugin>();
        
        PluginProcessor proc = new PluginProcessor();
        proc.process( plugin1, plugin, plugins, false );
        
        assertEquals(1, plugins.size());
        assertEquals("gid", plugins.get( 0 ).getGroupId());
    } 
    
    public void testPluginExecutionJoin_Phase()
    {
        Plugin plugin = new Plugin();
        plugin.setArtifactId( "aid" );
        plugin.setGroupId( "gid" );
        plugin.setVersion( "1.0" );
        
        PluginExecution ex = new PluginExecution();
        ex.setId( "id" );
        ex.setPhase( "p" );
        ex.setGoals( Arrays.asList("a", "b") );
        plugin.addExecution( ex );
        
        Plugin plugin1 = new Plugin();
        plugin1.setArtifactId( "aid" );
        plugin1.setGroupId( "gid" );
        plugin1.setVersion( "1.0" );
        
        PluginExecution ex1 = new PluginExecution();
        ex1.setId( "id" );
        ex1.setPhase( "p1" );
        ex1.setGoals( Arrays.asList("b", "c") );
        plugin1.addExecution( ex1 );    
        
        List<Plugin> plugins = new ArrayList<Plugin>();
        
        PluginProcessor proc = new PluginProcessor();
        proc.process( plugin1, plugin, plugins, false );
        
        assertEquals(1, plugins.size());
        assertEquals(1, plugins.get( 0 ).getExecutions().size());
        assertEquals("p", plugins.get( 0 ).getExecutions().get( 0 ).getPhase());
    }       
    
    
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
