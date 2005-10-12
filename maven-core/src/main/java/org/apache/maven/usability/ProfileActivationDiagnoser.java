package org.apache.maven.usability;

import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.usability.diagnostics.DiagnosisUtils;
import org.apache.maven.usability.diagnostics.ErrorDiagnoser;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

public class ProfileActivationDiagnoser
    implements ErrorDiagnoser
{

    public boolean canDiagnose( Throwable error )
    {
        return DiagnosisUtils.containsInCausality( error, ProfileActivationException.class );
    }

    public String diagnose( Throwable error )
    {
        ProfileActivationException activationException = (ProfileActivationException) DiagnosisUtils.getFromCausality( error, ProfileActivationException.class );
        
        StringBuffer messageBuffer = new StringBuffer();
        
        messageBuffer.append( "Error activating profiles." );
        messageBuffer.append( "\n\nReason: " ).append( activationException.getMessage() );
        
        if ( DiagnosisUtils.containsInCausality( activationException, ComponentLookupException.class ) )
        {
            ComponentLookupException cle = (ComponentLookupException) DiagnosisUtils.getFromCausality( activationException, ComponentLookupException.class );
            
            messageBuffer.append( "\n\nThere was a problem retrieving one or more profile activators." );
            messageBuffer.append( "\n" ).append( cle.getMessage() );
        }
        
        Throwable root = DiagnosisUtils.getRootCause( error );
        
        if ( root != null && root != error )
        {
            messageBuffer.append( "\n\nRoot Cause: " ).append( root.getMessage() );
        }
        
        messageBuffer.append( "\n" );
        
        return messageBuffer.toString();
    }

}
