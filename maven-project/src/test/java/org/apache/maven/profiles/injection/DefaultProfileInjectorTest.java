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

        model = new DefaultProfileInjector().inject( profile, model );

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

        model = new DefaultProfileInjector().inject( profile, model );

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

        model = new DefaultProfileInjector().inject( profile, model );

        Repository rRepository = model.getRepositories().get( 0 );

        assertEquals( "http://www.yahoo.com", rRepository.getUrl() );
    }

    /*
    public void testShouldPreserveModelModulesWhenProfileHasNone()
    {
        Model model = new Model();

        model.addModule( "module1" );

        Profile profile = new Profile();
        profile.setId( "testId" );

        model = new DefaultProfileInjector().inject( profile, model );

        List rModules = model.getModules();

        assertEquals( 1, rModules.size() );
        assertEquals( "module1", rModules.get( 0 ) );
    }

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

        model = new DefaultProfileInjector().inject( profile, model );

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
    */
}
