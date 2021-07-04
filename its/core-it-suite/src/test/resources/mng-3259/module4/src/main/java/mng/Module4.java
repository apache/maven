package mng;

import java.rmi.RemoteException;
import javax.ejb.CreateException;
import javax.ejb.EJBObject;


/**
 * An EJB for Module4
 */
public interface Module4 extends EJBObject
{
    public boolean doIt() throws RemoteException;
}

