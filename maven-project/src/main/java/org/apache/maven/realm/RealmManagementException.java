package org.apache.maven.realm;

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.codehaus.plexus.component.repository.exception.ComponentRepositoryException;
import org.codehaus.plexus.configuration.PlexusConfigurationException;

import java.net.MalformedURLException;

public class RealmManagementException
    extends Exception
{

    private final String realmId;
    private String offendingGroupId;
    private String offendingArtifactId;
    private String offendingVersion;

    public RealmManagementException( String realmId, String message, DuplicateRealmException cause )
    {
        super( message, cause );
        this.realmId = realmId;
    }

    public RealmManagementException( String realmId, Artifact offendingArtifact, String message, MalformedURLException cause )
    {
        super( message, cause );
        this.realmId = realmId;
        offendingGroupId = offendingArtifact.getGroupId();
        offendingArtifactId = offendingArtifact.getArtifactId();
        offendingVersion = offendingArtifact.getVersion();
    }

    public RealmManagementException( String realmId,
                                     String message,
                                     NoSuchRealmException cause )
    {
        super( message, cause );
        this.realmId = realmId;
    }

    public RealmManagementException( String realmId,
                                     String message,
                                     ComponentRepositoryException cause )
    {
        super( message, cause );
        this.realmId = realmId;
    }

    public RealmManagementException( String realmId,
                                     String message,
                                     PlexusConfigurationException cause )
    {
        super( message, cause );
        this.realmId = realmId;
    }

    public RealmManagementException( String realmId, String message )
    {
        super( message );
        this.realmId = realmId;
    }

    public String getRealmId()
    {
        return realmId;
    }

    public String getOffendingGroupId()
    {
        return offendingGroupId;
    }

    public String getOffendingArtifactId()
    {
        return offendingArtifactId;
    }

    public String getOffendingVersion()
    {
        return offendingVersion;
    }

}
