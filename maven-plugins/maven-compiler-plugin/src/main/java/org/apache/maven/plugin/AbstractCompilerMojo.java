package org.apache.maven.plugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerError;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.manager.CompilerManager;
import org.codehaus.plexus.compiler.manager.NoSuchCompilerException;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;
import org.codehaus.plexus.compiler.util.scan.mapping.SourceMapping;
import org.codehaus.plexus.compiler.util.scan.mapping.SingleTargetSourceMapping;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map;

/**
 * @author others
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id: StaleSourceScannerTest.java 2393 2005-08-08 22:32:59Z kenney $
 */
public abstract class AbstractCompilerMojo
    extends AbstractMojo
{
    /**
     * Whether to include debugging information in the compiled class files.
     * The default value is true.
     *
     * @parameter expression="${maven.compiler.debug}" default-value="true"
     */
    private boolean debug;

    /**
     * Output source locations where deprecated APIs are used
     *
     * @parameter
     */
    private boolean showDeprecation;

    /**
     * Output warnings
     *
     * @parameter
     */
    private boolean showWarnings;

    /**
     * The -source argument for the Java compiler
     *
     * @parameter
     */
    private String source;

    /**
     * The -target argument for the Java compiler
     *
     * @parameter
     */
    private String target;

    /**
     * The -encoding argument for the Java compiler
     *
     * @parameter
     */
    private String encoding;

    /**
     * The granularity in milliseconds of the last modification
     * date for testing whether a source needs recompilation
     *
     * @parameter expression="${lastModGranularityMs}" default-value="0"
     */
    private int staleMillis;

    /**
     * @parameter default-value="javac"
     */
    private String compilerId;

    /**
     * Runs the compiler in a separate process.
     *
     * If not set the compiler will default to a executable.
     *
     * @parameter default-value="false"
     */
    private boolean fork;

    /**
     * The executable of the compiler to use.
     *
     * @parameter
     */
    private String executable;

    /**
     * Arguements to be passed to the compiler if fork is set to true.
     *
     * This is because the list of valid arguements passed to a Java compiler
     * varies based on the compiler version.
     *
     * @parameter
     */
    private Map compilerArguments;

    /**
     * The directory to run the compiler from if fork is true.
     *
     * @parameter expression="${basedir}"
     * @required
     * @readonly
     */
    private File basedir;

    /**
     * @parameter expression="${component.org.codehaus.plexus.compiler.manager.CompilerManager}"
     * @required
     * @readonly
     */
    private CompilerManager compilerManager;

    protected abstract List getClasspathElements();

    protected abstract List getCompileSourceRoots();

    protected abstract File getOutputDirectory();

    public void execute()
        throws MojoExecutionException
    {
        // ----------------------------------------------------------------------
        // Look up the compiler. This is done before other code than can
        // cause the mojo to return before the lookup is done possibly resulting
        // in misconfigured POMs still building.
        // ----------------------------------------------------------------------

        Compiler compiler;

        try
        {
            compiler = compilerManager.getCompiler( compilerId );
        }
        catch ( NoSuchCompilerException e )
        {
            throw new MojoExecutionException( "No such compiler '" + e.getCompilerId() + "'." );
        }

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        List compileSourceRoots = removeEmptyCompileSourceRoots( getCompileSourceRoots() );

        if ( compileSourceRoots.isEmpty() )
        {
            getLog().info( "No sources to compile" );

            return;
        }

        // ----------------------------------------------------------------------
        // Create the compiler configuration
        // ----------------------------------------------------------------------

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        compilerConfiguration.setOutputLocation( getOutputDirectory().getAbsolutePath() );

        compilerConfiguration.setClasspathEntries( getClasspathElements() );

        compilerConfiguration.setSourceLocations( compileSourceRoots );

        compilerConfiguration.setDebug( debug );

        compilerConfiguration.setShowWarnings( showWarnings );

        compilerConfiguration.setShowDeprecation( showDeprecation );

        compilerConfiguration.setSourceVersion( source );

        compilerConfiguration.setTargetVersion( target );

        compilerConfiguration.setSourceEncoding( encoding );

        compilerConfiguration.setCustomCompilerArguments( compilerArguments );

        compilerConfiguration.setFork( fork );

        compilerConfiguration.setExecutable( executable );

        compilerConfiguration.setWorkingDirectory( basedir );

        // TODO: have an option to always compile (without need to clean)
        Set staleSources;

        try
        {
            staleSources = computeStaleSources( compilerConfiguration, compiler );
        }
        catch ( CompilerException e )
        {
            throw new MojoExecutionException( "Error while computing stale sources.", e );
        }

        if ( staleSources.isEmpty() )
        {
            getLog().info( "Nothing to compile - all classes are up to date" );

            return;
        }
        else
        {
            compilerConfiguration.setSourceFiles( staleSources );
        }

        // ----------------------------------------------------------------------
        // Dump configuration
        // ----------------------------------------------------------------------

        if ( getLog().isDebugEnabled() )
        {
            getLog().debug( "Classpath:" );

            for ( Iterator it = getClasspathElements().iterator(); it.hasNext(); )
            {
                String s = (String) it.next();

                getLog().debug( " " + s );
            }

            getLog().debug( "Source roots:" );

            for ( Iterator it = getCompileSourceRoots().iterator(); it.hasNext(); )
            {
                String root = (String) it.next();

                getLog().debug( " " + root );
            }
        }

        // ----------------------------------------------------------------------
        // Compile!
        // ----------------------------------------------------------------------

        List messages;

        getLog().debug( "Using compiler '" + compilerId + "'." );

        try
        {
            messages = compiler.compile( compilerConfiguration );
        }
        catch ( Exception e )
        {
            // TODO: don't catch Exception
            throw new MojoExecutionException( "Fatal error compiling", e );
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
            throw new CompilationFailureException( messages );
        }
    }

    private Set computeStaleSources( CompilerConfiguration compilerConfiguration, Compiler compiler )
        throws MojoExecutionException, CompilerException
    {
        CompilerOutputStyle outputStyle = compiler.getCompilerOutputStyle();

        SourceMapping mapping;

        if ( outputStyle == CompilerOutputStyle.ONE_OUTPUT_FILE_PER_INPUT_FILE )
        {
            mapping = new SuffixMapping( compiler.getInputFileEnding( compilerConfiguration ),
                                         compiler.getOutputFileEnding( compilerConfiguration ) );
        }
        else if ( outputStyle == CompilerOutputStyle.ONE_OUTPUT_FILE_FOR_ALL_INPUT_FILES )
        {
            mapping = new SingleTargetSourceMapping( compiler.getOutputFile( compilerConfiguration ) );
        }
        else
        {
            throw new MojoExecutionException( "Unknown compiler output style: '" + outputStyle + "'." );
        }

        SourceInclusionScanner scanner = new StaleSourceScanner( staleMillis );

        scanner.addSourceMapping( mapping );

        Set staleSources = new HashSet();

        for ( Iterator it = getCompileSourceRoots().iterator(); it.hasNext(); )
        {
            String sourceRoot = (String) it.next();

            File rootFile = new File( sourceRoot );

            if ( !rootFile.isDirectory() )
            {
                continue;
            }

            try
            {
                staleSources.addAll( scanner.getIncludedSources( rootFile, getOutputDirectory() ) );
            }
            catch ( InclusionScanException e )
            {
                throw new MojoExecutionException( "Error scanning source root: \'" + sourceRoot + "\' " +
                                                  "for stale files to recompile.", e );
            }
        }

        return staleSources;
    }

    /**
     * @todo also in ant plugin. This should be resolved at some point so that it does not need to
     * be calculated continuously - or should the plugins accept empty source roots as is?
     */
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
