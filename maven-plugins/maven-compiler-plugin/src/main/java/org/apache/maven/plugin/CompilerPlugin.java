package org.apache.maven.plugin;

import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerError;

import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.io.File;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 *
 * @version $Id$
 */

// Conditions underwhich we fail
// - specified source directory does not exist
// - missing classpath Elements
// - compilation error

// How to accurately report failures to users

public class CompilerPlugin
{
    private Map compilers;

    private String sourceDirectory;

    private String outputDirectory;

    private String[] classpathElements;

    private String compiler;

    private boolean debug = true;

    public void execute()
        throws Exception
    {
        if ( ! new File( sourceDirectory ).exists() )
        {
            throw new Exception( "The specified source directory '"+ sourceDirectory + "' does not exist!" );
        }

        Compiler compiler = (Compiler) compilers.get( this.compiler );

        List messages = compiler.compile( classpathElements, new String[]{sourceDirectory}, outputDirectory );

        if ( debug )
        {
            for ( int i = 0; i < classpathElements.length; i++ )
            {
                String message;

                if ( new File( classpathElements[i] ).exists() )
                {
                    message = "present in repository.";
                }
                else
                {
                    message = "Warning! not present in repository!";
                }

                System.out.println( "classpathElements[ "+ i +" ] = " + classpathElements[i] + ": " + message );
            }
        }

        boolean compilationError = false;

        for ( Iterator i = messages.iterator(); i.hasNext(); )
        {
            CompilerError message = (CompilerError) i.next();

            if ( message.isError() )
            {
                compilationError = true;
            }

            System.out.println( message.getMessage() );
        }

        if ( compilationError )
        {
            throw new Exception( "Compilation failure!" );
        }
    }
}
