package org.apache.maven.realm;

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.codehaus.plexus.component.discovery.ComponentDiscoverer;
import org.codehaus.plexus.component.discovery.ComponentDiscovererManager;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryEvent;
import org.codehaus.plexus.component.discovery.ComponentDiscoveryListener;
import org.codehaus.plexus.configuration.PlexusConfigurationException;
import org.codehaus.plexus.context.Context;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RealmScanningUtils
{

    private static final String DISCOVERY_REALM_ID = "discovery realm";

    public static List scanForComponentSetDescriptors( Artifact artifact, ComponentDiscoverer discoverer,
                                                       Context context, String discoveryContextId )
        throws RealmManagementException
    {
        ClassWorld discoveryWorld = new ClassWorld();

        List componentSetDescriptors;
        try
        {
            // Create an entire new ClassWorld, ClassRealm for discovering
            // the immediate components of the extension artifact, so we don't pollute the
            // container with component descriptors or realms that don't have any meaning beyond discovery.
            ClassRealm discoveryRealm;
            try
            {
                discoveryRealm = discoveryWorld.newRealm( DISCOVERY_REALM_ID );
            }
            catch ( DuplicateRealmException e )
            {
                throw new RealmManagementException( discoveryContextId,
                                                    "Unable to create temporary ClassRealm for local-component discovery.",
                                                    e );
            }

            try
            {
                discoveryRealm.addURL( artifact.getFile().toURL() );
            }
            catch ( MalformedURLException e )
            {
                throw new RealmManagementException( discoveryContextId, artifact,
                                                    "Unable to generate URL from artifact file: " + artifact.getFile() +
                                                        " for local-component discovery.", e );
            }

            try
            {
                // Find the extension component descriptors that exist ONLY in the immediate extension
                // artifact...this prevents us from adding plexus-archiver components to the mix, for instance,
                // when the extension uses that dependency.
                componentSetDescriptors = discoverer.findComponents( context, discoveryRealm );
            }
            catch ( PlexusConfigurationException e )
            {
                throw new RealmManagementException( discoveryContextId,
                                                    "Unable to discover components in artifact: " + artifact.getId(),
                                                    e );
            }
        }
        finally
        {
            Collection realms = discoveryWorld.getRealms();
            for ( Iterator it = realms.iterator(); it.hasNext(); )
            {
                ClassRealm realm = (ClassRealm) it.next();
                try
                {
                    discoveryWorld.disposeRealm( realm.getId() );
                }
                catch ( NoSuchRealmException e )
                {
                }
            }
        }

        return componentSetDescriptors;
    }

    public static ComponentDiscovererManager getDummyComponentDiscovererManager()
    {
        return new DummyDiscovererManager();
    }

    private static final class DummyDiscovererManager
        implements ComponentDiscovererManager
    {

        public void fireComponentDiscoveryEvent( ComponentDiscoveryEvent arg0 )
        {
        }

        public List getComponentDiscoverers()
        {
            return null;
        }

        public Map getComponentDiscoveryListeners()
        {
            return null;
        }

        public List getListeners()
        {
            return null;
        }

        public void initialize()
        {
        }

        public void registerComponentDiscoveryListener( ComponentDiscoveryListener l )
        {
        }

        public void removeComponentDiscoveryListener( ComponentDiscoveryListener l )
        {
        }

    }
}
