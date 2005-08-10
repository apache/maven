package org.apache.plugins.csharp.compiler;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.plugins.csharp.helpers.AntBuildListener;
import org.codehaus.plexus.compiler.CompilerConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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


/**
 * @author <a href="mailto:gdodinet@karmicsoft.com">Gilles Dodinet</a>
 * @version $Id$
 * @description Compiles c# sources
 * @goal compile
 * @phase compile
 * @requiresDependencyResolution compile
 */
public class CSharpCompilerMojo
    extends AbstractMojo
{

    /**
     * @parameter
     */
    private String debug = Boolean.TRUE.toString();

    /**
     * @parameter
     */
    private String optimize = Boolean.TRUE.toString();

    /**
     * @parameter
     */
    private String unsafe = Boolean.FALSE.toString();

    /**
     * @parameter
     */
    private String incremental = Boolean.FALSE.toString();

    /**
     * @parameter
     */
    private String fullPaths = Boolean.TRUE.toString();

    /**
     * @parameter
     */
    private String warnLevel = "4";

    /**
     * @parameter
     */
    private String mainClass;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private String outputDirectory;

    /**
     * @parameter
     */
    private String destFile;

    /**
     * @parameter
     * @required
     */
    private String type = "library";

    /**
     * @parameter expression="${project.compileSourceRoots}"
     * @required
     * @readonly
     */
    private List compileSourceRoots;

    public void execute()
        throws MojoExecutionException
    {
        CompilerConfiguration config = new CompilerConfiguration();

        Map compilerOptions = new HashMap();

        config.setOutputLocation( outputDirectory );

        config.setSourceLocations( compileSourceRoots );

        compilerOptions.put( "optimize", Boolean.valueOf( optimize ) );
        compilerOptions.put( "unsafe", Boolean.valueOf( unsafe ) );
        compilerOptions.put( "incremental", Boolean.valueOf( incremental ) );
        compilerOptions.put( "fullPaths", Boolean.valueOf( fullPaths ) );
        compilerOptions.put( "warnLevel", Integer.valueOf( warnLevel ) );
        compilerOptions.put( "mainClass", mainClass );
        compilerOptions.put( "destFile", destFile );

        //until handlers ready
        compilerOptions.put( "type", type );
        //compilerOptions.put("type", project.getPackaging());

        config.setCompilerOptions( compilerOptions );
        CSharpCompiler compiler = new CSharpCompiler();
        compiler.setBasedir( project.getBasedir() );
        compiler.setAntBuildListener( new AntBuildListener( this.getLog() ) );

        String artifactList = "";
        Set artifacts = project.getArtifacts();
        int u = 0;
        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            if ( "dll".equals( artifact.getType() ) )
            {

                File file = artifact.getFile();

                artifactList += file.getAbsolutePath();
                if ( u < artifacts.size() - 1 )
                {
                    artifactList += ":";
                }

            }
            u++;
        }
        compiler.setReferences( artifactList );

        try
        {
            compiler.compile( config );
        }
        catch ( Exception e )
        {
            throw new MojoExecutionException( "Error compiling C# sources", e );
        }

    }

    /**
     * @return Returns the debug.
     */
    public String getDebug()
    {
        return debug;
    }

    /**
     * @param debug The debug to set.
     */
    public void setDebug( String debug )
    {
        this.debug = debug;
    }

    /**
     * @return Returns the fullPaths.
     */
    public String getFullPaths()
    {
        return fullPaths;
    }

    /**
     * @param fullPaths The fullPaths to set.
     */
    public void setFullPaths( String fullPaths )
    {
        this.fullPaths = fullPaths;
    }

    /**
     * @return Returns the incremental.
     */
    public String getIncremental()
    {
        return incremental;
    }

    /**
     * @param incremental The incremental to set.
     */
    public void setIncremental( String incremental )
    {
        this.incremental = incremental;
    }

    /**
     * @return Returns the mainClass.
     */
    public String getMainClass()
    {
        return mainClass;
    }

    /**
     * @param mainClass The mainClass to set.
     */
    public void setMainClass( String mainClass )
    {
        this.mainClass = mainClass;
    }

    /**
     * @return Returns the optimize.
     */
    public String getOptimize()
    {
        return optimize;
    }

    /**
     * @param optimize The optimize to set.
     */
    public void setOptimize( String optimize )
    {
        this.optimize = optimize;
    }

    /**
     * @return Returns the outputDirectory.
     */
    public String getOutputDirectory()
    {
        return outputDirectory;
    }

    /**
     * @param outputDirectory The outputDirectory to set.
     */
    public void setOutputDirectory( String outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    /**
     * @return Returns the compileSourceRoots.
     */
    public List getCompileSourceRoots()
    {
        return compileSourceRoots;
    }

    /**
     * @param compileSourceRoots The compileSourceRoots to set.
     */
    public void setCompileSourceRoots( List compileSourceRoots )
    {
        this.compileSourceRoots = compileSourceRoots;
    }

    /**
     * @return Returns the project.
     */
    public MavenProject getProject()
    {
        return project;
    }

    /**
     * @param project The project to set.
     */
    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    /**
     * @return Returns the unsafe.
     */
    public String getUnsafe()
    {
        return unsafe;
    }

    /**
     * @param unsafe The unsafe to set.
     */
    public void setUnsafe( String unsafe )
    {
        this.unsafe = unsafe;
    }

    /**
     * @return Returns the warnLevel.
     */
    public String getWarnLevel()
    {
        return warnLevel;
    }

    /**
     * @param warnLevel The warnLevel to set.
     */
    public void setWarnLevel( String warnLevel )
    {
        this.warnLevel = warnLevel;
    }

    /**
     * @return Returns the destFile.
     */
    public String getDestFile()
    {
        return destFile;
    }

    /**
     * @param destFile The destFile to set.
     */
    public void setDestFile( String destFile )
    {
        this.destFile = destFile;
    }

    /**
     * @return Returns the type.
     */
    public String getType()
    {
        return type;
    }

    /**
     * @param type The type to set.
     */
    public void setType( String type )
    {
        this.type = type;
    }
}
