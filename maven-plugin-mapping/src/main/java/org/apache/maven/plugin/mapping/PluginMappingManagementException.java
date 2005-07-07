package org.apache.maven.plugin.mapping;

public class PluginMappingManagementException
    extends Exception
{

    public PluginMappingManagementException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public PluginMappingManagementException( String message )
    {
        super( message );
    }

}
