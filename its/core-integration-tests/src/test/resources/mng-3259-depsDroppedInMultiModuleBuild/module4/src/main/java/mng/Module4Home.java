package mng;

/**
 * Home interface for Module4.
 */
public interface Module4Home
   extends javax.ejb.EJBHome
{
   public static final String COMP_NAME="java:comp/env/ejb/Module4";
   public static final String JNDI_NAME="mng/Module4";

   public Module4 create()
      throws javax.ejb.CreateException,java.rmi.RemoteException;

}
