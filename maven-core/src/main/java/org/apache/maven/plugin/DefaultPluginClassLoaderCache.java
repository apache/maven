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
    
    public void put( String key, ClassRealm pluginClassLoader )
    {
        pluginClassLoaders.put(  key, pluginClassLoader );
    }

    public ClassRealm get( String key )
    {
        return pluginClassLoaders.get( key );
    }

    public int size()
    {
        return pluginClassLoaders.size();
    }
}
