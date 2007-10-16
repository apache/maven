package org.apache.maven.core.test;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;

public class MyArtifactFactory
    implements ArtifactFactory
{

    private static final boolean OPTIONAL = false;
    private static final ArtifactHandler HANDLER = new DefaultArtifactHandler( "jar" );
    private static final String CLASSIFIER = null;
    private static final String TYPE = "jar";
    private static final String SCOPE = Artifact.SCOPE_COMPILE;
    private static final VersionRange VERSION = VersionRange.createFromVersion( "1.1.1" );
    private static final String AID = "test-artifact";
    private static final String GID = "test.group";

    public Artifact createArtifact( String groupId,
                                    String artifactId,
                                    String version,
                                    String scope,
                                    String type )
    {
        return new DefaultArtifact( GID, AID, VERSION, SCOPE, TYPE, CLASSIFIER, HANDLER, OPTIONAL );
    }

    public Artifact createArtifactWithClassifier( String groupId,
                                                  String artifactId,
                                                  String version,
                                                  String type,
                                                  String classifier )
    {
        return new DefaultArtifact( GID, AID, VERSION, SCOPE, TYPE, CLASSIFIER, HANDLER, OPTIONAL );
    }

    public Artifact createBuildArtifact( String groupId,
                                         String artifactId,
                                         String version,
                                         String packaging )
    {
        return new DefaultArtifact( GID, AID, VERSION, SCOPE, TYPE, CLASSIFIER, HANDLER, OPTIONAL );
    }

    public Artifact createDependencyArtifact( String groupId,
                                              String artifactId,
                                              VersionRange versionRange,
                                              String type,
                                              String classifier,
                                              String scope )
    {
        return new DefaultArtifact( GID, AID, VERSION, SCOPE, TYPE, CLASSIFIER, HANDLER, OPTIONAL );
    }

    public Artifact createDependencyArtifact( String groupId,
                                              String artifactId,
                                              VersionRange versionRange,
                                              String type,
                                              String classifier,
                                              String scope,
                                              boolean optional )
    {
        return new DefaultArtifact( GID, AID, VERSION, SCOPE, TYPE, CLASSIFIER, HANDLER, OPTIONAL );
    }

    public Artifact createDependencyArtifact( String groupId,
                                              String artifactId,
                                              VersionRange versionRange,
                                              String type,
                                              String classifier,
                                              String scope,
                                              String inheritedScope )
    {
        return new DefaultArtifact( GID, AID, VERSION, SCOPE, TYPE, CLASSIFIER, HANDLER, OPTIONAL );
    }

    public Artifact createDependencyArtifact( String groupId,
                                              String artifactId,
                                              VersionRange versionRange,
                                              String type,
                                              String classifier,
                                              String scope,
                                              String inheritedScope,
                                              boolean optional )
    {
        return new DefaultArtifact( GID, AID, VERSION, SCOPE, TYPE, CLASSIFIER, HANDLER, OPTIONAL );
    }

    public Artifact createExtensionArtifact( String groupId,
                                             String artifactId,
                                             VersionRange versionRange )
    {
        return new DefaultArtifact( GID, AID, VERSION, SCOPE, TYPE, CLASSIFIER, HANDLER, OPTIONAL );
    }

    public Artifact createParentArtifact( String groupId,
                                          String artifactId,
                                          String version )
    {
        return new DefaultArtifact( GID, AID, VERSION, SCOPE, TYPE, CLASSIFIER, HANDLER, OPTIONAL );
    }

    public Artifact createPluginArtifact( String groupId,
                                          String artifactId,
                                          VersionRange versionRange )
    {
        return new DefaultArtifact( GID, AID, VERSION, SCOPE, TYPE, CLASSIFIER, HANDLER, OPTIONAL );
    }

    public Artifact createProjectArtifact( String groupId,
                                           String artifactId,
                                           String version )
    {
        return new DefaultArtifact( GID, AID, VERSION, SCOPE, TYPE, CLASSIFIER, HANDLER, OPTIONAL );
    }

    public Artifact createProjectArtifact( String groupId,
                                           String artifactId,
                                           String version,
                                           String scope )
    {
        return new DefaultArtifact( GID, AID, VERSION, SCOPE, TYPE, CLASSIFIER, HANDLER, OPTIONAL );
    }
}
