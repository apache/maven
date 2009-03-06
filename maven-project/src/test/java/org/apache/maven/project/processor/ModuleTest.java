package org.apache.maven.project.processor;

import java.util.Arrays;

import org.apache.maven.model.Model;

import junit.framework.TestCase;

public class ModuleTest extends TestCase {

	public void testIsMostSpecialized()
	{
		ModuleProcessor proc = new ModuleProcessor();
		
		Model targetModel = new Model();
		Model childModel = new Model();
		childModel.setModules(Arrays.asList("m1", "m2"));
		
		proc.process(null, childModel, targetModel, true);
		
		assertEquals(2, targetModel.getModules().size());
		assertEquals("m1", targetModel.getModules().get(0));
		assertEquals("m2", targetModel.getModules().get(1));
	}
	
	public void testIsNotMostSpecialized()
	{
		ModuleProcessor proc = new ModuleProcessor();
		
		Model targetModel = new Model();
		Model childModel = new Model();
		childModel.setModules(Arrays.asList("m1", "m2"));
		
		proc.process(null, childModel, targetModel, false);
		
		assertEquals(0, targetModel.getModules().size());
	}	
	
	public void testImmutable()
	{
		ModuleProcessor proc = new ModuleProcessor();
		
		Model targetModel = new Model();
		Model childModel = new Model();
		childModel.setModules(Arrays.asList("m1", "m2"));
		
		proc.process(null, childModel, targetModel, true);
		
		childModel.getModules().set(0, "m0");
		
		assertEquals("m1", targetModel.getModules().get(0));	
	}
	
}
