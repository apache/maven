package mng;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

/**
 * An EJB for Module4
 */
public class Module4Bean implements SessionBean
{
    public void ejbCreate() throws CreateException
    {
    }

    public void ejbRemove()
    {
    }

    public void ejbActivate()
    {
    }

    public void ejbPassivate()
    {
    }

    public void setSessionContext(SessionContext sessionContext)
    {
    }

    public boolean doIt()
    {
        return true;
    }
}
