package org.apache.maven.plugin;

import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerError;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @plugin.id compiler
 * @plugin.description A maven2 plugin which integrates the use of Maven2 with IntelliJ's IDEA
 *
 * @parameter <name> <type> <required> <validatator> <description>
 * 
 * This may be on a per method basis
 * @parameter sourceDirectories String[] required validator
 * @parameter outputDirectory String required validator
 * @parameter classpathElements String[] required validator
 * @parameter compiler String required validator
 * 
 * The goal would map to a method if multiple methods were allowed
 * @goal.name idea
 * @goal.idea.parameter project #project
 *
 * There could be threadsafe and non threadsafe versions of a compiler
 * plugin. The case where you instantiate a compiler plugin that maintains
 * a reference to an incremental compiler.
 * 
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @todo make a plugin for each plugin type so that they are not all globbed in here
 *       this will remove the magic plexus does with Map requirements which will make
 *       reuse outside of the maven/plexus context easier
 * @todo use compile source roots and not the pom.build.sourceDirectory so that any
 *       sort of preprocessing and/or source generation can be taken into consideration.
 */

public class CompilerPlugin
    extends AbstractPlugin
{
    private Map compilers;

    private boolean debug = false;

    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {
        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        String sourceDirectory = (String) request.getParameter( "sourceDirectory" );

        String outputDirectory = (String) request.getParameter( "outputDirectory" );

        String[] classpathElements = (String[]) request.getParameter( "classpathElements" );

        String compilerId = (String) request.getParameter( "compiler" );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------
        
        if ( ! new File( sourceDirectory ).exists() )
        {
            return;
        }

        Compiler compiler = (Compiler) compilers.get( compilerId );

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
