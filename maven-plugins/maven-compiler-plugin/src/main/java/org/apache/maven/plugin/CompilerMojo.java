package org.apache.maven.plugin;

import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerError;
import org.codehaus.plexus.compiler.javac.JavacCompiler;

import java.io.File;
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
 *  name="sourceDirectory"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#project.build.sourceDirectory"
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
 * @todo use compile source roots and not the pom.build.sourceDirectory so that any
 *       sort of preprocessing and/or source generation can be taken into consideration.
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

        String sourceDirectory = (String) request.getParameter( "sourceDirectory" );

        String outputDirectory = (String) request.getParameter( "outputDirectory" );

        String[] classpathElements = (String[]) request.getParameter( "classpathElements" );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------
        
        if ( ! new File( sourceDirectory ).exists() )
        {
            return;
        }
        
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        
        compilerConfiguration.setOutputLocation(outputDirectory);
        compilerConfiguration.setClasspathEntries(Arrays.asList(classpathElements));
        compilerConfiguration.setSourceLocations(Arrays.asList(new String[] {sourceDirectory}));
        
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
}
