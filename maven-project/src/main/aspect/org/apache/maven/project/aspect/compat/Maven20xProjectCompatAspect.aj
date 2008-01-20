package org.apache.maven.project.aspect.compat;

import org.apache.maven.project.DefaultMavenProjectBuilder;

import java.util.HashMap;
import java.util.Map;

public privileged aspect Maven20xProjectCompatAspect
{

    //DO NOT USE, it is here only for backward compatibility reasons. The existing
    // maven-assembly-plugin (2.2-beta-1) is accessing it via reflection.

// the aspect weaving seems not to work for reflection from plugin.
//    private Map DefaultMavenProjectBuilder.processedProjectCache = new HashMap();

}
