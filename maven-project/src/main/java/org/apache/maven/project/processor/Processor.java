package org.apache.maven.project.processor;

public interface Processor {
	void process(Object parent, Object child, Object target, boolean isChildMostSpecialized);
	
	Object getParent();
	
	Object getChild();
}
