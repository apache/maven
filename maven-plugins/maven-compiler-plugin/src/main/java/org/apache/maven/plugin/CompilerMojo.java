package org.apache.maven.plugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import org.apache.maven.plugin.util.scan.InclusionScanException;
import org.apache.maven.plugin.util.scan.SourceInclusionScanner;
import org.apache.maven.plugin.util.scan.StaleSourceScanner;
import org.apache.maven.plugin.util.scan.mapping.SuffixMapping;
import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerError;
import org.codehaus.plexus.compiler.javac.JavacCompiler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id$
 * @goal compile
 * @requiresDependencyResolution compile
 * @description Compiles application sources
 * @parameter name="compileSourceRoots" type="java.util.List" required="true" validator=""
 * expression="#project.compileSourceRoots" description=""
 * @parameter name="outputDirectory" type="String" required="true" validator=""
 * expression="#project.build.outputDirectory" description=""
 * @parameter name="classpathElements" type="List" required="true" validator=""
 * expression="#project.compileClasspathElements" description=""
 * @parameter name="debug" type="boolean" required="false" validator=""
 * expression="#maven.compiler.debug" description="Whether to include debugging
 * information in the compiled class files; the default value is false"
 * @todo change debug parameter type to Boolean
 * @parameter name="source" type="String" required="false" expression="#source" validator=""
 * description="The -source argument for the Java compiler"
 * @parameter name="target" type="String" required="false" expression="#target" validator=""
 * description="The -target argument for the Java compiler"
 * @parameter name="staleMillis" type="long" required="false" expression="#lastModGranularityMs"
 * validator="" description="The granularity in milliseconds of the last modification
 * date for testing whether a source needs recompilation"
 * @todo change staleMillis parameter type to Long
 */

public class CompilerMojo
    extends AbstractPlugin
{
    private Compiler compiler = new JavacCompiler();

    // TODO: use boolean when supported
    private String debug = Boolean.FALSE.toString();

    private List compileSourceRoots;

    private List classpathElements;

    private String outputDirectory;

    private String source;

    private String target;

    // TODO: Use long when supported
    private String staleMillis = "0";

    public void execute()
        throws PluginExecutionException
    {
        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        compileSourceRoots = removeEmptyCompileSourceRoots( compileSourceRoots );
        if ( compileSourceRoots.isEmpty() )
        {
            getLog().info( "No sources to compile" );
            return;
        }

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

        compilerConfiguration.setOutputLocation( outputDirectory );
        compilerConfiguration.setClasspathEntries( classpathElements );
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

        if ( debug != null && "true".equals( debug ) )
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
            throw new PluginExecutionException( "Fatal error compiling", e );
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
        throws PluginExecutionException
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
                throw new PluginExecutionException( "Invalid staleMillis plugin parameter value: \'" + staleMillis +
                                                    "\'", e );
            }

        }
        SuffixMapping mapping = new SuffixMapping( ".java", ".class" );

        SourceInclusionScanner scanner = new StaleSourceScanner( staleTime );

        scanner.addSourceMapping( mapping );

        File outDir = new File( outputDirectory );

        Set staleSources = new HashSet();

        for ( Iterator it = compileSourceRoots.iterator(); it.hasNext(); )
        {
            String sourceRoot = (String) it.next();

            File rootFile = new File( sourceRoot );

            try
            {
                staleSources.addAll( scanner.getIncludedSources( rootFile, outDir ) );
            }
            catch ( InclusionScanException e )
            {
                throw new PluginExecutionException( "Error scanning source root: \'" + sourceRoot +
                                                    "\' for stale files to recompile.", e );
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