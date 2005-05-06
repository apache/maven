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
import org.codehaus.plexus.compiler.javac.JavacCompiler;
import org.codehaus.plexus.compiler.util.scan.InclusionScanException;
import org.codehaus.plexus.compiler.util.scan.SourceInclusionScanner;
import org.codehaus.plexus.compiler.util.scan.StaleSourceScanner;
import org.codehaus.plexus.compiler.util.scan.mapping.SuffixMapping;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public abstract class AbstractCompilerMojo
    extends AbstractMojo
{

    private Compiler compiler = new JavacCompiler();
    
    // TODO: use boolean when supported
    /**
     * Whether to include debugging information in the compiled class files.
     * <br/>
     * <br/>
     * The default value is true.
     * 
     * @parameter expression="${maven.compiler.debug}"
     */
    private String debug = Boolean.TRUE.toString();
    
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
    
    // TODO: Use long when supported
    /**
     * The granularity in milliseconds of the last modification
     * date for testing whether a source needs recompilation
     * 
     * @parameter alias="${lastModGranularityMs}"
     */
    private String staleMillis = "0";
    
    protected abstract List getClasspathElements();

    protected abstract List getCompileSourceRoots();

    protected abstract String getOutputDirectory();

    public void execute()
        throws MojoExecutionException
    {
        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        List compileSourceRoots = removeEmptyCompileSourceRoots( getCompileSourceRoots() );
        if ( compileSourceRoots.isEmpty() )
        {
            getLog().info( "No sources to compile" );
            return;
        }

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        compilerConfiguration.setOutputLocation( getOutputDirectory() );
        compilerConfiguration.setClasspathEntries( getClasspathElements() );
        compilerConfiguration.setSourceLocations( compileSourceRoots );

        // TODO: have an option to always compile (without need to clean)
        Set staleSources = computeStaleSources();

        if ( staleSources.isEmpty() )
        {
            getLog().info( "Nothing to compile - all classes are up to date" );
            return;
        }
        else
        {
            compilerConfiguration.setSourceFiles( staleSources );
        }

        if ( source != null )
        {
            compilerConfiguration.addCompilerOption( "-source", source );
        }

        if ( target != null )
        {
            compilerConfiguration.addCompilerOption( "-target", target );
        }

        if ( debug != null && Boolean.valueOf( debug ).booleanValue() )
        {
            compilerConfiguration.setDebug( true );
        }

        List messages = null;
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

    private Set computeStaleSources()
        throws MojoExecutionException
    {
        long staleTime = 0;

        if ( staleMillis != null && staleMillis.length() > 0 )
        {
            try
            {
                staleTime = Long.parseLong( staleMillis );
            }
            catch ( NumberFormatException e )
            {
                throw new MojoExecutionException( "Invalid staleMillis plugin parameter value: \'" + staleMillis
                    + "\'", e );
            }

        }
        SuffixMapping mapping = new SuffixMapping( ".java", ".class" );

        SourceInclusionScanner scanner = new StaleSourceScanner( staleTime );

        scanner.addSourceMapping( mapping );

        File outDir = new File( getOutputDirectory() );

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
                staleSources.addAll( scanner.getIncludedSources( rootFile, outDir ) );
            }
            catch ( InclusionScanException e )
            {
                throw new MojoExecutionException( "Error scanning source root: \'" + sourceRoot
                    + "\' for stale files to recompile.", e );
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
