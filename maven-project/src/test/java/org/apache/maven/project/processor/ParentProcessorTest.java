package org.apache.maven.project.processor;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;

import junit.framework.TestCase;

public class ParentProcessorTest extends TestCase
{
    public void testVersion()
    {
        Model targetModel = new Model();
        Model childModel = new Model();     
        Parent parent = new Parent();
        parent.setVersion( "1.0" );
        childModel.setParent(parent);   
        
        ParentProcessor mp = new ParentProcessor();
        mp.process(null, childModel, targetModel, false);
        assertEquals("1.0", targetModel.getParent().getVersion());              
    }
}
