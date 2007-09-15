package org.apache.maven.plugin;

import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

/**
 * 
 * @author <a href="mailto:piotr@tabor.waw.pl">Piotr Tabor</a>
 */
public interface PluginRealmManager
{
    public static final String ROLE=PluginRealmManager.class.getName();
    
    public ClassRealm getOrCreateRealm( Plugin projectPlugin, Artifact pluginArtifact, Set artifacts ) throws PluginManagerException;
}