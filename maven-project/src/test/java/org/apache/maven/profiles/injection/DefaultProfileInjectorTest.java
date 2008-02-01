package org.apache.maven.profiles.injection;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Repository;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public class DefaultProfileInjectorTest
    extends TestCase
{

    public void testShouldUseMainPluginDependencyVersionOverManagedDepVersion()
    {
        PluginContainer profile = new PluginContainer();
        Plugin profilePlugin = createPlugin( "group", "artifact", "1", Collections.EMPTY_MAP );
        Dependency profileDep = createDependency( "g", "a", "2" );
        profilePlugin.addDependency( profileDep );
        profile.addPlugin( profilePlugin );

        PluginContainer model = new PluginContainer();
        Plugin plugin = createPlugin( "group", "artifact", "1", Collections.EMPTY_MAP );
        Dependency dep = createDependency( "g", "a", "1" );
        plugin.addDependency( dep );
        model.addPlugin( plugin );

        new DefaultProfileInjector().injectPlugins( profile, model );

        assertEquals( profileDep.getVersion(), ((Dependency) plugin.getDependencies().get( 0 ) ).getVersion() );
    }

    private Dependency createDependency( String gid,
                                         String aid,
                                         String ver )
    {
        Dependency dep = new Dependency();
        dep.setGroupId( gid );
        dep.setArtifactId( aid );
        dep.setVersion( ver );

        return dep;
    }

    /**
     * Test that this is the resulting ordering of plugins after merging:
     *
     * Given:
     *
     *   model: X -> A -> B -> D -> E
     *   profile: Y -> A -> C -> D -> F
     *
     * Result:
     *
     *   X -> Y -> A -> B -> C -> D -> E -> F
     */
    public void testShouldPreserveOrderingOfPluginsAfterProfileMerge()
    {
        PluginContainer profile = new PluginContainer();

        profile.addPlugin( createPlugin( "group", "artifact", "1.0", Collections.EMPTY_MAP ) );
        profile.addPlugin( createPlugin( "group2", "artifact2", "1.0", Collections.singletonMap( "key", "value" ) ) );

        PluginContainer model = new PluginContainer();

        model.addPlugin( createPlugin( "group3", "artifact3", "1.0", Collections.EMPTY_MAP ) );
        model.addPlugin( createPlugin( "group2", "artifact2", "1.0", Collections.singletonMap( "key2", "value2" ) ) );

        new DefaultProfileInjector().injectPlugins( profile, model );

        List results = model.getPlugins();

        assertEquals( 3, results.size() );

        Plugin result1 = (Plugin) results.get( 0 );

        assertEquals( "group3", result1.getGroupId() );
        assertEquals( "artifact3", result1.getArtifactId() );

        Plugin result2 = (Plugin) results.get( 1 );

        assertEquals( "group", result2.getGroupId() );
        assertEquals( "artifact", result2.getArtifactId() );

        Plugin result3 = (Plugin) results.get( 2 );

        assertEquals( "group2", result3.getGroupId() );
        assertEquals( "artifact2", result3.getArtifactId() );

        Xpp3Dom result3Config = (Xpp3Dom) result3.getConfiguration();

        assertNotNull( result3Config );

        assertEquals( "value", result3Config.getChild( "key" ).getValue() );
        assertEquals( "value2", result3Config.getChild( "key2" ).getValue() );
    }

    private Plugin createPlugin( String groupId, String artifactId, String version, Map configuration )
    {
        Plugin plugin = new Plugin();
        plugin.setGroupId( groupId );
        plugin.setArtifactId( artifactId );
        plugin.setVersion( version );

        Xpp3Dom config = new Xpp3Dom( "configuration" );

        if( configuration != null )
        {
            for ( Iterator it = configuration.entrySet().iterator(); it.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it.next();

                Xpp3Dom param = new Xpp3Dom( String.valueOf( entry.getKey() ) );
                param.setValue( String.valueOf( entry.getValue() ) );

                config.addChild( param );
            }
        }

        plugin.setConfiguration( config );

        return plugin;
    }

    public void testProfilePluginConfigurationShouldOverrideCollidingModelPluginConfiguration()
    {
        Plugin mPlugin = new Plugin();
        mPlugin.setGroupId( "test" );
        mPlugin.setArtifactId( "test-artifact" );
        mPlugin.setVersion( "1.0-SNAPSHOT" );

        Xpp3Dom mConfigChild = new Xpp3Dom( "test" );
        mConfigChild.setValue( "value" );

        Xpp3Dom mConfigChild2 = new Xpp3Dom( "test2" );
        mConfigChild2.setValue( "value2" );

        Xpp3Dom mConfig = new Xpp3Dom( "configuration" );
        mConfig.addChild( mConfigChild );
        mConfig.addChild( mConfigChild2 );

        mPlugin.setConfiguration( mConfig );

        Build mBuild = new Build();
        mBuild.addPlugin( mPlugin );

        Model model = new Model();
        model.setBuild( mBuild );

        Plugin pPlugin = new Plugin();
        pPlugin.setGroupId( "test" );
        pPlugin.setArtifactId( "test-artifact" );
        pPlugin.setVersion( "1.0-SNAPSHOT" );

        Xpp3Dom pConfigChild = new Xpp3Dom( "test" );
        pConfigChild.setValue( "replacedValue" );

        Xpp3Dom pConfig = new Xpp3Dom( "configuration" );
        pConfig.addChild( pConfigChild );

        pPlugin.setConfiguration( pConfig );

        BuildBase pBuild = new BuildBase();
        pBuild.addPlugin( pPlugin );

        Profile profile = new Profile();
        profile.setId( "testId" );

        profile.setBuild( pBuild );

        new DefaultProfileInjector().inject( profile, model );

        Build rBuild = model.getBuild();
        Plugin rPlugin = (Plugin) rBuild.getPlugins().get( 0 );
        Xpp3Dom rConfig = (Xpp3Dom) rPlugin.getConfiguration();

        Xpp3Dom rChild = rConfig.getChild( "test" );

        assertEquals( "replacedValue", rChild.getValue() );

        Xpp3Dom rChild2 = rConfig.getChild( "test2" );

        assertEquals( "value2", rChild2.getValue() );
    }

    public void testModelConfigShouldPersistWhenPluginHasExecConfigs()
    {
        Plugin mPlugin = new Plugin();
        mPlugin.setGroupId( "test" );
        mPlugin.setArtifactId( "test-artifact" );
        mPlugin.setVersion( "1.0-SNAPSHOT" );

        Xpp3Dom mConfigChild = new Xpp3Dom( "test" );
        mConfigChild.setValue( "value" );

        Xpp3Dom mConfigChild2 = new Xpp3Dom( "test2" );
        mConfigChild2.setValue( "value2" );

        Xpp3Dom mConfig = new Xpp3Dom( "configuration" );
        mConfig.addChild( mConfigChild );
        mConfig.addChild( mConfigChild2 );

        mPlugin.setConfiguration( mConfig );

        Build mBuild = new Build();
        mBuild.addPlugin( mPlugin );

        Model model = new Model();
        model.setBuild( mBuild );

        Plugin pPlugin = new Plugin();
        pPlugin.setGroupId( "test" );
        pPlugin.setArtifactId( "test-artifact" );
        pPlugin.setVersion( "1.0-SNAPSHOT" );

        PluginExecution pExec = new PluginExecution();
        pExec.setId("profile-injected");

        Xpp3Dom pConfigChild = new Xpp3Dom( "test" );
        pConfigChild.setValue( "replacedValue" );

        Xpp3Dom pConfig = new Xpp3Dom( "configuration" );
        pConfig.addChild( pConfigChild );

        pExec.setConfiguration( pConfig );

        pPlugin.addExecution( pExec );

        BuildBase pBuild = new BuildBase();
        pBuild.addPlugin( pPlugin );

        Profile profile = new Profile();
        profile.setId( "testId" );

        profile.setBuild( pBuild );

        new DefaultProfileInjector().inject( profile, model );

        Build rBuild = model.getBuild();
        Plugin rPlugin = (Plugin) rBuild.getPlugins().get( 0 );

        PluginExecution rExec = (PluginExecution) rPlugin.getExecutionsAsMap().get( "profile-injected" );

        assertNotNull( rExec );

        Xpp3Dom rExecConfig = (Xpp3Dom) rExec.getConfiguration();

        Xpp3Dom rChild = rExecConfig.getChild( "test" );

        assertEquals( "replacedValue", rChild.getValue() );

        Xpp3Dom rConfig = (Xpp3Dom) rPlugin.getConfiguration();

        assertNotNull( rConfig );

        Xpp3Dom rChild2 = rConfig.getChild( "test2" );

        assertEquals( "value2", rChild2.getValue() );
    }

    public void testProfileRepositoryShouldOverrideModelRepository()
    {
        Repository mRepository = new Repository();
        mRepository.setId( "testId" );
        mRepository.setName( "Test repository" );
        mRepository.setUrl( "http://www.google.com" );

        Model model = new Model();
        model.addRepository( mRepository );

        Repository pRepository = new Repository();
        pRepository.setId( "testId" );
        pRepository.setName( "Test repository" );
        pRepository.setUrl( "http://www.yahoo.com" );

        Profile profile = new Profile();
        profile.setId( "testId" );

        profile.addRepository( pRepository );

        new DefaultProfileInjector().inject( profile, model );

        Repository rRepository = (Repository) model.getRepositories().get( 0 );

        assertEquals( "http://www.yahoo.com", rRepository.getUrl() );
    }

    public void testShouldPreserveModelModulesWhenProfileHasNone()
    {
        Model model = new Model();

        model.addModule( "module1" );

        Profile profile = new Profile();
        profile.setId( "testId" );

        new DefaultProfileInjector().inject( profile, model );

        List rModules = model.getModules();

        assertEquals( 1, rModules.size() );
        assertEquals( "module1", rModules.get( 0 ) );
    }

    // NOTE: The execution-id's are important, because they are NOT in
    // alphabetical order. The trunk version of Maven currently injects
    // profiles into a TreeMap, then calls map.values(), which puts the
    // executions in alphabetical order...the WRONG order.
    public void testShouldPreserveOrderingOfProfileInjectedPluginExecutions()
    {
        Plugin profilePlugin = new Plugin();
        profilePlugin.setGroupId( "group" );
        profilePlugin.setArtifactId( "artifact" );
        profilePlugin.setVersion( "version" );

        PluginExecution exec1 = new PluginExecution();
        exec1.setId( "z" );
        profilePlugin.addExecution( exec1 );

        PluginExecution exec2 = new PluginExecution();
        exec2.setId( "y" );
        profilePlugin.addExecution( exec2 );

        BuildBase buildBase = new BuildBase();
        buildBase.addPlugin( profilePlugin );

        Profile profile = new Profile();
        profile.setBuild( buildBase );

        Plugin modelPlugin = new Plugin();
        modelPlugin.setGroupId( "group" );
        modelPlugin.setArtifactId( "artifact" );
        modelPlugin.setVersion( "version" );

        PluginExecution exec3 = new PluginExecution();
        exec3.setId( "w" );
        modelPlugin.addExecution( exec3 );

        PluginExecution exec4 = new PluginExecution();
        exec4.setId( "x" );
        modelPlugin.addExecution( exec4 );

        Build build = new Build();
        build.addPlugin( modelPlugin );

        Model model = new Model();
        model.setBuild( build );

        new DefaultProfileInjector().inject( profile, model );

        List plugins = model.getBuild().getPlugins();
        assertNotNull( plugins );
        assertEquals( 1, plugins.size() );

        Plugin plugin = (Plugin) plugins.get( 0 );

        List executions = plugin.getExecutions();
        assertNotNull( executions );
        assertEquals( 4, executions.size() );

        Iterator it = executions.iterator();

        PluginExecution e = (PluginExecution) it.next();
        assertEquals( "w", e.getId() );

        e = (PluginExecution) it.next();
        assertEquals( "x", e.getId() );

        e = (PluginExecution) it.next();
        assertEquals( "z", e.getId() );

        e = (PluginExecution) it.next();
        assertEquals( "y", e.getId() );

    }
}
