package org.apache.maven.plugin;

import java.util.List;

public class InvalidPluginDescriptorException
    extends Exception
{
    private List<String> errors;
    
    public InvalidPluginDescriptorException( String message, List<String> errors )
    {
        super( message );
        this.errors = errors;
    }
}
