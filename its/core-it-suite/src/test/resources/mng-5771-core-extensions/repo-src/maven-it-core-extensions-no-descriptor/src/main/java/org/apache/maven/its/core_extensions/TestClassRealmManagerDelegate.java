package org.apache.maven.its.core_extensions;

import org.apache.maven.classrealm.ClassRealmManagerDelegate;
import org.apache.maven.classrealm.ClassRealmRequest;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;

@Component( role = ClassRealmManagerDelegate.class, hint = "TestClassRealmManagerDelegate" )
public class TestClassRealmManagerDelegate
    implements ClassRealmManagerDelegate
{
    public void setupRealm( ClassRealm classRealm, ClassRealmRequest request )
    {
    }
}
