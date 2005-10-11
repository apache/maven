package org.apache.maven.usability;

import org.apache.maven.project.InvalidProjectModelException;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.validation.ModelValidationResult;

public class ProjectBuildDiagnoser
    implements ErrorDiagnoser
{

    public boolean canDiagnose( Throwable error )
    {
        return DiagnosisUtils.containsInCausality( error, ProjectBuildingException.class );
    }

    public String diagnose( Throwable error )
    {
        ProjectBuildingException pbe = (ProjectBuildingException) DiagnosisUtils.getFromCausality( error, ProjectBuildingException.class );
        
        StringBuffer message = new StringBuffer();
        
        message.append( "Error building POM (may not be this project's POM)." ).append( "\n\n" );
        
        message.append( "\nProject ID: " ).append( pbe.getProjectId() );
        
        if ( pbe instanceof InvalidProjectModelException )
        {
            InvalidProjectModelException ipme = (InvalidProjectModelException) pbe;
            
            message.append( "\nPOM Location: " ).append( ipme.getPomLocation() );
            
            ModelValidationResult result = ipme.getValidationResult();
            
            if ( result != null )
            {
                message.append( "\nValidation Messages:\n\n" ).append( ipme.getValidationResult().render( "    " ) );
            }
        }
        
        message.append( "\n\n" ).append( "Reason: " ).append( pbe.getMessage() );
        
        Throwable t = DiagnosisUtils.getRootCause( error );
        
        if ( t != null && t != pbe )
        {
            message.append( "\n" ).append( "Root Cause: " ).append( t.getMessage() );
        }
        
        message.append( "\n\n" );
        
        return message.toString();
    }

}
