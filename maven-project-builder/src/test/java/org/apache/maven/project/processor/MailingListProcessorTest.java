package org.apache.maven.project.processor;

import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;

import junit.framework.TestCase;

public class MailingListProcessorTest
    extends TestCase
{
    public void testChildCopy()
    {
        Model child = new Model();

        MailingList list = new MailingList();
        list.setArchive( "archive" );
        list.setPost( "post" );

        child.getMailingLists().add( list );

        Model target = new Model();

        MailingListProcessor proc = new MailingListProcessor();
        proc.process( null, child, target, false );

        assertEquals( "archive", target.getMailingLists().get( 0 ).getArchive() );

        list.setArchive( "aaa" );
        assertEquals( "archive", target.getMailingLists().get( 0 ).getArchive() );
    }

    public void testParentCopy()
    {
        Model child = new Model();

        Model parent = new Model();

        MailingList list = new MailingList();
        list.setArchive( "archive" );
        list.setPost( "post" );

        parent.getMailingLists().add( list );

        Model target = new Model();

        MailingListProcessor proc = new MailingListProcessor();
        proc.process( parent, child, target, false );

        assertEquals( "archive", target.getMailingLists().get( 0 ).getArchive() );

        list.setArchive( "aaa" );
        assertEquals( "archive", target.getMailingLists().get( 0 ).getArchive() );
    }
}
