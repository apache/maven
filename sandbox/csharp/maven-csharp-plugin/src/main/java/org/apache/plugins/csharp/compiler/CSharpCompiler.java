package org.apache.plugins.csharp.compiler;

/*
 * Copyright 2005 The Apache Software Foundation.
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

import org.apache.plugins.csharp.helpers.AntBuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.dotnet.CSharp;
import org.codehaus.plexus.compiler.AbstractCompiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:gdodinet@karmicsoft.com">Gilles Dodinet</a>
 * @version $Id$
 */
public class CSharpCompiler
    extends AbstractCompiler
{
    private File basedir;

    private AntBuildListener antBuildListener;

    private String references;

    public List compile( CompilerConfiguration config )
        throws Exception
    {

        CSharp csc = createCompiler( config );

        csc.execute();

        return null;
    }

    private CSharp createCompiler( CompilerConfiguration config )
        throws Exception
    {
        Map compilerOptions = config.getCompilerOptions();

        CSharp csc = new CSharp();

        Project antProject = new Project();
        antProject.setBaseDir( basedir );

        if ( antBuildListener == null )
        {
            antBuildListener = new AntBuildListener();
        }

        antProject.addBuildListener( antBuildListener );

        csc.setProject( antProject );

        csc.setOptimize( getBooleanOption( compilerOptions, "optimize", true ) );
        csc.setUnsafe( getBooleanOption( compilerOptions, "unsafe", false ) );
        csc.setIncremental( getBooleanOption( compilerOptions, "incremental", false ) );
        csc.setFullPaths( getBooleanOption( compilerOptions, "fullpaths", true ) );
        csc.setWarnLevel( getIntOption( compilerOptions, "warnLevel", 4 ) );
        csc.setDebug( getBooleanOption( compilerOptions, "debug", true ) );

        csc.setMainClass( (String) compilerOptions.get( "mainClass" ) );

        String type = (String) compilerOptions.get( "type" );

        csc.setTargetType( type );
        csc.setReferences( references );

        File destDir = new File( config.getOutputLocation() );
        if ( !destDir.exists() )
        {
            destDir.mkdirs();
        }
        csc.setDestDir( destDir );

        String destFileName = (String) compilerOptions.get( "destFile" );
        if ( destFileName == null )
        {
            destFileName = (String) compilerOptions.get( "mainClass" );
        }
        csc.setDestFile( new File( destDir, destFileName + "." + getTypeExtension( type ) ) );

        return csc;
    }

    /**
     * @param type
     * @return
     * @throws Exception
     */
    private String getTypeExtension( String type )
        throws Exception
    {
        if ( "exe".equals( type ) || "winexe".equals( type ) )
        {
            return "exe";
        }
        if ( "library".equals( type ) || "module".equals( type ) )
        {
            return "dll";
        }
        throw new Exception( "Unrecognized type" );
    }

    private boolean getBooleanOption( Map options, String optionName, boolean defaultValue )
    {
        Boolean optionValue = (Boolean) options.get( optionName );
        return optionValue != null ? optionValue.booleanValue() : defaultValue;
    }

    private int getIntOption( Map options, String optionName, int defaultValue )
    {
        Integer optionValue = (Integer) options.get( optionName );
        return optionValue != null ? optionValue.intValue() : defaultValue;
    }

    /**
     * @param basedir
     */
    public void setBasedir( File basedir )
    {
        this.basedir = basedir;
    }

    /**
     * @param antBuildListener The antBuildListener to set.
     */
    public void setAntBuildListener( AntBuildListener antBuildListener )
    {
        this.antBuildListener = antBuildListener;
    }

    /**
     * @param additionalModules The additionalModules to set.
     */
    public void setReferences( String additionalModules )
    {
        this.references = additionalModules;
    }
}
