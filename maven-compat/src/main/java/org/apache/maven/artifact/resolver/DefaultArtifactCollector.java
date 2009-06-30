package org.apache.maven.artifact.resolver;

import org.codehaus.plexus.component.annotations.Component;

@Deprecated
@Component(role=ArtifactCollector.class)
public class DefaultArtifactCollector
    extends org.apache.maven.repository.legacy.resolver.DefaultLegacyArtifactCollector
    implements ArtifactCollector
{
}
