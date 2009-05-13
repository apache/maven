package org.apache.maven.plugin;

import org.codehaus.plexus.classworlds.realm.ClassRealm;

/**
 * For the most part plugins do not specify their own dependencies so the {@link ClassLoader} used to
 * execute a {@link Mojo} remains the same across projects. But we do need to account for the case where
 * plugin dependencies are specified. Maven has a default implementation and integrators can create their
 * own implementations to deal with different environments like an IDE.
 * 
 * @author Jason van Zyl
 *
 */
public interface PluginClassLoaderCache
{
    void put( String key, ClassRealm pluginClassLoader );
    
    ClassRealm get( String key );
    
    int size();
}
