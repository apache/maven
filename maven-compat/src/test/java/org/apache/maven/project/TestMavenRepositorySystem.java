package org.apache.maven.project;

import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.repository.legacy.LegacyRepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

@Component(role = RepositorySystem.class, hint = "classpath")
public class TestMavenRepositorySystem
    extends LegacyRepositorySystem
{
    @Requirement(hint="classpath")
    private ArtifactResolver artifactResolver;
}
