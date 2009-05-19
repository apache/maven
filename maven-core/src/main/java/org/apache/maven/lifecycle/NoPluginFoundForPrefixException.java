package org.apache.maven.lifecycle;

public class NoPluginFoundForPrefixException
    extends Exception
{
    private String prefix;
    
    public NoPluginFoundForPrefixException( String prefix )
    {
        super( "No plugin found for prefix '" + prefix + "'" ); 
    }
}
