package org.apache.maven.lifecycle;

import org.apache.maven.lifecycle.model.CleanBinding;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.model.Phase;
import org.codehaus.plexus.PlexusTestCase;

import java.util.List;

public class ClassLoaderXmlBindingLoaderTest
    extends PlexusTestCase
{

    public void testBeanAccess_ParseSingleCleanBinding()
        throws LifecycleLoaderException, LifecycleSpecificationException
    {
        LifecycleBindings bindings = new ClassLoaderXmlBindingLoader( "single-clean-mapping.xml" ).getBindings();

        CleanBinding cleanBinding = bindings.getCleanBinding();
        assertNotNull( cleanBinding );

        Phase preClean = cleanBinding.getPreClean();
        assertNotNull( preClean );

        List mojos = preClean.getBindings();
        assertNotNull( mojos );
        assertEquals( 1, mojos.size() );

        MojoBinding mojo = (MojoBinding) mojos.get( 0 );
        assertEquals( "group", mojo.getGroupId() );
        assertEquals( "artifact", mojo.getArtifactId() );
        assertNull( mojo.getVersion() );
        assertEquals( "goalOne", mojo.getGoal() );
    }

    public void testComponentAccess_ParseSingleCleanBinding()
        throws Exception
    {
        LifecycleBindingLoader loader = (LifecycleBindingLoader) lookup( LifecycleBindingLoader.ROLE, "single-clean-mapping" );

        LifecycleBindings bindings = loader.getBindings();

        CleanBinding cleanBinding = bindings.getCleanBinding();
        assertNotNull( cleanBinding );

        Phase preClean = cleanBinding.getPreClean();
        assertNotNull( preClean );

        List mojos = preClean.getBindings();
        assertNotNull( mojos );
        assertEquals( 1, mojos.size() );

        MojoBinding mojo = (MojoBinding) mojos.get( 0 );
        assertEquals( "group", mojo.getGroupId() );
        assertEquals( "artifact", mojo.getArtifactId() );
        assertNull( mojo.getVersion() );
        assertEquals( "goalOne", mojo.getGoal() );
    }

}
