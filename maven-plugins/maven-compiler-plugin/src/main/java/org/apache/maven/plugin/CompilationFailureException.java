package org.apache.maven.plugin;

import org.codehaus.plexus.compiler.CompilerError;

import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class CompilationFailureException
    extends PluginExecutionException
{
    private static final String LS = System.getProperty( "line.separator" );

    public CompilationFailureException( List messages )
    {
        // TODO: this is a bit nasty
        super( messages, "Compilation failure", longMessage( messages ) );
    }

    public static String longMessage( List messages )
    {
        StringBuffer sb = new StringBuffer();

        for ( Iterator it = messages.iterator(); it.hasNext() ; )
        {
            CompilerError compilerError = (CompilerError) it.next();

            sb.append( compilerError ).append( LS );
        }

        return sb.toString();
    }
}
