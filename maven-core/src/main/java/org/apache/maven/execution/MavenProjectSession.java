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

    public void addComponentRealm( ClassRealm realm )
    {
        componentRealms.put( realm.getId(), realm );
    }

    public static String createRealmId( Artifact realmArtifact )
    {
        return ArtifactUtils.versionlessKey( realmArtifact );
    }

    public boolean containsRealm( Artifact extensionArtifact )
    {
        String id = createRealmId( extensionArtifact );
        return componentRealms.containsKey( id );
    }

    public boolean containsRealm( Plugin plugin )
    {
        String id = createRealmId( plugin );
        return componentRealms.containsKey( id );
    }

    public ClassRealm getProjectRealm()
    {
        return projectRealm;
    }

    public ClassRealm createExtensionRealm( Artifact extensionArtifact )
        throws DuplicateRealmException
    {
        String realmId = MavenProjectSession.createRealmId( extensionArtifact );
        ClassRealm extRealm = container.getContainerRealm().createChildRealm( projectId + "/" + realmId );

        componentRealms.put( realmId, extRealm );

        return extRealm;
    }

    public static String createProjectId( String groupId,
                                          String artifactId,
                                          String version )
    {
        return groupId + ":" + artifactId + ":" + version;
    }

    public ClassRealm getComponentRealm( String key )
    {
        return (ClassRealm) componentRealms.get( key );
    }


    public ClassRealm createPluginRealm( Plugin projectPlugin )
        throws DuplicateRealmException
    {
        String realmId = MavenProjectSession.createRealmId( projectPlugin );
        ClassRealm extRealm = projectRealm.createChildRealm( realmId );

        componentRealms.put( realmId, extRealm );

        return extRealm;
    }

    public static String createRealmId( Plugin plugin )
    {
        return ArtifactUtils.versionlessKey( plugin.getGroupId(), plugin.getArtifactId() );
    }

    public static String createRealmId( PluginDescriptor pd )
    {
        return ArtifactUtils.versionlessKey( pd.getGroupId(), pd.getArtifactId() );
    }

    public ClassRealm getPluginRealm( PluginDescriptor pluginDescriptor )
        throws NoSuchRealmException
    {
        String realmId = MavenProjectSession.createRealmId( pluginDescriptor );
        ClassRealm extRealm = projectRealm.getWorld().getRealm( realmId );

        componentRealms.put( realmId, extRealm );

        return extRealm;
    }

}
