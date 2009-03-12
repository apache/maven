package org.apache.maven.project.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;

import junit.framework.TestCase;

public class PluginsManagementProcessorTest extends TestCase
{
    public void testChildCopy_Dependencies()
    {
        PluginsManagementProcessor proc = new PluginsManagementProcessor();
        
        Plugin p = new Plugin();
        p.setArtifactId( "aid" );
        p.setGroupId( "gid");
        p.setVersion( "1.0" );
        Dependency d = new Dependency();
        d.setArtifactId( "d-aid" );
        d.setGroupId( "gid" );
        
        p.setDependencies( new ArrayList<Dependency>(Arrays.asList(d) ));
        
        Plugin p1 = new Plugin();
        p1.setArtifactId( "aid" );
        p1.setGroupId( "gid");
        p1.setVersion( "1.0" );
        p1.setInherited( "true" );
        
        Dependency d1 = new Dependency();
        d1.setArtifactId( "d1-aid" );
        d1.setGroupId( "gid" );
        
        p1.setDependencies( Arrays.asList( d1 ) );
        
        List<Plugin> plugins = new ArrayList<Plugin>();
        plugins.add(p);
        proc.process( null, Arrays.asList(p1), plugins , false);
        
        assertEquals(1, plugins.size());
        assertEquals(2, plugins.get( 0 ).getDependencies().size());
        assertEquals("true", plugins.get( 0 ).getInherited());
    }   
}
