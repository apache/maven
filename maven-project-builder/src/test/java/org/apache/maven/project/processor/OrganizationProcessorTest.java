package org.apache.maven.project.processor;

import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;

import junit.framework.TestCase;

public class OrganizationProcessorTest
    extends TestCase
{
    public void testChildCopy()
    {
        Model child = new Model();
        Organization org = new Organization();
        org.setName( "name" );
        org.setUrl( "url" );

        child.setOrganization( org );

        Model target = new Model();

        OrganizationProcessor proc = new OrganizationProcessor();
        proc.process( null, child, target, false );

        assertNotNull( target.getOrganization() );
        assertEquals( "name", target.getOrganization().getName() );
        assertEquals( "url", target.getOrganization().getUrl() );

        org.setName( "new" );
        assertEquals( "name", target.getOrganization().getName() );
    }
    
    public void testParentCopy()
    {
        Model child = new Model();
        Organization org = new Organization();
        org.setName( "name" );
        org.setUrl( "url" );

        Model parent = new Model();
        parent.setOrganization( org );

        Model target = new Model();

        OrganizationProcessor proc = new OrganizationProcessor();
        proc.process( parent, child, target, false );

        assertNotNull( target.getOrganization() );
        assertEquals( "name", target.getOrganization().getName() );
        assertEquals( "url", target.getOrganization().getUrl() );

        org.setName( "new" );
        assertEquals( "name", target.getOrganization().getName() );
    }  
}
