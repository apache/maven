/* Created on Sep 21, 2004 */
package org.apache.maven.util;

/**
 * @author jdcasey
 */
public class GraphTraversalException
    extends Exception
{

    public GraphTraversalException( Throwable cause )
    {
        super( cause );
    }

    public GraphTraversalException( String message, Throwable cause )
    {
        super( message, cause );
    }

}