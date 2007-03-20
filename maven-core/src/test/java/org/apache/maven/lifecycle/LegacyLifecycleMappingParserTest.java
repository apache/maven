package org.apache.maven.lifecycle;

import org.apache.maven.lifecycle.binding.LegacyLifecycleMappingParser;
import org.apache.maven.lifecycle.binding.LegacyLifecycleParsingTestComponent;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.lifecycle.model.BuildBinding;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.codehaus.plexus.PlexusTestCase;

import java.util.List;

public class LegacyLifecycleMappingParserTest
    extends PlexusTestCase
{

    private LegacyLifecycleParsingTestComponent testComponent;

    private LifecycleMapping testMapping;
    
    private LifecycleMapping testMapping2;
    
    private LegacyLifecycleMappingParser parser;

    public void setUp()
        throws Exception
    {
        super.setUp();
        
        parser = (LegacyLifecycleMappingParser) lookup( LegacyLifecycleMappingParser.ROLE, "default" );

        testComponent = (LegacyLifecycleParsingTestComponent) lookup( LegacyLifecycleParsingTestComponent.ROLE, "default" );
        testMapping = (LifecycleMapping) lookup( LifecycleMapping.ROLE, "test-mapping" );
        testMapping2 = (LifecycleMapping) lookup( LifecycleMapping.ROLE, "test-mapping2" );
    }

    public void tearDown()
        throws Exception
    {
        release( testComponent );
        release( testMapping );
        release( testMapping2 );

        super.tearDown();
    }

    public void testParseDefaultMappings_UsingExistingDefaultMappings()
        throws LifecycleSpecificationException
    {
        List lifecycles = testComponent.getLifecycles();
        LifecycleBindings bindings = parser.parseDefaultMappings( lifecycles );

        //        <clean>org.apache.maven.plugins:maven-clean-plugin:clean</clean>
        List cleanPhase = bindings.getCleanBinding().getClean().getBindings();
        assertEquals( 1, cleanPhase.size() );

        MojoBinding binding = (MojoBinding) cleanPhase.get( 0 );
        assertMojo( "org.apache.maven.plugins", "maven-clean-plugin", "clean", binding );

        //        <site>org.apache.maven.plugins:maven-site-plugin:site</site>
        List sitePhase = bindings.getSiteBinding().getSite().getBindings();
        assertEquals( 1, sitePhase.size() );

        binding = (MojoBinding) sitePhase.get( 0 );
        assertMojo( "org.apache.maven.plugins", "maven-site-plugin", "site", binding );

        //      <site-deploy>org.apache.maven.plugins:maven-site-plugin:deploy</site-deploy>
        List siteDeployPhase = bindings.getSiteBinding().getSiteDeploy().getBindings();
        assertEquals( 1, siteDeployPhase.size() );

        binding = (MojoBinding) siteDeployPhase.get( 0 );
        assertMojo( "org.apache.maven.plugins", "maven-site-plugin", "deploy", binding );
    }

    private void assertMojo( String groupId, String artifactId, String goal, MojoBinding binding )
    {
        assertEquals( groupId, binding.getGroupId() );
        assertEquals( artifactId, binding.getArtifactId() );
        assertEquals( goal, binding.getGoal() );
    }

    public void testParseMappings_SparselyPopulatedMappings()
        throws LifecycleSpecificationException
    {
        LifecycleBindings bindings = parser.parseMappings( testMapping, "test-mapping" );

        BuildBinding bb = bindings.getBuildBinding();
        assertNotNull( bb );

        //      <phases>
        //        <package>org.apache.maven.plugins:maven-site-plugin:attach-descriptor</package>
        //        <install>org.apache.maven.plugins:maven-install-plugin:install</install>
        //        <deploy>org.apache.maven.plugins:maven-deploy-plugin:deploy</deploy>
        //      </phases>
        //      <optional-mojos>
        //        <optional-mojo>org.apache.maven.plugins:maven-site-plugin:attach-descriptor</optional-mojo>
        //      </optional-mojos>
        List mojos = bb.getCreatePackage().getBindings();

        assertEquals( 1, mojos.size() );
        assertMojo( "org.apache.maven.plugins", "maven-site-plugin", "attach-descriptor", (MojoBinding) mojos.get( 0 ) );
        assertTrue( ( (MojoBinding) mojos.get( 0 ) ).isOptional() );

        mojos = bb.getInstall().getBindings();

        assertEquals( 1, mojos.size() );
        assertMojo( "org.apache.maven.plugins", "maven-install-plugin", "install", (MojoBinding) mojos.get( 0 ) );
        assertFalse( ( (MojoBinding) mojos.get( 0 ) ).isOptional() );

        mojos = bb.getDeploy().getBindings();

        assertEquals( 1, mojos.size() );
        assertMojo( "org.apache.maven.plugins", "maven-deploy-plugin", "deploy", (MojoBinding) mojos.get( 0 ) );
        assertFalse( ( (MojoBinding) mojos.get( 0 ) ).isOptional() );
    }

    public void testParseMappings_MappingsWithTwoBindingsInOnePhase()
        throws LifecycleSpecificationException
    {
        LifecycleBindings bindings = parser.parseMappings( testMapping2, "test-mapping2" );

        BuildBinding bb = bindings.getBuildBinding();
        assertNotNull( bb );

        //      <phases>
        //        <package>org.apache.maven.plugins:maven-site-plugin:attach-descriptor</package>
        //        <install>org.apache.maven.plugins:maven-install-plugin:install</install>
        //        <deploy>org.apache.maven.plugins:maven-deploy-plugin:deploy</deploy>
        //      </phases>
        //      <optional-mojos>
        //        <optional-mojo>org.apache.maven.plugins:maven-site-plugin:attach-descriptor</optional-mojo>
        //      </optional-mojos>
        List mojos = bb.getCreatePackage().getBindings();

        assertEquals( 2, mojos.size() );
        assertMojo( "org.apache.maven.plugins", "maven-site-plugin", "attach-descriptor", (MojoBinding) mojos.get( 0 ) );
        assertTrue( ( (MojoBinding) mojos.get( 0 ) ).isOptional() );
        assertMojo( "org.apache.maven.plugins", "maven-clean-plugin", "clean", (MojoBinding) mojos.get( 1 ) );

        mojos = bb.getInstall().getBindings();

        assertEquals( 1, mojos.size() );
        assertMojo( "org.apache.maven.plugins", "maven-install-plugin", "install", (MojoBinding) mojos.get( 0 ) );
        assertFalse( ( (MojoBinding) mojos.get( 0 ) ).isOptional() );

        mojos = bb.getDeploy().getBindings();

        assertEquals( 1, mojos.size() );
        assertMojo( "org.apache.maven.plugins", "maven-deploy-plugin", "deploy", (MojoBinding) mojos.get( 0 ) );
        assertFalse( ( (MojoBinding) mojos.get( 0 ) ).isOptional() );
    }

}
