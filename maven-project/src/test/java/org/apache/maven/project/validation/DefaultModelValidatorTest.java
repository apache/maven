package org.apache.maven.project.validation;

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

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.AbstractMavenProjectTestCase;
import org.codehaus.plexus.util.ReaderFactory;

import java.io.Reader;
import java.util.List;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class DefaultModelValidatorTest
    extends AbstractMavenProjectTestCase
{
    private Model model;

    private ModelValidator validator;

    public void testMissingModelVersion()
        throws Exception
    {
        ModelValidationResult result = validate( "missing-modelVersion-pom.xml" );

        assertEquals( 1, result.getMessageCount() );

        assertEquals( "'modelVersion' is missing.", result.getMessage( 0 ) );
    }

    public void testMissingArtifactId()
        throws Exception
    {
        ModelValidationResult result = validate( "missing-artifactId-pom.xml" );

        assertEquals( 1, result.getMessageCount() );

        assertEquals( "'artifactId' is missing.", result.getMessage( 0 ) );
    }

    public void testMissingGroupId()
        throws Exception
    {
        ModelValidationResult result = validate( "missing-groupId-pom.xml" );

        assertEquals( 1, result.getMessageCount() );

        assertEquals( "'groupId' is missing.", result.getMessage( 0 ) );
    }

    public void testInvalidIds()
        throws Exception
    {
        ModelValidationResult result = validate( "invalid-ids-pom.xml" );

        assertEquals( 2, result.getMessageCount() );

        assertEquals( "'groupId' with value 'o/a/m' does not match a valid id pattern.", result.getMessage( 0 ) );

        assertEquals( "'artifactId' with value 'm$-do$' does not match a valid id pattern.", result.getMessage( 1 ) );
    }

    public void testMissingType()
        throws Exception
    {
        ModelValidationResult result = validate( "missing-type-pom.xml" );

        assertEquals( 1, result.getMessageCount() );

        assertEquals( "'packaging' is missing.", result.getMessage( 0 ) );
    }

    public void testMissingVersion()
        throws Exception
    {
        ModelValidationResult result = validate( "missing-version-pom.xml" );

        assertEquals( 1, result.getMessageCount() );

        assertEquals( "'version' is missing.", result.getMessage( 0 ) );
    }

    public void testInvalidAggregatorPackaging()
        throws Exception
    {
        ModelValidationResult result = validate( "invalid-aggregator-packaging-pom.xml" );

        assertEquals( 1, result.getMessageCount() );

        assertTrue( result.getMessage( 0 ).indexOf( "Aggregator projects require 'pom' as packaging." ) > -1 );
    }

    public void testMissingDependencyArtifactId()
        throws Exception
    {
        ModelValidationResult result = validate( "missing-dependency-artifactId-pom.xml" );

        assertEquals( 1, result.getMessageCount() );

        assertTrue( result.getMessage( 0 ).indexOf( "'dependencies.dependency.artifactId' is missing." ) > -1 );
    }

    public void testMissingDependencyGroupId()
        throws Exception
    {
        ModelValidationResult result = validate( "missing-dependency-groupId-pom.xml" );

        assertEquals( 1, result.getMessageCount() );

        assertTrue( result.getMessage( 0 ).indexOf( "'dependencies.dependency.groupId' is missing." ) > -1 );
    }

    public void testMissingDependencyVersion()
        throws Exception
    {
        ModelValidationResult result = validate( "missing-dependency-version-pom.xml" );

        assertEquals( 1, result.getMessageCount() );

        assertTrue( result.getMessage( 0 ).indexOf( "'dependencies.dependency.version' is missing" ) > -1 );
    }

    public void testMissingDependencyManagementArtifactId()
        throws Exception
    {
        ModelValidationResult result = validate( "missing-dependency-mgmt-artifactId-pom.xml" );

        assertEquals( 1, result.getMessageCount() );

        assertTrue( result.getMessage( 0 ).indexOf(
            "'dependencyManagement.dependencies.dependency.artifactId' is missing." ) > -1 );
    }

    public void testMissingDependencyManagementGroupId()
        throws Exception
    {
        ModelValidationResult result = validate( "missing-dependency-mgmt-groupId-pom.xml" );

        assertEquals( 1, result.getMessageCount() );

        assertTrue( result.getMessage( 0 ).indexOf(
            "'dependencyManagement.dependencies.dependency.groupId' is missing." ) > -1 );
    }

    public void testMissingAll()
        throws Exception
    {
        ModelValidationResult result = validate( "missing-1-pom.xml" );

        assertEquals( 4, result.getMessageCount() );

        List messages = result.getMessages();

        assertTrue( messages.contains( "\'modelVersion\' is missing." ) );
        assertTrue( messages.contains( "\'groupId\' is missing." ) );
        assertTrue( messages.contains( "\'artifactId\' is missing." ) );
        assertTrue( messages.contains( "\'version\' is missing." ) );
        // type is inherited from the super pom
    }

    public void testMissingPluginArtifactId()
        throws Exception
    {
        ModelValidationResult result = validate( "missing-plugin-artifactId-pom.xml" );

        assertEquals( 1, result.getMessageCount() );

        assertEquals( "'build.plugins.plugin.artifactId' is missing.", result.getMessage( 0 ) );
    }

    public void testMissingRepositoryId()
        throws Exception
    {
        ModelValidationResult result = validate( "missing-repository-id-pom.xml" );

        assertEquals( 2, result.getMessageCount() );

        assertEquals( "'repositories.repository.id' is missing.", result.getMessage( 0 ) );

        assertEquals( "'repositories.repository.url' is missing.", result.getMessage( 1 ) );
//
//        assertEquals( "'pluginRepositories.pluginRepository.id' is missing.", result.getMessage( 2 ) );
//
//        assertEquals( "'pluginRepositories.pluginRepository.url' is missing.", result.getMessage( 3 ) );
    }

    public void testMissingResourceDirectory()
        throws Exception
    {
        ModelValidationResult result = validate( "missing-resource-directory-pom.xml" );

        assertEquals( 2, result.getMessageCount() );

        assertEquals( "'build.resources.resource.directory' is missing.", result.getMessage( 0 ) );

        assertEquals( "'build.testResources.testResource.directory' is missing.", result.getMessage( 1 ) );
    }

    private ModelValidationResult validate( String testName )
        throws Exception
    {
        Reader input = ReaderFactory.newXmlReader( getFileForClasspathResource( "validation/" + testName ) );

        MavenXpp3Reader reader = new MavenXpp3Reader();

        validator = (ModelValidator) lookup( ModelValidator.ROLE );

        model = reader.read( input );

        ModelValidationResult result = validator.validate( model );

        assertNotNull( result );

        input.close();

        return result;
    }
}
