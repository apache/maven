package org.apache.maven.project;

import org.apache.maven.project.validation.ModelValidationResult;

public class InvalidProjectModelException
    extends ProjectBuildingException
{

    private final String pomLocation;
    private ModelValidationResult validationResult;

    public InvalidProjectModelException( String projectId, String pomLocation, String message, ModelValidationResult validationResult )
    {
        super( projectId, message );
        
        this.pomLocation = pomLocation;
        this.validationResult = validationResult;
    }

    public InvalidProjectModelException( String projectId, String pomLocation, String message )
    {
        super( projectId, message );
        
        this.pomLocation = pomLocation;
    }

    public final String getPomLocation()
    {
        return pomLocation;
    }

    public final ModelValidationResult getValidationResult()
    {
        return validationResult;
    }

}
