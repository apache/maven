/* Created on Aug 7, 2004 */
package org.apache.maven.plugin.loader.marmalade.tags;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.codehaus.marmalade.model.AbstractMarmaladeTag;
import org.codehaus.marmalade.model.MarmaladeAttributes;
import org.codehaus.marmalade.runtime.MarmaladeExecutionContext;
import org.codehaus.marmalade.runtime.MarmaladeExecutionException;

import java.util.LinkedList;
import java.util.List;

/**
 * @author jdcasey
 */
public class MojoDescriptorTag extends AbstractMarmaladeTag
    implements DescriptionOwner
{
    public static final String ID_ATTRIBUTE = "id";
    public static final String INSTANTIATION_STRATEGY_ATTRIBUTE = "instantiation-strategy";
    public static final String EXECUTION_STRATEGY_ATTRIBUTE = "execution-strategy";
    public static final String GOAL_ATTRIBUTE = "goal";
    
    private String id;
    private String instantiationStrategy;
    private String executionStrategy;
    private String goal;
    private String description;
    private List prereqs = new LinkedList(  );
    private List dependencies = new LinkedList(  );
    private List parameters = new LinkedList();

    protected void doExecute( MarmaladeExecutionContext context )
        throws MarmaladeExecutionException
    {
        this.id = ( String ) requireTagAttribute( ID_ATTRIBUTE, String.class,
                context );
        
        MarmaladeAttributes attributes = getAttributes();
        this.instantiationStrategy = ( String ) attributes.getValue( INSTANTIATION_STRATEGY_ATTRIBUTE,
                String.class, context );
        
        this.executionStrategy = ( String ) attributes.getValue( EXECUTION_STRATEGY_ATTRIBUTE,
                String.class, context );
        
        this.goal = (String)attributes.getValue(GOAL_ATTRIBUTE, String.class, context);
    }

    public List getDependencies(  )
    {
        return dependencies;
    }

    public MojoDescriptor getMojoDescriptor(  )
    {
        MojoDescriptor descriptor = new MojoDescriptor(  );

        descriptor.setId( id );
        
        if(instantiationStrategy != null) {
            descriptor.setInstantiationStrategy( instantiationStrategy );
        }
        
        if(executionStrategy != null) {
            descriptor.setExecutionStrategy( executionStrategy );
        }
        
        if(goal != null) {
            descriptor.setGoal(goal);
        }
        
        descriptor.setPrereqs(prereqs);
        descriptor.setParameters(parameters);

        return descriptor;
    }

    public void setPrereqs( List prereqs )
    {
        this.prereqs = prereqs;
    }

    public void setDependencies( List dependencies)
    {
        this.dependencies = dependencies;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public void setParameters(List parameters) {
        this.parameters = parameters;
    }
}
