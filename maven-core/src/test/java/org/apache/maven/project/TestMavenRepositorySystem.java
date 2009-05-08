package org.apache.maven.project;

import org.apache.maven.repository.LegacyRepositorySystem;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;

@Component(role = RepositorySystem.class, hint = "test")
public class TestMavenRepositorySystem
    extends LegacyRepositorySystem
{
}
