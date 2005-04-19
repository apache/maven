package org.apache.maven.tools.repoclean.translate;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.tools.repoclean.report.DummyReporter;
import org.apache.maven.tools.repoclean.report.Reporter;
import org.codehaus.plexus.PlexusTestCase;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class PomV3ToV4TranslatorTest
    extends PlexusTestCase
{

    public void testShouldConvertScopePropertyToDependencyScope()
        throws Exception
    {
        PomV3ToV4Translator translator = (PomV3ToV4Translator) lookup( PomV3ToV4Translator.ROLE );

        Reporter reporter = new DummyReporter();

        org.apache.maven.model.v3_0_0.Dependency v3Dep = new org.apache.maven.model.v3_0_0.Dependency();
        v3Dep.setGroupId( "testGroup" );
        v3Dep.setArtifactId( "testArtifact" );
        v3Dep.setVersion( "1.0" );
        v3Dep.addProperty( "scope", "test" );

        org.apache.maven.model.v3_0_0.Model v3Model = new org.apache.maven.model.v3_0_0.Model();
        v3Model.addDependency( v3Dep );

        Model result = translator.translate( v3Model, reporter );
        assertEquals( "test", ( (Dependency) result.getDependencies().get( 0 ) ).getScope() );

    }

    public void testShouldConvertDependencyWithTypePluginToBuildPluginEntry()
        throws Exception
    {
        PomV3ToV4Translator translator = (PomV3ToV4Translator) lookup( PomV3ToV4Translator.ROLE );

        Reporter reporter = new DummyReporter();

        org.apache.maven.model.v3_0_0.Dependency v3Dep = new org.apache.maven.model.v3_0_0.Dependency();
        v3Dep.setGroupId( "testGroup" );
        v3Dep.setArtifactId( "testArtifact" );
        v3Dep.setVersion( "1.0" );
        v3Dep.setType( "plugin" );

        org.apache.maven.model.v3_0_0.Model v3Model = new org.apache.maven.model.v3_0_0.Model();
        v3Model.addDependency( v3Dep );

        Model result = translator.translate( v3Model, reporter );

        Build build = result.getBuild();

        Plugin plugin = (Plugin) build.getPlugins().get( 0 );

        assertEquals( "testGroup", plugin.getGroupId() );
        assertEquals( "testArtifact", plugin.getArtifactId() );
        assertEquals( "1.0", plugin.getVersion() );
    }

    public void testShouldConvertDependencyWithTypePluginAndGroupMavenToBuildPluginEntryWithOAMPluginsGroup()
        throws Exception
    {
        PomV3ToV4Translator translator = (PomV3ToV4Translator) lookup( PomV3ToV4Translator.ROLE );

        Reporter reporter = new DummyReporter();

        org.apache.maven.model.v3_0_0.Dependency v3Dep = new org.apache.maven.model.v3_0_0.Dependency();
        v3Dep.setGroupId( "maven" );
        v3Dep.setArtifactId( "testArtifact" );
        v3Dep.setVersion( "1.0" );
        v3Dep.setType( "plugin" );

        org.apache.maven.model.v3_0_0.Model v3Model = new org.apache.maven.model.v3_0_0.Model();
        v3Model.addDependency( v3Dep );

        Model result = translator.translate( v3Model, reporter );

        Build build = result.getBuild();

        Plugin plugin = (Plugin) build.getPlugins().get( 0 );

        assertEquals( "org.apache.maven.plugins", plugin.getGroupId() );
        assertEquals( "testArtifact", plugin.getArtifactId() );
        assertEquals( "1.0", plugin.getVersion() );
    }

}
