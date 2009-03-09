package org.apache.maven.project.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Notifier;

public class CiManagementProcessor
    extends BaseProcessor
{
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        Model t = (Model) target;
        Model c = (Model) child;
        Model p = (Model) parent;

        if ( c.getCiManagement() != null )
        {
            CiManagement childMng = c.getCiManagement();
            CiManagement mng = new CiManagement();

            mng.setSystem( childMng.getSystem() );
            mng.setUrl( childMng.getUrl() );
            t.setCiManagement( mng );
            addNotifiers( c.getCiManagement().getNotifiers(), t.getCiManagement() );
        }
        else if ( p != null && p.getCiManagement() != null )
        {
            CiManagement parentMng = p.getCiManagement();
            CiManagement mng = new CiManagement();

            mng.setSystem( parentMng.getSystem() );
            mng.setUrl( parentMng.getUrl() );
            t.setCiManagement( mng );
            addNotifiers( p.getCiManagement().getNotifiers(), t.getCiManagement() );
        }
    }

    private static void addNotifiers( List<Notifier> notifiers, CiManagement ciManagement )
    {
        if ( notifiers == null )
        {
            return;
        }
        List<Notifier> n = new ArrayList<Notifier>();

        for ( Notifier notifier : notifiers )
        {
            Notifier notifierCopy = new Notifier();

            Properties properties = new Properties();
            properties.putAll( notifier.getConfiguration() );
            notifierCopy.setConfiguration( properties );

            notifierCopy.setAddress( notifier.getAddress() );
            notifierCopy.setSendOnError( notifier.isSendOnError() );
            notifierCopy.setSendOnFailure( notifier.isSendOnFailure() );
            notifierCopy.setSendOnSuccess( notifier.isSendOnSuccess() );
            notifierCopy.setSendOnWarning( notifier.isSendOnWarning() );
            notifierCopy.setType( notifier.getType() );
            n.add( notifierCopy );

        }
        ciManagement.getNotifiers().addAll( n );
    }
}
