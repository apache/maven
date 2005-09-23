package org.apache.maven.usability;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.PluginContainerException;

public class PluginContainerDiagnoser
    implements ErrorDiagnoser
{

    public boolean canDiagnose( Throwable error )
    {
        return DiagnosisUtils.containsInCausality( error, PluginContainerException.class );
    }

    public String diagnose( Throwable error )
    {
        PluginContainerException exception = (PluginContainerException) DiagnosisUtils.getFromCausality( error, PluginContainerException.class );
        
        // this is a little hackish, but it's simple.
        String originalMessage = exception.getOriginalMessage();
        Plugin plugin = exception.getPlugin();
        
        StringBuffer message = new StringBuffer();
        
        message.append( "Failed to prepare plugin for execution.");
        message.append( "\n" );
        message.append( "\nGroupId: " ).append( plugin.getGroupId() );
        message.append( "\nArtifactId: " ).append( plugin.getArtifactId() );
        message.append( "\nVersion: " ).append( plugin.getVersion() );
        message.append( "\nReason: " ).append( originalMessage );
        
        if ( originalMessage.startsWith( "Cannot resolve artifact" ) )
        {
            message.append( DiagnosisUtils.getOfflineWarning() );
        }
        else
        {
            Throwable rootCause = DiagnosisUtils.getRootCause( exception );
            
            if ( rootCause != null )
            {
                message.append( "\nRoot Cause: " ).append( rootCause.getMessage() );
            }
        }
        
        message.append( "\n\n" );
        
        return message.toString();
    }

}
