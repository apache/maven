package org.apache.maven.project.processor;

import java.util.Properties;

import junit.framework.TestCase;

import org.apache.maven.model.Model;

public class PropertiesProcessorTest extends TestCase
{
    public void testParentChildMerge()
    {
        Model targetModel = new Model();
        Model childModel = new Model(); 
        Model parentModel = new Model(); 
        
        Properties childProperties = new Properties();
        childProperties.put( "k1", "v1" );
        childModel.setProperties( childProperties );
        
        Properties parentProperties = new Properties();
        parentProperties.put( "k2", "v2" );
        parentModel.setProperties( parentProperties );
        
        PropertiesProcessor proc = new PropertiesProcessor();
        proc.process( parentModel, childModel, targetModel, false );
        
        assertEquals(2, targetModel.getProperties().size());
        
        //Test order of child first
        /*
        ArrayList list = Collections.list( targetModel.getProperties().elements() ); 
        targetModel.getProperties().list( System.out );      
        assertEquals("v1", list.get( 0 ));
        assertEquals("v2", list.get( 1 ));
        */
    }
    
    public void testChildCopy()
    {
        Model targetModel = new Model();
        Model childModel = new Model(); 
        
        Properties childProperties = new Properties();
        childProperties.put( "k1", "v1" );
        childModel.setProperties( childProperties );
         
        PropertiesProcessor proc = new PropertiesProcessor();
        proc.process( null, childModel, targetModel, false );
        
        assertEquals(1, targetModel.getProperties().size());
    }   
    
    public void testParentCopy()
    {
        Model targetModel = new Model();
        Model childModel = new Model(); 
        Model parentModel = new Model(); 
        
        Properties parentProperties = new Properties();
        parentProperties.put( "k2", "v2" );
        parentModel.setProperties( parentProperties );
         
        PropertiesProcessor proc = new PropertiesProcessor();
        proc.process( parentModel, childModel, targetModel, false );
        
        assertEquals(1, targetModel.getProperties().size());
    }    
}
