package org.apache.maven.execution;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

import java.util.Collection;
import java.util.List;

public interface MavenRealmManager
{

    void clear();

    boolean hasExtensionRealm( Artifact extensionArtifact );

    ClassRealm createExtensionRealm( Artifact extensionArtifact,
                                     Collection artifacts )
        throws RealmManagementException;

    void importExtensionsIntoProjectRealm( String projectGroupId,
                                           String projectArtifactId,
                                           String projectVersion,
                                           Artifact extensionArtifact )
        throws RealmManagementException;

    ClassRealm getProjectRealm( String groupId,
                                String artifactId,
                                String version );

    ClassRealm getPluginRealm( Plugin plugin );

    List getPluginArtifacts( Plugin plugin );

    ClassRealm createPluginRealm( Plugin plugin,
                                  Artifact pluginArtifact,
                                  Collection artifacts )
        throws RealmManagementException;
}
