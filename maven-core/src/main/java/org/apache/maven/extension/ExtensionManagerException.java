package org.apache.maven.extension;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.plugin.InvalidPluginException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginNotFoundException;
import org.apache.maven.plugin.version.PluginVersionNotFoundException;
import org.apache.maven.plugin.version.PluginVersionResolutionException;
import org.apache.maven.realm.RealmManagementException;

public class ExtensionManagerException
    extends Exception
{

    private Artifact extensionArtifact;

    private ArtifactResolutionResult resolutionResult;

    private String projectGroupId;

    private String projectArtifactId;

    private String projectVersion;

    public ExtensionManagerException( String message,
                                      Artifact extensionArtifact,
                                      String projectGroupId,
                                      String projectArtifactId,
                                      String projectVersion,
                                      ArtifactMetadataRetrievalException cause )
    {
        super( message, cause );
        this.extensionArtifact = extensionArtifact;
        this.projectGroupId = projectGroupId;
        this.projectArtifactId = projectArtifactId;
        this.projectVersion = projectVersion;
    }

    public ExtensionManagerException( String message,
                                      Artifact extensionArtifact,
                                      String projectGroupId,
                                      String projectArtifactId,
                                      String projectVersion,
                                      ArtifactResolutionResult result )
    {
        super( message );
        this.extensionArtifact = extensionArtifact;
        this.projectGroupId = projectGroupId;
        this.projectArtifactId = projectArtifactId;
        this.projectVersion = projectVersion;
        resolutionResult = result;
    }

    public ExtensionManagerException( String message,
                                      Artifact extensionArtifact,
                                      String projectGroupId,
                                      String projectArtifactId,
                                      String projectVersion,
                                      RealmManagementException cause )
    {
        super( message, cause );
        this.extensionArtifact = extensionArtifact;
        this.projectGroupId = projectGroupId;
        this.projectArtifactId = projectArtifactId;
        this.projectVersion = projectVersion;
    }

    public ExtensionManagerException( String message,
                                      Artifact extensionArtifact,
                                      String projectGroupId,
                                      String projectArtifactId,
                                      String projectVersion,
                                      ArtifactResolutionException cause )
    {
        super( message, cause );
        this.extensionArtifact = extensionArtifact;
        this.projectGroupId = projectGroupId;
        this.projectArtifactId = projectArtifactId;
        this.projectVersion = projectVersion;
    }

    public ExtensionManagerException( String message,
                                      Artifact extensionArtifact,
                                      String projectGroupId,
                                      String projectArtifactId,
                                      String projectVersion,
                                      ArtifactNotFoundException cause )
    {
        super( message, cause );
        this.extensionArtifact = extensionArtifact;
        this.projectGroupId = projectGroupId;
        this.projectArtifactId = projectArtifactId;
        this.projectVersion = projectVersion;
    }

    public ExtensionManagerException( String message,
                                      Artifact extensionArtifact,
                                      String projectGroupId,
                                      String projectArtifactId,
                                      String projectVersion,
                                      PluginNotFoundException cause )
    {
        super( message, cause );
        this.extensionArtifact = extensionArtifact;
        this.projectGroupId = projectGroupId;
        this.projectArtifactId = projectArtifactId;
        this.projectVersion = projectVersion;
    }

    public ExtensionManagerException( String message,
                                      Artifact extensionArtifact,
                                      String projectGroupId,
                                      String projectArtifactId,
                                      String projectVersion,
                                      PluginVersionResolutionException cause )
    {
        super( message, cause );
        this.extensionArtifact = extensionArtifact;
        this.projectGroupId = projectGroupId;
        this.projectArtifactId = projectArtifactId;
        this.projectVersion = projectVersion;
    }

    public ExtensionManagerException( String message,
                                      Artifact extensionArtifact,
                                      String projectGroupId,
                                      String projectArtifactId,
                                      String projectVersion,
                                      InvalidPluginException cause )
    {
        super( message, cause );
        this.extensionArtifact = extensionArtifact;
        this.projectGroupId = projectGroupId;
        this.projectArtifactId = projectArtifactId;
        this.projectVersion = projectVersion;
    }

    public ExtensionManagerException( String message,
                                      Artifact extensionArtifact,
                                      String projectGroupId,
                                      String projectArtifactId,
                                      String projectVersion,
                                      PluginManagerException cause )
    {
        super( message, cause );
        this.extensionArtifact = extensionArtifact;
        this.projectGroupId = projectGroupId;
        this.projectArtifactId = projectArtifactId;
        this.projectVersion = projectVersion;
    }

    public ExtensionManagerException( String message,
                                      Artifact extensionArtifact,
                                      String projectGroupId,
                                      String projectArtifactId,
                                      String projectVersion,
                                      PluginVersionNotFoundException cause )
    {
        super( message, cause );
        this.extensionArtifact = extensionArtifact;
        this.projectGroupId = projectGroupId;
        this.projectArtifactId = projectArtifactId;
        this.projectVersion = projectVersion;
    }

    public Artifact getExtensionArtifact()
    {
        return extensionArtifact;
    }

    public String getProjectGroupId()
    {
        return projectGroupId;
    }

    public String getProjectArtifactId()
    {
        return projectArtifactId;
    }

    public String getProjectVersion()
    {
        return projectVersion;
    }

    public ArtifactResolutionResult getArtifactResolutionResult()
    {
        return resolutionResult;
    }

}
