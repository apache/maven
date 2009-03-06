package org.apache.maven.project.processor;

import java.util.Collection;

import org.apache.maven.model.Model;
/*
 * hold original pom
 * Track where a property is from
 */
public class ModelProcessor extends BaseProcessor
{
		
	public ModelProcessor(Collection<Processor> processors) 
	{
		super(processors);
	}
	
	public void process(Object parent, Object child, Object target, boolean isChildMostSpecialized)
	{
		super.process(parent, child, target, isChildMostSpecialized);

		Model c = (Model) child;
		Model t = (Model) target;
		Model p = null;
		if (parent != null)
		{
			p = (Model) parent;	
		}
				
		//Version
		if(c.getVersion() == null)
		{
			if(c.getParent() != null)
			{
				t.setVersion(c.getParent().getVersion());
			}			
		}
		else
		{
			t.setVersion(c.getVersion());	
		}
		
		//GroupId
		if(c.getGroupId() == null)
		{
			if(c.getParent() != null)
			{
				t.setGroupId(c.getParent().getGroupId());	
			}			
		}
		else
		{
			t.setGroupId(c.getGroupId());	
		}	
		
		t.setModelVersion(c.getModelVersion());
		t.setPackaging(c.getPackaging());
		
		if(isChildMostSpecialized)
		{
			t.setName(c.getName());
			t.setDescription(c.getDescription());
		}
					
	}
}
