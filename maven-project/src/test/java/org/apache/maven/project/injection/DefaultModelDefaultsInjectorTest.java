package org.apache.maven.project.injection;

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

import junit.framework.TestCase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.project.artifact.TestModelDefaultsInjector;

import java.util.List;

/**
 * @author jdcasey
 */
public class DefaultModelDefaultsInjectorTest
    extends TestCase
{
    public void testShouldConstructWithNoParams()
    {
        new TestModelDefaultsInjector();
    }

    public void testShouldMergeManagedDependencyOfTypeEJBToDependencyList()
    {
        Model model = new Model();
        
        Dependency managedDep = new Dependency();
        
        managedDep.setGroupId( "group" );
        managedDep.setArtifactId( "artifact" );
        managedDep.setVersion( "1.0" );
        managedDep.setType( "ejb" );
        
        DependencyManagement depMgmt = new DependencyManagement();
        
        depMgmt.addDependency( managedDep );
        
        model.setDependencyManagement( depMgmt );
        
        Dependency dep = new Dependency();
        
        dep.setGroupId( "group" );
        dep.setArtifactId( "artifact" );
        
        // looks like groupId:artifactId:type is the minimum for identification, where
        // type is defaulted to "jar".
        dep.setType( "ejb" );
        
        model.addDependency( dep );
        
        new TestModelDefaultsInjector().injectDefaults( model );
        
        List resultingDeps = model.getDependencies();
        
        assertEquals( 1, resultingDeps.size() );
        
        Dependency result = (Dependency) resultingDeps.get( 0 );
        
        assertEquals( "1.0", result.getVersion() );
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
        def.setScope( "scope" );

        DependencyManagement depMgmt = new DependencyManagement();

        depMgmt.addDependency( def );

        model.setDependencyManagement( depMgmt );

        new TestModelDefaultsInjector().injectDefaults( model );

        List deps = model.getDependencies();
        assertEquals( 1, deps.size() );

        Dependency result = (Dependency) deps.get( 0 );
        assertEquals( def.getVersion(), result.getVersion() );
    }

    public void testShouldMergeDependencyExclusionsFromDefaultsToDependency()
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
        def.setScope( "scope" );
        
        Exclusion exc = new Exclusion();
        exc.setArtifactId( "mydep" );
        exc.setGroupId( "mygrp" );
        
        def.addExclusion( exc );
        
        DependencyManagement depMgmt = new DependencyManagement();

        depMgmt.addDependency( def );

        model.setDependencyManagement( depMgmt );

        new TestModelDefaultsInjector().injectDefaults( model );

        List deps = model.getDependencies();
        assertEquals( 1, deps.size() );

        Dependency result = (Dependency) deps.get( 0 );
        assertEquals( def.getVersion(), result.getVersion() );
        
        List resultExclusions = result.getExclusions();
        assertNotNull( resultExclusions );
        assertEquals( 1, resultExclusions.size() );
        
        Exclusion resultExclusion = (Exclusion) resultExclusions.get( 0 );
        assertEquals( "mydep", resultExclusion.getArtifactId() );
        assertEquals( "mygrp", resultExclusion.getGroupId() );
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

        new TestModelDefaultsInjector().injectDefaults( model );

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

        new TestModelDefaultsInjector().injectDefaults( model );

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

        DependencyManagement depMgmt = new DependencyManagement();

        depMgmt.addDependency( def );

        model.setDependencyManagement( depMgmt );

        new TestModelDefaultsInjector().injectDefaults( model );

        List deps = model.getDependencies();
        assertEquals( 1, deps.size() );
    }

    public void testShouldNotMergeDefaultPropertiesWhenDependencySuppliesProperties()
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

        new TestModelDefaultsInjector().injectDefaults( model );

        List deps = model.getDependencies();
        assertEquals( 1, deps.size() );
    }

    public void testShouldMergeDefaultScopeWhenDependencyDoesntSupplyScope()
    {
        Model model = new Model();

        Dependency dep = new Dependency();
        dep.setGroupId( "myGroup" );
        dep.setArtifactId( "myArtifact" );
        dep.setVersion( "1.0.1" );
        dep.setScope( "scope" );

        model.addDependency( dep );

        Dependency def = new Dependency();
        def.setGroupId( dep.getGroupId() );
        def.setArtifactId( dep.getArtifactId() );

        DependencyManagement depMgmt = new DependencyManagement();

        depMgmt.addDependency( def );

        model.setDependencyManagement( depMgmt );

        new TestModelDefaultsInjector().injectDefaults( model );

        List deps = model.getDependencies();
        assertEquals( 1, deps.size() );

        Dependency result = (Dependency) deps.get( 0 );

        assertEquals( "scope", result.getScope() );
    }

    public void testShouldNotMergeDefaultScopeWhenDependencySuppliesScope()
    {
        Model model = new Model();

        Dependency dep = new Dependency();
        dep.setGroupId( "myGroup" );
        dep.setArtifactId( "myArtifact" );
        dep.setVersion( "1.0.1" );
        dep.setScope( "scope" );

        model.addDependency( dep );

        Dependency def = new Dependency();
        def.setGroupId( dep.getGroupId() );
        def.setArtifactId( dep.getArtifactId() );
        def.setScope( "default" );

        DependencyManagement depMgmt = new DependencyManagement();

        depMgmt.addDependency( def );

        model.setDependencyManagement( depMgmt );

        new TestModelDefaultsInjector().injectDefaults( model );

        List deps = model.getDependencies();
        assertEquals( 1, deps.size() );

        Dependency result = (Dependency) deps.get( 0 );
        assertEquals( "scope", result.getScope() );
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
        new TestModelDefaultsInjector().injectDefaults( model );
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
