package org.apache.maven.plugin;

import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerError;
import org.codehaus.plexus.compiler.javac.JavacCompiler;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @goal compile
 *
 * @requiresDependencyResolution
 *
 * @description Compiles application sources
 *
 * @parameter
 *  name="compileSourceRootsList"
 *  type="java.util.List"
 *  required="true"
 *  validator=""
 *  expression="#project.compileSourceRootsList"
 *  description=""
 *
 * @parameter
 *  name="outputDirectory"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#project.build.output"
 *  description=""
 *
 * @parameter
 *  name="classpathElements"
 *  type="String[]"
 *  required="true"
 *  validator=""
 *  expression="#project.classpathElements"
 *  description=""
 *
 * @parameter
 *  name="debug"
 *  type="String"
 *  required="false"
 *  validator=""
 *  expression="#maven.compiler.debug"
 *  description="Whether to include debugging information in the compiled class files; the default value is false"
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @todo change debug parameter type to Boolean
 */

public class CompilerMojo
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

        List compileSourceRootsList = (List) request.getParameter( "compileSourceRootsList" );

        String outputDirectory = (String) request.getParameter( "outputDirectory" );

        String[] classpathElements = (String[]) request.getParameter( "classpathElements" );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        compileSourceRootsList = removeEmptyCompileSourceRoots( compileSourceRootsList );
        if ( compileSourceRootsList.isEmpty() )
        {
            return;
        }

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        
        compilerConfiguration.setOutputLocation(outputDirectory);
        compilerConfiguration.setClasspathEntries(Arrays.asList(classpathElements));
        compilerConfiguration.setSourceLocations( compileSourceRootsList );
        
        /* Compile with debugging info */
        String debugAsString = (String) request.getParameter( "debug" );

        if ( debugAsString != null )
        {
            if ( Boolean.valueOf( debugAsString ).booleanValue() )
            {
                compilerConfiguration.setDebug( true );
            }
        }

        List messages = compiler.compile(compilerConfiguration);

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
        }

        if ( compilationError )
        {
            response.setExecutionFailure( new CompilationFailureResponse( messages ) );
        }
    }

    /** @todo also in ant plugin. This should be resolved at some point so that it does not need to be calculated continuously - or should the plugins accept empty source roots as is? */
    private static List removeEmptyCompileSourceRoots( List compileSourceRootsList )
    {
        List newCompileSourceRootsList = new ArrayList();
        if ( compileSourceRootsList != null )
        {
            // copy as I may be modifying it
            for ( Iterator i = compileSourceRootsList.iterator(); i.hasNext(); )
            {
                String srcDir = (String) i.next();
                if ( !newCompileSourceRootsList.contains( srcDir ) && new File( srcDir ).exists() )
                {
                    newCompileSourceRootsList.add( srcDir );
                }
            }
        }
        return newCompileSourceRootsList;
    }
}
