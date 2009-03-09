package org.apache.maven.project.processor;

import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.Model;

public class IssueManagementProcessor extends BaseProcessor
{
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        Model t = (Model) target;
        Model c = (Model) child;
        Model p = (Model) parent;
        
        if( c.getIssueManagement() != null)
        {
            IssueManagement childMng = c.getIssueManagement();
            IssueManagement mng = new IssueManagement();
            
            mng.setSystem( childMng.getSystem() );
            mng.setUrl( childMng.getUrl() );
            t.setIssueManagement( mng );
        } 
        else if(p != null && p.getIssueManagement() != null)
        {
            IssueManagement parentMng = p.getIssueManagement();
            IssueManagement mng = new IssueManagement();
            
            mng.setSystem( parentMng .getSystem() );
            mng.setUrl( parentMng .getUrl() ); 
            t.setIssueManagement( mng );
        }
    }
}
