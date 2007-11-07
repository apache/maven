package org.apache.maven.project;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;

import java.io.File;

public class InvalidProjectVersionException
    extends ProjectBuildingException
{

    private final String locationInPom;
    private final String offendingVersion;

    public InvalidProjectVersionException( String projectId, String locationInPom, String offendingVersion, File pomFile, InvalidVersionSpecificationException cause )
    {
        super( projectId, formatMessage( projectId, locationInPom, offendingVersion, cause ), pomFile, cause );
        this.locationInPom = locationInPom;
        this.offendingVersion = offendingVersion;
    }

    private static String formatMessage( String projectId,
                                         String locationInPom,
                                         String offendingVersion,
                                         InvalidVersionSpecificationException cause )
    {
        return "Invalid version: " + offendingVersion + " found for: " + locationInPom + " in project: " + projectId + ". Reason: " + cause.getMessage();
    }

    public String getOffendingVersion()
    {
        return offendingVersion;
    }

    public String getLocationInPom()
    {
        return locationInPom;
    }

}
