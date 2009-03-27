package org.apache.maven.project.processor;

import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.Model;

import junit.framework.TestCase;

public class IssueManagementProcessorTest
    extends TestCase
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

        assertEquals( "system", target.getIssueManagement().getSystem() );
        assertEquals( "http://url", target.getIssueManagement().getUrl() );
    }

    /**
     * If child does not have issue management node, then use parent.
     */
    public void testParentCopy()
    {
        IssueManagement mng = new IssueManagement();
        mng.setSystem( "system" );
        mng.setUrl( "http://url" );

        Model child = new Model();

        Model parent = new Model();
        ;
        parent.setIssueManagement( mng );

        Model target = new Model();

        IssueManagementProcessor proc = new IssueManagementProcessor();
        proc.process( parent, child, target, false );

        assertEquals( "system", target.getIssueManagement().getSystem() );
        assertEquals( "http://url", target.getIssueManagement().getUrl() );
    }

    public void testChildCopy_DontInheritParent()
    {
        IssueManagement mng = new IssueManagement();
        mng.setSystem( "system" );
        mng.setUrl( "http://url" );

        Model child = new Model();
        child.setIssueManagement( mng );

        IssueManagement mng1 = new IssueManagement();
        Model parent = new Model();
        ;
        mng1.setSystem( "system-1" );
        mng1.setUrl( "http://url-1" );
        parent.setIssueManagement( mng1 );

        Model target = new Model();

        IssueManagementProcessor proc = new IssueManagementProcessor();
        proc.process( parent, child, target, false );

        assertEquals( "system", target.getIssueManagement().getSystem() );
        assertEquals( "http://url", target.getIssueManagement().getUrl() );
    }
}
