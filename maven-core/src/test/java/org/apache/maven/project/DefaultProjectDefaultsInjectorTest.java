package org.apache.maven.project;

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

import junit.framework.TestCase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.project.injection.DefaultModelDefaultsInjector;

import java.util.List;
import java.util.Properties;

/**
 * @author jdcasey
 */
public class DefaultProjectDefaultsInjectorTest
extends TestCase
{

    public void testShouldConstructWithNoParams()
    {
        new DefaultModelDefaultsInjector();
    }

    public void testShouldSucceedInMergingDependencyWithDependency()
    {
        Model model = new Model();

        Dependency dep = new Dependency();
        dep.setGroupId( "myGroup" );
        dep.setArtifactId( "myArtifact" );

        model.addDependency( dep );

        Dependency def = new Dependency();
        def.setGroupId( dep.getGroupId() );
        def.setArtifactId( dep.getArtifactId() );
        def.setVersion( "1.0.1" );
        def.setFile( "file" );

        DependencyManagement depMgmt = new DependencyManagement();

        depMgmt.addDependency( def );

        model.setDependencyManagement( depMgmt );

        new DefaultModelDefaultsInjector().injectDefaults( model );

        List deps = model.getDependencies();
        assertEquals( 1, deps.size() );

        Dependency result = (Dependency) deps.get( 0 );
        assertEquals( def.getVersion(), result.getVersion() );
    }

    public void testShouldMergeDefaultUrlAndArtifactWhenDependencyDoesntSupplyVersion()
    {
        Model model = new Model();

        Dependency dep = new Dependency();
        dep.setGroupId( "myGroup" );
        dep.setArtifactId( "myArtifact" );

        model.addDependency( dep );

        Dependency def = new Dependency();
        def.setGroupId( dep.getGroupId() );
        def.setArtifactId( dep.getArtifactId() );
        def.setVersion( "1.0.1" );

        DependencyManagement depMgmt = new DependencyManagement();

        depMgmt.addDependency( def );

        model.setDependencyManagement( depMgmt );

        new DefaultModelDefaultsInjector().injectDefaults( model );

        List deps = model.getDependencies();
        assertEquals( 1, deps.size() );

        Dependency result = (Dependency) deps.get( 0 );
        assertEquals( def.getVersion(), result.getVersion() );
    }

    public void testShouldNotMergeDefaultUrlOrArtifactWhenDependencySuppliesVersion()
    {
        Model model = new Model();

        Dependency dep = new Dependency();
        dep.setGroupId( "myGroup" );
        dep.setArtifactId( "myArtifact" );
        dep.setVersion( "1.0.1" );

        model.addDependency( dep );

        Dependency def = new Dependency();
        def.setGroupId( dep.getGroupId() );
        def.setArtifactId( dep.getArtifactId() );

        DependencyManagement depMgmt = new DependencyManagement();

        depMgmt.addDependency( def );

        model.setDependencyManagement( depMgmt );

        new DefaultModelDefaultsInjector().injectDefaults( model );

        List deps = model.getDependencies();
        assertEquals( 1, deps.size() );

        Dependency result = (Dependency) deps.get( 0 );
        assertEquals( dep.getVersion(), result.getVersion() );
    }

    public void testShouldMergeDefaultPropertiesWhenDependencyDoesntSupplyProperties()
    {
        Model model = new Model();

        Dependency dep = new Dependency();
        dep.setGroupId( "myGroup" );
        dep.setArtifactId( "myArtifact" );
        dep.setVersion( "1.0.1" );

        model.addDependency( dep );

        Dependency def = new Dependency();
        def.setGroupId( dep.getGroupId() );
        def.setArtifactId( dep.getArtifactId() );

        Properties props = new Properties();
        props.setProperty( "test", "value" );

        def.setProperties( props );

        DependencyManagement depMgmt = new DependencyManagement();

        depMgmt.addDependency( def );

        model.setDependencyManagement( depMgmt );

        new DefaultModelDefaultsInjector().injectDefaults( model );

        List deps = model.getDependencies();
        assertEquals( 1, deps.size() );

        Dependency result = (Dependency) deps.get( 0 );

        assertEquals( "value", result.getProperties().getProperty( "test" ) );
    }

    public void testShouldNotMergeDefaultPropertiesWhenDependencySuppliesProperties()
    {
        Model model = new Model();

        Dependency dep = new Dependency();
        dep.setGroupId( "myGroup" );
        dep.setArtifactId( "myArtifact" );
        dep.setVersion( "1.0.1" );

        Properties props = new Properties();
        props.setProperty( "test", "value" );

        dep.setProperties( props );

        model.addDependency( dep );

        Dependency def = new Dependency();
        def.setGroupId( dep.getGroupId() );
        def.setArtifactId( dep.getArtifactId() );

        Properties props2 = new Properties();
        props2.setProperty( "test", "value2" );

        def.setProperties( props2 );

        DependencyManagement depMgmt = new DependencyManagement();

        depMgmt.addDependency( def );

        model.setDependencyManagement( depMgmt );

        new DefaultModelDefaultsInjector().injectDefaults( model );

        List deps = model.getDependencies();
        assertEquals( 1, deps.size() );

        Dependency result = (Dependency) deps.get( 0 );
        assertEquals( "value", result.getProperties().getProperty( "test" ) );
    }

    public void testShouldMergeDefaultFileWhenDependencyDoesntSupplyFile()
    {
        Model model = new Model();

        Dependency dep = new Dependency();
        dep.setGroupId( "myGroup" );
        dep.setArtifactId( "myArtifact" );
        dep.setVersion( "1.0.1" );
        dep.setFile( "file" );

        model.addDependency( dep );

        Dependency def = new Dependency();
        def.setGroupId( dep.getGroupId() );
        def.setArtifactId( dep.getArtifactId() );

        DependencyManagement depMgmt = new DependencyManagement();

        depMgmt.addDependency( def );

        model.setDependencyManagement( depMgmt );

        new DefaultModelDefaultsInjector().injectDefaults( model );

        List deps = model.getDependencies();
        assertEquals( 1, deps.size() );

        Dependency result = (Dependency) deps.get( 0 );

        assertEquals( "file", result.getFile() );
    }

    public void testShouldNotMergeDefaultFileWhenDependencySuppliesFile()
    {
        Model model = new Model();

        Dependency dep = new Dependency();
        dep.setGroupId( "myGroup" );
        dep.setArtifactId( "myArtifact" );
        dep.setVersion( "1.0.1" );
        dep.setFile( "file" );

        model.addDependency( dep );

        Dependency def = new Dependency();
        def.setGroupId( dep.getGroupId() );
        def.setArtifactId( dep.getArtifactId() );
        def.setFile( "default" );

        DependencyManagement depMgmt = new DependencyManagement();

        depMgmt.addDependency( def );

        model.setDependencyManagement( depMgmt );

        new DefaultModelDefaultsInjector().injectDefaults( model );

        List deps = model.getDependencies();
        assertEquals( 1, deps.size() );

        Dependency result = (Dependency) deps.get( 0 );
        assertEquals( "file", result.getFile() );
    }

    public void testShouldRejectDependencyWhereNoVersionIsFoundAfterDefaultsInjection()
    {
        Model model = new Model();

        Dependency dep = new Dependency();
        dep.setGroupId( "myGroup" );
        dep.setArtifactId( "myArtifact" );

        model.addDependency( dep );

        Dependency def = new Dependency();
        def.setGroupId( dep.getGroupId() );
        def.setArtifactId( dep.getArtifactId() );

        DependencyManagement depMgmt = new DependencyManagement();

        depMgmt.addDependency( def );

        model.setDependencyManagement( depMgmt );

//        try
//        {
        new DefaultModelDefaultsInjector().injectDefaults( model );
        Dependency dependency = (Dependency) model.getDependencies().get( 0 );
        assertNull( "check version is null", dependency.getVersion() );
//            fail("Should fail to validate dependency without a version.");
//        }
//        catch ( IllegalStateException e )
//        {
//            // should throw when it detects a missing version in the test dependency.
//        }
    }

}
