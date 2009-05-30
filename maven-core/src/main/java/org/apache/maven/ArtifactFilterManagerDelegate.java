package org.apache.maven;

import java.util.Set;

public interface ArtifactFilterManagerDelegate
{

    void addExcludes( Set<String> excludes );

    void addCoreExcludes( Set<String> excludes );

}
