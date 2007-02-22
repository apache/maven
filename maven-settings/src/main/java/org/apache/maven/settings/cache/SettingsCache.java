package org.apache.maven.settings.cache;

import org.apache.maven.context.BuildContext;
import org.apache.maven.context.BuildContextManager;
import org.apache.maven.context.ManagedBuildData;
import org.apache.maven.settings.Settings;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SettingsCache
    implements ManagedBuildData
{
    
    private static final String BUILD_CONTEXT_KEY = SettingsCache.class.getName();
    
    private static final String USER_SETTINGS_FILE = "user-settings";
    private static final String GLOBAL_SETTINGS_FILE = "global-settings";
    private static final String SETTINGS = "settings";
    
    private File userSettingsFile;
    private File globalSettingsFile;
    
    private Settings settings;
    
    private SettingsCache( File userSettingsFile, File globalSettingsFile )
    {
        this( userSettingsFile, globalSettingsFile, null );
    }
    
    public SettingsCache( File userSettingsFile, File globalSettingsFile, Settings settings )
    {
        this.userSettingsFile = userSettingsFile;
        this.globalSettingsFile = globalSettingsFile;
        this.settings = settings;
    }
    
    public File getUserSettingsSource()
    {
        return userSettingsFile;
    }
    
    public File getGlobalSettingsSource()
    {
        return globalSettingsFile;
    }
    
    public Settings getSettings()
    {
        return settings;
    }

    public Map getData()
    {
        Map data = new HashMap( 3 );
        
        data.put( USER_SETTINGS_FILE, userSettingsFile );
        data.put( GLOBAL_SETTINGS_FILE, globalSettingsFile );
        data.put( SETTINGS, settings );
        
        return data;
    }

    public String getStorageKey()
    {
        return BUILD_CONTEXT_KEY + "/" + String.valueOf( userSettingsFile ) + "/" + String.valueOf( globalSettingsFile );
    }

    public void setData( Map data )
    {
        this.userSettingsFile = (File) data.get( USER_SETTINGS_FILE );
        this.globalSettingsFile = (File) data.get( GLOBAL_SETTINGS_FILE );
        this.settings = (Settings) data.get( SETTINGS );
    }
    
    public void store( BuildContextManager buildContextManager )
    {
        BuildContext buildContext = buildContextManager.readBuildContext( true );
        buildContext.store( this );
        buildContextManager.storeBuildContext( buildContext );
    }

    public static SettingsCache read( BuildContextManager buildContextManager, File userSettingsFile )
    {
        return read( buildContextManager, userSettingsFile, null );
    }
    
    public static SettingsCache read( BuildContextManager buildContextManager, File userSettingsFile, File globalSettingsFile )
    {
        BuildContext buildContext = buildContextManager.readBuildContext( true );
        
        SettingsCache cache = new SettingsCache( userSettingsFile, globalSettingsFile );
        
        if ( !buildContext.retrieve( cache ) )
        {
            return null;
        }
        
        return cache;
    }

}
