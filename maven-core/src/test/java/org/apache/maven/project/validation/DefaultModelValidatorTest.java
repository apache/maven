package org.apache.maven.project.validation;

/*
 * LICENSE
 */

import org.apache.maven.MavenTestCase;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.FileReader;
import java.io.Reader;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class DefaultModelValidatorTest
    extends MavenTestCase
{
    private Model model;

    private ModelValidator validator;

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
    
        assertEquals( "'type' is empty.", result.getMessage( 0 ) );
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
    
        assertEquals( 3, result.getMessageCount() );

        assertEquals( "'groupId' is missing.", result.getMessage( 0 ) );
        assertEquals( "'artifactId' is missing.", result.getMessage( 1 ) );
        // type is inherited from the super pom
        assertEquals( "'version' is missing.", result.getMessage( 2 ) );
    }

    private ModelValidationResult validate( String testName )
        throws Exception
    {
        Reader input = new FileReader( getTestFile( "src/test/resources/validation/" + testName ) );

        MavenXpp3Reader reader = new MavenXpp3Reader();

        validator = (ModelValidator) lookup( ModelValidator.ROLE );

        model = reader.read( input );

        ModelValidationResult result = validator.validate( model );

        assertNotNull( result );

        input.close();

        return result;
    }
}
