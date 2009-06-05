package org.apache.maven.plugin;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

public interface PluginCache
{
    
    public static class CacheRecord
    {
        public final ClassRealm realm;
        public final List<Artifact> artifacts;

        public CacheRecord( ClassRealm realm, List<Artifact> artifacts )
        {
            this.realm = realm;
            this.artifacts = artifacts;
        }
    }

    PluginDescriptor getPluginDescriptor( Plugin plugin, ArtifactRepository localRepository,
                                          List<ArtifactRepository> remoteRepositories );

    void putPluginDescriptor( Plugin plugin, ArtifactRepository localRepository,
                              List<ArtifactRepository> remoteRepositories, PluginDescriptor pluginDescriptor );

    CacheRecord get( Plugin plugin, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories );

    void put( Plugin plugin, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories,
              ClassRealm pluginRealm, List<Artifact> pluginArtifacts );

    void flush();
}
