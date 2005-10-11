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
        
        message.append( "Error executing mojo: " ).append( mee.getSource() ).append( "\n\n" );
        message.append( mee.getLongMessage() ).append( "\n\n" );
        
        Throwable root = DiagnosisUtils.getRootCause( mee );
        
        if ( root != null && root != mee )
        {
            message.append( "Root Cause: " ).append( root.getMessage() ).append( "\n\n" );
        }
        
        return message.toString();
    }

}
