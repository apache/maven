package org.apache.maven.settings;

import java.util.List;

import junit.framework.TestCase;

public class SettingsUtilsTest
    extends TestCase
{

    public void testShouldAppendRecessivePluginGroupIds()
    {
        Settings dominant = new Settings();
        dominant.addPluginGroup( "org.apache.maven.plugins" );
        dominant.addPluginGroup( "org.codehaus.modello" );
        
        dominant.setRuntimeInfo(new RuntimeInfo(dominant));

        Settings recessive = new Settings();
        recessive.addPluginGroup( "org.codehaus.plexus" );

        recessive.setRuntimeInfo(new RuntimeInfo(recessive));

        SettingsUtils.merge( dominant, recessive, Settings.GLOBAL_LEVEL );

        List pluginGroups = dominant.getPluginGroups();

        assertNotNull( pluginGroups );
        assertEquals( 3, pluginGroups.size() );
        assertEquals( "org.apache.maven.plugins", pluginGroups.get( 0 ) );
        assertEquals( "org.codehaus.modello", pluginGroups.get( 1 ) );
        assertEquals( "org.codehaus.plexus", pluginGroups.get( 2 ) );
    }

}
