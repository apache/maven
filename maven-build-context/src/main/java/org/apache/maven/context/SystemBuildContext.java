package org.apache.maven.context;

import java.util.Properties;

public class SystemBuildContext
    implements ManagedBuildData
{
    
    public static final String BUILD_CONTEXT_KEY = SystemBuildContext.class.getName();
    
    private Properties systemProperties;
    private Properties envars;
    
    public SystemBuildContext()
    {
        this.systemProperties = System.getProperties();
    }
    
    public void setEnvironmentVariables( Properties envars )
    {
        this.envars = envars;
    }
    
    public Properties getEnvironmentVariables()
    {
        return envars;
    }
    
    public void setSystemProperties( Properties systemProperties )
    {
        this.systemProperties = systemProperties;
    }
    
    public Properties getSystemProperties()
    {
        return systemProperties;
    }
    
    public String getSystemProperty( String name )
    {
        return systemProperties == null ? null : systemProperties.getProperty( name );
    }

    public String getStorageKey()
    {
        return BUILD_CONTEXT_KEY;
    }

    public static SystemBuildContext getSystemBuildContext( BuildContextManager buildContextManager, boolean create )
    {
        BuildContext buildContext = buildContextManager.readBuildContext( false );
        
        SystemBuildContext systemContext = null;
        
        if ( buildContext != null )
        {
            systemContext = (SystemBuildContext) buildContext.get( BUILD_CONTEXT_KEY );
        }
        
        if ( create && systemContext == null )
        {
            systemContext = new SystemBuildContext();
        }
        
        return systemContext;
    }
    
    public void store( BuildContextManager buildContextManager )
    {
        BuildContext buildContext = buildContextManager.readBuildContext( true );
        
        buildContext.put( this );
        
        buildContextManager.storeBuildContext( buildContext );
    }
}
