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

    public boolean containsRealm( Plugin plugin )
    {
        String realmId = createPluginRealmId( ArtifactUtils.versionlessKey( plugin.getGroupId(), plugin.getArtifactId() ) );

        return componentRealms.containsKey( realmId );
    }

    public ClassRealm getProjectRealm()
    {
        return projectRealm;
    }

    public ClassRealm createExtensionRealm( Artifact extensionArtifact )
        throws DuplicateRealmException
    {
        String realmId = createExtensionRealmId( extensionArtifact );
        ClassRealm extRealm = container.getContainerRealm().createChildRealm( realmId );

        componentRealms.put( realmId, extRealm );

        return extRealm;
    }

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
