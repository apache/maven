/* Created on Aug 7, 2004 */
package org.apache.maven.plugin.loader.marmalade.tags;

import org.codehaus.marmalade.model.AbstractMarmaladeTagLibrary;

public class MojoTagLibrary extends AbstractMarmaladeTagLibrary {
    
    public static final String MOJO_TAG = "mojo";
    
    public static final String DESCRIPTOR_TAG = "descriptor";
    
    public static final String PREREQS_TAG = "prereqs";
    public static final String PREREQ_TAG = "prereq";
    
    public static final String PARAMETERS_TAG = "parameters";
    public static final String PARAMETER_TAG = "parameter";
    
    public static final String DESCRIPTION_TAG = "description";
    
    public static final String DEPENDENCIES_TAG = "dependencies";
    public static final String DEPENDENCY_TAG = "dependency";

    public MojoTagLibrary() {
        registerTag(MOJO_TAG, MojoTag.class);
        
        registerTag(DESCRIPTOR_TAG, MojoDescriptorTag.class);
        
        registerTag(PREREQS_TAG, MojoPrereqsTag.class);
        registerTag(PREREQ_TAG, MojoPrereqTag.class);
        
        registerTag(PARAMETERS_TAG, MojoParametersTag.class);
        registerTag(PARAMETER_TAG, MojoParameterTag.class);
        
        registerTag(DESCRIPTION_TAG, DescriptionTag.class);
        
        registerTag(DEPENDENCIES_TAG, MojoDependenciesTag.class);
        registerTag(DEPENDENCY_TAG, MojoDependencyTag.class);
    }

}
