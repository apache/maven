package org.apache.maven.project.processor;

import java.util.Properties;

import junit.framework.TestCase;

import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Notifier;

public class CiManagementProcessorTest
    extends TestCase
{
    public void testChildCopy()
    {
        CiManagement mng = new CiManagement();
        mng.setSystem( "system" );
        mng.setUrl( "uri" );

        Notifier notifier = new Notifier();
        notifier.setAddress( "address" );
        notifier.setType( "type" );
        Properties prop = new Properties();
        prop.put( "key", "value" );
        notifier.setConfiguration( prop );
        mng.addNotifier( notifier );

        Model child = new Model();
        child.setCiManagement( mng );

        Model target = new Model();

        CiManagementProcessor proc = new CiManagementProcessor();
        proc.process( null, child, target, false );

        assertEquals( "system", target.getCiManagement().getSystem() );
        assertEquals( "uri", target.getCiManagement().getUrl() );
        assertEquals( 1, target.getCiManagement().getNotifiers().size() );
        assertEquals( "address", target.getCiManagement().getNotifiers().get( 0 ).getAddress() );
    }
}
