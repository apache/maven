/* Created on Aug 7, 2004 */
package org.apache.maven.plugin.loader.marmalade.tags;

import org.apache.maven.plugin.descriptor.Dependency;
import org.codehaus.marmalade.model.AbstractMarmaladeTag;
import org.codehaus.marmalade.runtime.MarmaladeExecutionContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionException;

public class MojoDependencyTag extends AbstractMarmaladeTag
{
    public static final String GROUP_ID_ATTRIBUTE = "groupId";
    public static final String ARTIFACT_ID_ATTRIBUTE = "artifactId";
    public static final String VERSION_ATTRIBUTE = "version";
    public static final String TYPE_ATTRIBUTE = "type";
    
    protected void doExecute( MarmaladeExecutionContext context )
        throws MarmaladeExecutionException
    {
        String groupId = (String)requireTagAttribute(GROUP_ID_ATTRIBUTE, String.class, context);
        String artifactId = (String)requireTagAttribute(ARTIFACT_ID_ATTRIBUTE, String.class, context);
        String version = (String)requireTagAttribute(VERSION_ATTRIBUTE, String.class, context);
        
        String type = (String)getAttributes().getValue(TYPE_ATTRIBUTE, String.class, context);
        
        MojoDependenciesTag parent = (MojoDependenciesTag)requireParent(MojoDependenciesTag.class);
        
        Dependency dep = new Dependency();
        dep.setGroupId(groupId);
        dep.setArtifactId(artifactId);
        dep.setVersion(version);
        
        if(type != null) {
            dep.setType(type);
        }
        
        parent.addDependency(dep);
    }
    
    protected boolean alwaysProcessChildren() {
        // never process children...shouldn't even have any!
        return false;
    }
}
