package org.apache.maven.integrationtests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.integrationtests.AbstractMavenIntegrationTestCase;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

public class MavenITmng1493NonStandardModulePomNames
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng1493NonStandardModulePomNames()
        throws InvalidVersionSpecificationException
    {
        super( "[2.1-SNAPSHOT,)" ); // 2.1+
    }

    public void testitMNG1493 ()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-1493-nonstandardModulePomNames" );

        Verifier verifier;

        verifier = new Verifier( testDir.getAbsolutePath() );

        verifier.executeGoal( "initialize" );

        /*
         * This is the simplest way to check a build
         * succeeded. It is also the simplest way to create
         * an IT test: make the build pass when the test
         * should pass, and make the build fail when the
         * test should fail. There are other methods
         * supported by the verifier. They can be seen here:
         * http://maven.apache.org/shared/maven-verifier/apidocs/index.html
         */
        verifier.verifyErrorFreeLog();
    }
}
