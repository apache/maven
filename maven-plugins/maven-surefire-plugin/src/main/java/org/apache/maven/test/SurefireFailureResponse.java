package org.apache.maven.test;

import org.apache.maven.plugin.FailureResponse;

import java.util.List;
import java.util.Iterator;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public class SurefireFailureResponse
    extends FailureResponse
{
    private String LS = System.getProperty( "line.separator" );

    private String message;

    public SurefireFailureResponse( Object o )
    {
        super( o );
    }

    public String shortMessage()
    {
        return "There are some test failures.";
    }

    public String longMessage()
    {
        return shortMessage();
    }
}
