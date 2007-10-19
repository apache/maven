package org.apache.maven.execution;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Project-level session that stores extension and plugin realms for the project
 * with the specified projectId (groupId, artifactId, and version). The projectId
 * is the key here, not the project instance, since this session may be constructed
 * before the MavenProject instance has been created, in order to pre-scan for
 * extensions that may alter the project instance when it is constructed (using
 * custom profile activators, for instance).
 *
 * The {@link MavenProjectSession#getProjectRealm()} method is used in many cases
 * as the lookup realm when the project associated with this session is active,
 * as in the lifecycle executor. In other cases, where a plugin itself is being
 * executed, the {@link MavenProjectSession#getPluginRealm(Plugin)} and
 * {@link MavenProjectSession#getPluginRealm(PluginDescriptor)} methods allow for
 * retrieval of the {@link ClassRealm} instance - linked to this project - which
 * contains the plugin classes...in these cases, the plugin realm is used as
 * the lookupRealm.
 *
 * @author jdcasey
 *
 */
public class MavenProjectSession
{

    private Map componentRealms = new HashMap();

    private String projectId;

    private ClassRealm projectRealm;

    private final PlexusContainer container;

    public MavenProjectSession( String projectId,
                                PlexusContainer container )
        throws PlexusContainerException
    {
        this.projectId = projectId;
        this.container = container;
        projectRealm = container.createComponentRealm( projectId, Collections.EMPTY_LIST );
    }

    public String getProjectId()
    {
        return projectId;
    }

    private String createExtensionRealmId( Artifact realmArtifact )
    {
        return projectId + "/extensions/" + ArtifactUtils.versionlessKey( realmArtifact );
    }

    public boolean containsExtensionRealm( Artifact extensionArtifact )
    {
        String id = createExtensionRealmId( extensionArtifact );
        return componentRealms.containsKey( id );
    }

    public boolean containsPluginRealm( Plugin plugin )
    {
        String realmId = createPluginRealmId( ArtifactUtils.versionlessKey( plugin.getGroupId(), plugin.getArtifactId() ) );

        return componentRealms.containsKey( realmId );
    }

    public ClassRealm getProjectRealm()
    {
        return projectRealm;
    }

    /**
     * Creates a new ClassRealm for the given extension artifact. This realm
     * will be a child realm of the container passed to this instance in the
     * constructor, and does not inherit from the project realm. This is important,
     * since the project realm will eventually import certain extension
     * component classes from the realm resulting from this call.
     *
     * @param extensionArtifact
     * @return
     * @throws DuplicateRealmException
     */
    public ClassRealm createExtensionRealm( Artifact extensionArtifact )
        throws DuplicateRealmException
    {
        String realmId = createExtensionRealmId( extensionArtifact );
        ClassRealm extRealm = container.getContainerRealm().createChildRealm( realmId );

        componentRealms.put( realmId, extRealm );

        return extRealm;
    }

    /**
     * Create a projectId for use in the {@link MavenProjectSession} constructor
     * and lookup (from inside {@link MavenSession} currently). This method provides
     * a standard way of forming that id.
     *
     * @param groupId
     * @param artifactId
     * @param version
     * @return
     */
    public static String createProjectId( String groupId,
                                          String artifactId,
                                          String version )
    {
        return groupId + ":" + artifactId + ":" + version;
    }

    public ClassRealm getPluginRealm( Plugin plugin )
        throws NoSuchRealmException
    {
        String realmId = createPluginRealmId( ArtifactUtils.versionlessKey( plugin.getGroupId(), plugin.getArtifactId() ) );

        return projectRealm.getWorld().getRealm( realmId );
    }


    public ClassRealm createPluginRealm( Plugin plugin )
        throws DuplicateRealmException
    {
        String realmId = createPluginRealmId( ArtifactUtils.versionlessKey( plugin.getGroupId(), plugin.getArtifactId() ) );

        ClassRealm extRealm = projectRealm.createChildRealm( realmId );

        componentRealms.put( realmId, extRealm );

        return extRealm;
    }

    private String createPluginRealmId( String baseId )
    {
        return projectId + "/plugins/" + baseId;
    }

    public ClassRealm getPluginRealm( PluginDescriptor pd )
        throws NoSuchRealmException
    {
        String realmId = createPluginRealmId( ArtifactUtils.versionlessKey( pd.getGroupId(), pd.getArtifactId() ) );

        ClassRealm extRealm = projectRealm.getWorld().getRealm( realmId );

        componentRealms.put( realmId, extRealm );

        return extRealm;
    }

}
