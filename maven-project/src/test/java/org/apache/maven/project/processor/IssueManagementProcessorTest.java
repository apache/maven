package org.apache.maven.project.processor;

import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.Model;

import junit.framework.TestCase;

public class IssueManagementProcessorTest extends TestCase
{
    public void testChildCopy()
    {
        IssueManagement mng = new IssueManagement();
        mng.setSystem( "system" );
        mng.setUrl( "http://url" );
        
        Model child = new Model();
        child.setIssueManagement( mng );
        
        Model target = new Model();
        
        IssueManagementProcessor proc = new IssueManagementProcessor();
        proc.process( null, child, target, false );
        
        assertEquals("system", target.getIssueManagement().getSystem());
        assertEquals("http://url", target.getIssueManagement().getUrl());
    }
}
