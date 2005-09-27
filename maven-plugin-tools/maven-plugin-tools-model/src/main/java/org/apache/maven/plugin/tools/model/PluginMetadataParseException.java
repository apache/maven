package org.apache.maven.plugin.tools.model;

import java.io.File;

public class PluginMetadataParseException
    extends Exception
{
    
    static final long serialVersionUID = 1;

    private final File metadataFile;
    private final String originalMessage;

    public PluginMetadataParseException( File metadataFile, String message, Throwable cause )
    {
        super( "Error parsing file: " + metadataFile + ". Reason: " + message, cause );
        
        this.metadataFile = metadataFile;
        this.originalMessage = message;
    }

    public PluginMetadataParseException( File metadataFile, String message )
    {
        super( "Error parsing file: " + metadataFile + ". Reason: " + message );
        
        this.metadataFile = metadataFile;
        this.originalMessage = message;
    }
    
    public File getMetadataFile()
    {
        return metadataFile;
    }
    
    public String getOriginalMessage()
    {
        return originalMessage;
    }

}
