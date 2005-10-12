package org.apache.maven.usability;

import org.apache.maven.plugin.MojoExecutionException;

public class MojoExecutionExceptionDiagnoser
    implements ErrorDiagnoser
{

    public boolean canDiagnose( Throwable error )
    {
        return DiagnosisUtils.containsInCausality( error, MojoExecutionException.class );
    }

    public String diagnose( Throwable error )
    {
        MojoExecutionException mee = (MojoExecutionException) DiagnosisUtils.getFromCausality( error, MojoExecutionException.class );
        
        StringBuffer message = new StringBuffer();
        
        message.append( "Error executing mojo" );
        
        Object source = mee.getSource();
        if ( source != null )
        {
            message.append( ": " ).append( mee.getSource() ).append( "\n" );
        }
        else
        {
            message.append( ".\n" );
        }
        
        message.append( "\nMessage: " ).append( mee.getMessage() );
        
        String longMessage = mee.getLongMessage();
        if ( longMessage != null )
        {
            message.append( "\n\n" ).append( longMessage );
        }
        
        Throwable root = DiagnosisUtils.getRootCause( mee );
        
        if ( root != null && root != mee )
        {
            message.append( "\n\nRoot Cause: " ).append( root.getMessage() );
        }
        
        message.append( "\n\n" );
        
        return message.toString();
    }

}
