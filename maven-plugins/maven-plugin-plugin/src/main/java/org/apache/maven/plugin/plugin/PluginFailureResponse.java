package org.apache.maven.plugin.plugin;

import org.apache.maven.plugin.FailureResponse;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public class PluginFailureResponse
    extends FailureResponse
{
    private String LS = System.getProperty( "line.separator" );

    private String message;

    public PluginFailureResponse( Object o )
    {
        super( o );
    }

    public String shortMessage()
    {
        return (String) source;
    }

    public String longMessage()
    {
        return shortMessage();
    }
}
