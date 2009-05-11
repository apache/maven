package org.apache.maven.plugin;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;

@Component(role=PluginClassLoaderCache.class)
public class DefaultPluginClassLoaderCache
    implements PluginClassLoaderCache
{
    private Map<String,ClassRealm> pluginClassLoaders = new HashMap<String,ClassRealm>();
    
    public void cachePluginClassLoader( String key, ClassRealm pluginClassLoader )
    {
        pluginClassLoaders.put(  key, pluginClassLoader );
    }

    public ClassRealm getPluginClassLoader( String key )
    {
        return pluginClassLoaders.get( key );
    }

}
