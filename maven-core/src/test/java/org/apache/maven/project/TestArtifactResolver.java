package org.apache.maven.project;

import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.DefaultArtifactResolver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Component(role=ArtifactResolver.class,hint="classpath")
public class TestArtifactResolver
    extends DefaultArtifactResolver
{
    @Requirement(hint="classpath")
    private ArtifactMetadataSource source;
}
