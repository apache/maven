// TODO Attach license header here.
package org.apache.maven.project;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyDefault;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author jdcasey
 *
 * Created on Feb 1, 2005
 */
public class DefaultProjectDefaultsInjector implements ProjectDefaultsInjector
{
    
    public void injectDefaults(MavenProject project)
    {
        injectDependencyDefaults(project.getDependencies(), project.getDependencyDefaults());
    }

    /** Added: Feb 1, 2005 by jdcasey
     */
    private void injectDependencyDefaults( List dependencies, List dependencyDefaults )
    {
        // a given project's dependencies should be smaller than the group-defined defaults set...
        // in other words, the project's deps will probably be a subset of those specified in defaults.
        Map depsMap = new TreeMap();
        for ( Iterator it = dependencies.iterator(); it.hasNext(); )
        {
            Dependency dep = (Dependency) it.next();
            depsMap.put(dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getType(), dep);
        }
        
        for ( Iterator it = dependencyDefaults.iterator(); it.hasNext(); )
        {
            DependencyDefault depdef = (DependencyDefault) it.next();
            String key = depdef.getGroupId() + ":" + depdef.getArtifactId() + ":" + depdef.getType();
            
            Dependency dep = (Dependency) depsMap.get(key);
            if(dep != null)
            {
                mergeWithDefaults(dep, depdef);
                validateDependency(dep);
            }
        }
    }

    /** Added: Feb 1, 2005 by jdcasey
     */
    private void mergeWithDefaults( Dependency dep, DependencyDefault depdef )
    {
        if(dep.getVersion() == null && depdef.getVersion() != null)
        {
            dep.setVersion(depdef.getVersion());
            
            if(dep.getArtifact() == null && depdef.getArtifact() != null)
            {
                dep.setArtifact(depdef.getArtifact());
            }
            
            if(dep.getUrl() == null && depdef.getUrl() != null)
            {
                dep.setUrl(depdef.getUrl());
            }
        }
        
        Properties depProps = dep.getProperties();
        Properties depdefProps = depdef.getProperties();
        if(depProps == null && depdefProps != null)
        {
            dep.setProperties(depdefProps);
        }
    }

    /** Added: Feb 1, 2005 by jdcasey
     */
    private void validateDependency( Dependency dep )
    {
        if(StringUtils.isEmpty(dep.getVersion()))
        {
            throw new IllegalStateException("Dependency version is null for: " + dep.getId());
        }
    }

}
