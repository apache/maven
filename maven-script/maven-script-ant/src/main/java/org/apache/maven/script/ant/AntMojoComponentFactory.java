package org.apache.maven.script.ant;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.factory.ComponentInstantiationException;
import org.codehaus.plexus.component.factory.ant.AntComponentFactory;
import org.codehaus.plexus.component.factory.ant.AntScriptInvoker;
import org.codehaus.plexus.component.repository.ComponentDescriptor;

public class AntMojoComponentFactory
    extends AntComponentFactory
{
    public Object newInstance( ComponentDescriptor descriptor, ClassRealm realm, PlexusContainer container )
        throws ComponentInstantiationException
    {
        return new AntMojoWrapper( (AntScriptInvoker) super.newInstance( descriptor, realm, container ) );
    }

}
