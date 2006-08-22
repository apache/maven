package org.apache.maven.model.converter;

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

import junit.framework.Assert;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;
import org.codehaus.plexus.PlexusTestCase;

import java.util.Arrays;

public class PomV3ToV4TranslatorTest
    extends PlexusTestCase
{

    private ModelConverter translator;

    private org.apache.maven.model.v3_0_0.Dependency v3Dep;

    private org.apache.maven.model.v3_0_0.Model v3Model;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        translator = (ModelConverter) lookup( ModelConverter.ROLE );

        v3Dep = new org.apache.maven.model.v3_0_0.Dependency();
        v3Dep.setGroupId( "testGroup" );
        v3Dep.setArtifactId( "testArtifact" );
        v3Dep.setVersion( "1.0" );

        v3Model = new org.apache.maven.model.v3_0_0.Model();
        v3Model.setBuild( new org.apache.maven.model.v3_0_0.Build() );
    }

    public void testConvertedEmptyResourceDirectory()
        throws PomTranslationException
    {
        org.apache.maven.model.v3_0_0.Resource v3Resource = new org.apache.maven.model.v3_0_0.Resource();
        v3Resource.setIncludes( Arrays.asList( new String[]{"**/*.properties"} ) );
        v3Model.getBuild().addResource( v3Resource );

        Model result = translator.translate( v3Model );
        Resource resource = (Resource) result.getBuild().getResources().get( 0 );
        Assert.assertEquals( "check directory of v3Resource", ".", resource.getDirectory() );
    }

    public void testShouldConvertScopePropertyToDependencyScope()
        throws Exception
    {
        v3Dep.addProperty( "scope", "test" );

        v3Model.addDependency( v3Dep );

        Model result = translator.translate( v3Model );
        Assert.assertEquals( "test", ( (Dependency) result.getDependencies().get( 0 ) ).getScope() );

    }

    public void testShouldConvertOptionalPropertyToDependencyOptional()
        throws Exception
    {
        v3Dep.addProperty( "optional", "true" );

        v3Model.addDependency( v3Dep );

        Model result = translator.translate( v3Model );
        Assert.assertTrue( ( (Dependency) result.getDependencies().get( 0 ) ).isOptional() );

        v3Dep.addProperty( "optional", "TRUE" );

        v3Model.addDependency( v3Dep );

        result = translator.translate( v3Model );
        Assert.assertTrue( ( (Dependency) result.getDependencies().get( 0 ) ).isOptional() );
    }

    public void testDontBreakWithMalformedOptionalProperty()
        throws Exception
    {
        v3Dep.addProperty( "optional", "xxx" );

        v3Model.addDependency( v3Dep );

        Model result = translator.translate( v3Model );
        Assert.assertFalse( ( (Dependency) result.getDependencies().get( 0 ) ).isOptional() );

        v3Dep.addProperty( "optional", "" );

        v3Model.addDependency( v3Dep );

        result = translator.translate( v3Model );
        Assert.assertFalse( ( (Dependency) result.getDependencies().get( 0 ) ).isOptional() );
    }

    public void testShouldConvertDependencyWithTypePluginToBuildPluginEntry()
        throws Exception
    {
        v3Dep.setType( "plugin" );

        v3Model.addDependency( v3Dep );

        Model result = translator.translate( v3Model );

        Build build = result.getBuild();

        Plugin plugin = (Plugin) build.getPlugins().get( 0 );

        Assert.assertEquals( "testGroup", plugin.getGroupId() );
        Assert.assertEquals( "testArtifact", plugin.getArtifactId() );
        Assert.assertEquals( "1.0", plugin.getVersion() );

        Assert.assertEquals( "check one dependency", 1, result.getDependencies().size() );
        Dependency dep = (Dependency) result.getDependencies().get( 0 );
        Assert.assertEquals( "junit", dep.getGroupId() );
        Assert.assertEquals( "junit", dep.getArtifactId() );
        Assert.assertEquals( "3.8.2", dep.getVersion() );
        Assert.assertEquals( "test", dep.getScope() );
    }

    public void testShouldConvertDependencyWithTypePluginAndGroupMavenToBuildPluginEntryWithOAMPluginsGroup()
        throws Exception
    {
        v3Dep.setGroupId( "maven" );
        v3Dep.setType( "plugin" );

        v3Model.addDependency( v3Dep );

        Model result = translator.translate( v3Model );

        Build build = result.getBuild();

        Plugin plugin = (Plugin) build.getPlugins().get( 0 );

        Assert.assertEquals( "org.apache.maven.plugins", plugin.getGroupId() );
        Assert.assertEquals( "testArtifact", plugin.getArtifactId() );
        Assert.assertEquals( "1.0", plugin.getVersion() );

        Assert.assertEquals( "check one dependencies", 1, result.getDependencies().size() );
        Dependency dep = (Dependency) result.getDependencies().get( 0 );
        Assert.assertEquals( "junit", dep.getGroupId() );
        Assert.assertEquals( "junit", dep.getArtifactId() );
        Assert.assertEquals( "3.8.2", dep.getVersion() );
        Assert.assertEquals( "test", dep.getScope() );
    }

}
