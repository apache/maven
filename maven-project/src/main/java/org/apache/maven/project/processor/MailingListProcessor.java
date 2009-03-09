package org.apache.maven.project.processor;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;

public class MailingListProcessor
    extends BaseProcessor
{

    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        Model t = (Model) target;
        Model c = (Model) child;
        Model p = (Model) parent;

        if ( !c.getMailingLists().isEmpty() )
        {
            copyMailingLists( c.getMailingLists(), t );
        }
        else if ( p != null && !p.getMailingLists().isEmpty() )
        {
            copyMailingLists( p.getMailingLists(), t );
        }
    }

    private static void copyMailingLists( List<MailingList> mailingLists, Model target )
    {
        List<MailingList> targetList = target.getMailingLists();
        for ( MailingList mailingList : mailingLists )
        {
            MailingList listCopy = new MailingList();
            listCopy.setArchive( mailingList.getArchive() );
            listCopy.setName( mailingList.getName() );
            listCopy.setOtherArchives( new ArrayList<String>( mailingList.getOtherArchives() ) );
            listCopy.setPost( mailingList.getPost() );
            listCopy.setSubscribe( mailingList.getSubscribe() );
            listCopy.setUnsubscribe( mailingList.getUnsubscribe() );
            targetList.add( listCopy );
        }
    }
}
