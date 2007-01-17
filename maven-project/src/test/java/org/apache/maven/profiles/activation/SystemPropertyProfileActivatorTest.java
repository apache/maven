package org.apache.maven.profiles.activation;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.context.DefaultContext;

import java.util.Properties;

import junit.framework.TestCase;

public class SystemPropertyProfileActivatorTest
    extends TestCase
{

    public void testCanDetect_ShouldReturnTrueWhenActivationPropertyIsPresent()
        throws ContextException
    {
        ActivationProperty prop = new ActivationProperty();
        prop.setName( "test" );

        Activation activation = new Activation();

        activation.setProperty( prop );

        Profile profile = new Profile();

        profile.setActivation( activation );

        assertTrue( buildProfileActivator().canDetermineActivation( profile ) );
    }

    public void testCanDetect_ShouldReturnFalseWhenActivationPropertyIsNotPresent()
        throws ContextException
    {
        Activation activation = new Activation();

        Profile profile = new Profile();

        profile.setActivation( activation );

        assertFalse( buildProfileActivator().canDetermineActivation( profile ) );
    }

    public void testIsActive_ShouldReturnTrueWhenPropertyNameSpecifiedAndPresent()
        throws ContextException
    {
        ActivationProperty prop = new ActivationProperty();
        prop.setName( "test" );

        Activation activation = new Activation();

        activation.setProperty( prop );

        Profile profile = new Profile();

        profile.setActivation( activation );

        System.setProperty( "test", "true" );

        assertTrue( buildProfileActivator().isActive( profile ) );
    }

    public void testIsActive_ShouldReturnFalseWhenPropertyNameSpecifiedAndMissing()
        throws ContextException
    {
        ActivationProperty prop = new ActivationProperty();
        prop.setName( "test" );

        Activation activation = new Activation();

        activation.setProperty( prop );

        Profile profile = new Profile();

        profile.setActivation( activation );

        Properties props = System.getProperties();
        props.remove( "test" );
        System.setProperties( props );

        assertFalse( buildProfileActivator().isActive( profile ) );
    }

    private SystemPropertyProfileActivator buildProfileActivator()
        throws ContextException
    {
        SystemPropertyProfileActivator activator = new SystemPropertyProfileActivator();
        activator.contextualize( new DefaultContext() );

        return activator;
    }

}
