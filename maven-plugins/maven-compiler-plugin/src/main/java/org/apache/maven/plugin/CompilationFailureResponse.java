package org.apache.maven.plugin;

import org.codehaus.plexus.compiler.CompilerError;

import java.util.List;
import java.util.Iterator;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class CompilationFailureResponse
    extends FailureResponse
{
    public CompilationFailureResponse( Object o )
    {
        super( o );
    }

    public String shortMessage()
    {
        return "Compilation failure";
    }

    public String longMessage()
    {
        StringBuffer sb = new StringBuffer();

        List messages = (List)source;

        for ( Iterator it = messages.iterator(); it.hasNext() ; )
        {
            CompilerError compilerError = (CompilerError) it.next();

            sb.append( compilerError );
        }

        return sb.toString();
    }
}
