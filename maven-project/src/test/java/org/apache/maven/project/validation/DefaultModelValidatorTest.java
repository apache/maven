package org.apache.maven.project.validation;

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

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProjectTestCase;

import java.io.FileReader;
import java.io.Reader;
import java.util.List;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class DefaultModelValidatorTest
    extends MavenProjectTestCase
{
    private Model model;

    private ModelValidator validator;
    
    public void testMissingModelVersion() throws Exception
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

    public void testMissingType()
        throws Exception
    {
        ModelValidationResult result = validate( "missing-type-pom.xml" );

        assertEquals( 1, result.getMessageCount() );

        assertEquals( "'packaging' is empty.", result.getMessage( 0 ) );
    }

    public void testMissingVersion()
        throws Exception
    {
        ModelValidationResult result = validate( "missing-version-pom.xml" );

        assertEquals( 1, result.getMessageCount() );

        assertEquals( "'version' is missing.", result.getMessage( 0 ) );
    }

    public void testMissingAll()
        throws Exception
    {
        ModelValidationResult result = validate( "missing-1-pom.xml" );

        assertEquals( 4, result.getMessageCount() );

        List messages = result.getMessages();
        
        assertTrue( messages.contains("\'modelVersion\' is missing."));
        assertTrue( messages.contains("\'groupId\' is missing."));
        assertTrue( messages.contains("\'artifactId\' is missing."));
        assertTrue( messages.contains("\'version\' is missing."));
        // type is inherited from the super pom
    }

    private ModelValidationResult validate( String testName )
        throws Exception
    {
        Reader input = new FileReader( getFileForClasspathResource( "/validation/" + testName ) );

        MavenXpp3Reader reader = new MavenXpp3Reader();

        validator = (ModelValidator) lookup( ModelValidator.ROLE );

        model = reader.read( input );

        ModelValidationResult result = validator.validate( model );

        assertNotNull( result );

        input.close();

        return result;
    }
}
