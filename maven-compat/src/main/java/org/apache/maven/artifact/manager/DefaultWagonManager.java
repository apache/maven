package org.apache.maven.artifact.manager;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.codehaus.plexus.component.annotations.Component;

@Component(role=WagonManager.class) 
public class DefaultWagonManager
    extends org.apache.maven.repository.legacy.DefaultWagonManager
    implements WagonManager
{
}
