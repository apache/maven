package org.apache.maven.plugin;

import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerError;
import org.codehaus.plexus.compiler.javac.JavacCompiler;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * @maven.plugin.id compiler
 * @maven.plugin.description A maven2 plugin compiles project sources.
 *
 * @parameter sourceDirectory String required validator
 * @parameter outputDirectory String required validator
 * @parameter classpathElements String[] required validator
 * @parameter compiler String required validator
 * 
 * @goal.name compile
 * @goal.compile.description Compiles application sources
 * @goal.compile.parameter sourceDirectory #project.build.sourceDirectory
 * @goal.compile.parameter outputDirectory #maven.build.dest
 * @goal.compile.parameter classpathElements #project.classpathElements
 *
 * @goal.name test:compile
 * @goal.test:compile.prereq compile
 * @goal.test:compile.description Compiles test sources
 * @goal.test:compile.parameter sourceDirectory #project.build.unitTestSourceDirectory
 * @goal.test:compile.parameter outputDirectory #maven.test.dest
 * @goal.test:compile.parameter classpathElements #project.classpathElements
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
    private Compiler compiler = new JavacCompiler();

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
