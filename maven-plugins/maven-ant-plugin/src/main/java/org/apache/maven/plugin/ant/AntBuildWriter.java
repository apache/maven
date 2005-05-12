package org.apache.maven.plugin.ant;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class AntBuildWriter
{
    private MavenProject project;

    private File localRepository;

    public AntBuildWriter( MavenProject project, File localRepository )
    {
        this.project = project;
        this.localRepository = localRepository;
    }

    public void write()
        throws AntPluginException
    {
        writeBuildXml();

        System.out.println(
            "Wrote Ant project for " + project.getArtifactId() + " to " + project.getBasedir().getAbsolutePath() );
    }

    // ----------------------------------------------------------------------
    // build.xml
    // ----------------------------------------------------------------------

    protected void writeBuildXml()
        throws AntPluginException
    {
        FileWriter w;

        try
        {
            // TODO: parameter
            w = new FileWriter( new File( project.getBasedir(), "build.xml" ) );
        }
        catch ( IOException ex )
        {
            throw new AntPluginException( "Exception while opening file.", ex );
        }

        XMLWriter writer = new PrettyPrintXMLWriter( w );

        writer.startElement( "project" );
        writer.addAttribute( "name", project.getArtifactId() );
        writer.addAttribute( "default", "jar" );
        writer.addAttribute( "basedir", "." );

        writeProperties( writer );

        writeBuildPathDefinition( writer );

        writeCleanTarget( writer );
        List compileSourceRoots = removeEmptyCompileSourceRoots( project.getCompileSourceRoots() );
        writeCompileTarget( writer, compileSourceRoots );

        // TODO: what if type is not JAR?
        writeJarTarget( writer );

        List testCompileSourceRoots = removeEmptyCompileSourceRoots( project.getTestCompileSourceRoots() );
        writeCompileTestsTarget( writer, testCompileSourceRoots );
        writeTestTargets( writer, testCompileSourceRoots );

        writeGetDepsTarget( writer );

        writer.endElement(); // project

        close( w );
    }

    private void writeCompileTestsTarget( XMLWriter writer, List testCompileSourceRoots )
    {
        writer.startElement( "target" );
        writer.addAttribute( "name", "compile-tests" );
        writer.addAttribute( "depends", "junit-present, compile" );
        writer.addAttribute( "description", "Compile the test code" );
        writer.addAttribute( "if", "junit.present" );

        writeCompileTasks( writer, project.getBasedir(), "${maven.test.output}", testCompileSourceRoots,
                           project.getBuild().getTestResources(), "${maven.build.output}" );

        writer.endElement(); // target
    }

    private void writeTestTargets( XMLWriter writer, List testCompileSourceRoots )
    {
        writer.startElement( "target" );
        writer.addAttribute( "name", "test" );
        writer.addAttribute( "depends", "junit-present, compile-tests" );
        writer.addAttribute( "if", "junit.present" );
        writer.addAttribute( "description", "Run the test cases" );

        if ( !testCompileSourceRoots.isEmpty() )
        {
            writer.startElement( "mkdir" );
            writer.addAttribute( "dir", "${maven.test.reports}" );
            writer.endElement(); //mkdir

            writer.startElement( "junit" );
            writer.addAttribute( "printSummary", "yes" );
            writer.addAttribute( "haltonerror", "true" );
            writer.addAttribute( "haltonfailure", "true" );
            writer.addAttribute( "fork", "true" );
            writer.addAttribute( "dir", "." );

            writer.startElement( "sysproperty" );
            writer.addAttribute( "key", "basedir" );
            writer.addAttribute( "value", "." );
            writer.endElement(); // sysproperty

            writer.startElement( "formatter" );
            writer.addAttribute( "type", "xml" );
            writer.endElement(); // formatter

            writer.startElement( "formatter" );
            writer.addAttribute( "type", "plain" );
            writer.addAttribute( "usefile", "false" );
            writer.endElement(); // formatter

            writer.startElement( "classpath" );
            writer.startElement( "path" );
            writer.addAttribute( "refid", "build.classpath" );
            writer.endElement(); // path
            writer.startElement( "pathelement" );
            writer.addAttribute( "location", "${maven.build.output}" );
            writer.endElement(); // pathelement
            writer.startElement( "pathelement" );
            writer.addAttribute( "location", "${maven.test.output}" );
            writer.endElement(); // pathelement
            writer.endElement(); // classpath

            writer.startElement( "batchtest" );
            for ( Iterator i = testCompileSourceRoots.iterator(); i.hasNext(); )
            {
                writer.startElement( "fileset" );
                String testSrcDir = (String) i.next();
                writer.addAttribute( "dir", toRelative( project.getBasedir(), testSrcDir ) );
/* TODO: need to get these from the test plugin somehow?
                UnitTest unitTest = project.getBuild().getUnitTest();
                writeIncludesExcludes( writer, unitTest.getIncludes(), unitTest.getExcludes() );
                // TODO: m1 allows additional test exclusions via maven.ant.excludeTests
*/
                writeIncludesExcludes( writer, Collections.singletonList( "**/*Test.java" ),
                                       Collections.singletonList( "**/*Abstract*Test.java" ) );
                writer.endElement(); // fileset
            }
            writer.endElement(); // batchtest

            writer.endElement(); // junit
        }
        writer.endElement(); // target

        writer.startElement( "target" );
        writer.addAttribute( "name", "test-junit-present" );

        writer.startElement( "available" );
        writer.addAttribute( "classname", "junit.framework.Test" );
        writer.addAttribute( "property", "junit.present" );
        writer.endElement(); // available

        writer.endElement(); // target

        writer.startElement( "target" );
        writer.addAttribute( "name", "junit-present" );
        writer.addAttribute( "depends", "test-junit-present" );
        writer.addAttribute( "unless", "junit.present" );

        writer.startElement( "echo" );
        writer.writeText( "================================= WARNING ================================" );
        writer.endElement(); // echo

        writer.startElement( "echo" );
        writer.writeText( " Junit isn't present in your $ANT_HOME/lib directory. Tests not executed. " );
        writer.endElement(); // echo

        writer.startElement( "echo" );
        writer.writeText( "==========================================================================" );
        writer.endElement(); // echo

        writer.endElement(); // target
    }

    private void writeJarTarget( XMLWriter writer )
    {
        writer.startElement( "target" );
        writer.addAttribute( "name", "jar" );
        writer.addAttribute( "depends", "compile,test" );
        writer.addAttribute( "description", "Clean the JAR" );

        writer.startElement( "jar" );
        writer.addAttribute( "jarfile", "${maven.build.directory}/${maven.build.final.name}.jar" );
        writer.addAttribute( "basedir", "${maven.build.output}" );
        writer.addAttribute( "excludes", "**/package.html" );
        writer.endElement(); // jar

        writer.endElement(); // target
    }

    private void writeCleanTarget( XMLWriter writer )
    {
        writer.startElement( "target" );
        writer.addAttribute( "name", "clean" );
        writer.addAttribute( "description", "Clean the output directory" );

        writer.startElement( "delete" );
        writer.addAttribute( "dir", "${maven.build.directory}" );
        writer.endElement(); // delete

        writer.endElement(); // target
    }

    private void writeCompileTarget( XMLWriter writer, List compileSourceRoots )
    {
        writer.startElement( "target" );
        writer.addAttribute( "name", "compile" );
        writer.addAttribute( "depends", "get-deps" );
        writer.addAttribute( "description", "Compile the code" );

        writeCompileTasks( writer, project.getBasedir(), "${maven.build.output}", compileSourceRoots,
                           project.getBuild().getResources(), null );

        writer.endElement(); // target
    }

    private static void writeCompileTasks( XMLWriter writer, File basedir, String outputDirectory,
                                           List compileSourceRoots, List resources, String additionalClassesDirectory )
    {
        writer.startElement( "mkdir" );
        writer.addAttribute( "dir", outputDirectory );
        writer.endElement(); // mkdir

        if ( !compileSourceRoots.isEmpty() )
        {
            writer.startElement( "javac" );
            writer.addAttribute( "destdir", outputDirectory );
            writer.addAttribute( "excludes", "**/package.html" );
            writer.addAttribute( "debug", "true" ); // TODO: use compiler setting
            writer.addAttribute( "deprecation", "true" ); // TODO: use compiler setting
            writer.addAttribute( "optimize", "false" ); // TODO: use compiler setting

            for ( Iterator i = compileSourceRoots.iterator(); i.hasNext(); )
            {
                String srcDir = (String) i.next();

                writer.startElement( "src" );
                writer.startElement( "pathelement" );
                writer.addAttribute( "location", toRelative( basedir, srcDir ) );
                writer.endElement(); // pathelement
                writer.endElement(); // src
            }

            if ( additionalClassesDirectory == null )
            {
                writer.startElement( "classpath" );
                writer.addAttribute( "refid", "build.classpath" );
                writer.endElement(); // classpath
            }
            else
            {
                writer.startElement( "classpath" );
                writer.startElement( "path" );
                writer.addAttribute( "refid", "build.classpath" );
                writer.endElement(); // path
                writer.startElement( "pathelement" );
                writer.addAttribute( "location", additionalClassesDirectory );
                writer.endElement(); // pathelement
                writer.endElement(); // classpath

            }

            writer.endElement(); // javac
        }

        for ( Iterator i = resources.iterator(); i.hasNext(); )
        {
            Resource resource = (Resource) i.next();

            if ( new File( resource.getDirectory() ).exists() )
            {
                String outputDir = outputDirectory;
                if ( resource.getTargetPath() != null && resource.getTargetPath().length() > 0 )
                {
                    outputDir = outputDir + "/" + resource.getTargetPath();

                    writer.startElement( "mkdir" );
                    writer.addAttribute( "dir", outputDir );
                    writer.endElement(); // mkdir
                }

                writer.startElement( "copy" );
                writer.addAttribute( "todir", outputDir );

                writer.startElement( "fileset" );
                writer.addAttribute( "dir", toRelative( basedir, resource.getDirectory() ) );

                writeIncludesExcludes( writer, resource.getIncludes(), resource.getExcludes() );

                writer.endElement(); // fileset

                writer.endElement(); // copy
            }
        }
    }

    private static List removeEmptyCompileSourceRoots( List compileSourceRoots )
    {
        List newCompileSourceRootsList = new ArrayList();
        if ( compileSourceRoots != null )
        {
            // copy as I may be modifying it
            for ( Iterator i = compileSourceRoots.iterator(); i.hasNext(); )
            {
                String srcDir = (String) i.next();
                if ( new File( srcDir ).exists() )
                {
                    newCompileSourceRootsList.add( srcDir );
                }
            }
        }
        return newCompileSourceRootsList;
    }

    private static void writeIncludesExcludes( XMLWriter writer, List includes, List excludes )
    {
        for ( Iterator i = includes.iterator(); i.hasNext(); )
        {
            String include = (String) i.next();
            writer.startElement( "include" );
            writer.addAttribute( "name", include );
            writer.endElement(); // include
        }
        for ( Iterator i = excludes.iterator(); i.hasNext(); )
        {
            String exclude = (String) i.next();
            writer.startElement( "exclude" );
            writer.addAttribute( "name", exclude );
            writer.endElement(); // exclude
        }
    }

    private void writeGetDepsTarget( XMLWriter writer )
    {
        writer.startElement( "target" );
        writer.addAttribute( "name", "test-offline" );

        writer.startElement( "condition" );
        writer.addAttribute( "property", "maven.mode.offline" );
        writer.startElement( "equals" );
        writer.addAttribute( "arg1", "${build.sysclasspath}" );
        writer.addAttribute( "arg2", "only" );
        writer.endElement(); // equals
        writer.endElement(); // condition

        writer.endElement(); // target
        writer.startElement( "target" );
        writer.addAttribute( "name", "get-deps" );
        writer.addAttribute( "depends", "test-offline" );
        writer.addAttribute( "description", "Download all dependencies" );
        writer.addAttribute( "unless", "maven.mode.offline" ); // TODO: check, and differs from m1

        writer.startElement( "mkdir" );
        writer.addAttribute( "dir", "${maven.repo.local}" );
        writer.endElement(); // mkdir

        // TODO: proxy - probably better to use wagon!

        for ( Iterator i = project.getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            // TODO: should the artifacthandler be used instead?
            String path = toRelative( localRepository, artifact.getFile().getPath() );

            for ( Iterator j = project.getRepositories().iterator(); j.hasNext(); )
            {
                Repository repository = (Repository) j.next();

                writer.startElement( "get" );
                writer.addAttribute( "src", repository.getUrl() + "/" + path );
                writer.addAttribute( "dest", "${maven.repo.local}/" + path );
                writer.addAttribute( "usetimestamp", "true" );
                writer.addAttribute( "ignoreerrors", "true" );
                writer.endElement(); // get
            }
        }

        writer.endElement(); // target
    }

    private void writeBuildPathDefinition( XMLWriter writer )
    {
        writer.startElement( "path" );
        writer.addAttribute( "id", "build.classpath" );
        writer.startElement( "fileset" );
        writer.addAttribute( "dir", "${maven.repo.local}" );
        for ( Iterator i = project.getArtifacts().iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();
            writer.startElement( "include" );
            writer.addAttribute( "name", toRelative( localRepository, artifact.getFile().getPath() ) );
            writer.endElement(); // include
        }
        writer.endElement(); // fileset
        writer.endElement(); // path
    }

    private void writeProperties( XMLWriter writer )
    {
        // TODO: optional in m1
        // TODO: USD properties
        writer.startElement( "property" );
        writer.addAttribute( "file", "${user.home}/.m2/maven.properties" );
        writer.endElement(); // property

        writer.startElement( "property" );
        writer.addAttribute( "name", "maven.build.output" );
        writer.addAttribute( "value", toRelative( project.getBasedir(), project.getBuild().getOutputDirectory() ) );
        writer.endElement(); // property

        writer.startElement( "property" );
        writer.addAttribute( "name", "maven.build.directory" );
        writer.addAttribute( "value", toRelative( project.getBasedir(), project.getBuild().getDirectory() ) );
        writer.endElement(); // property

        writer.startElement( "property" );
        writer.addAttribute( "name", "maven.build.final.name" );
        writer.addAttribute( "value", project.getBuild().getFinalName() );
        writer.endElement(); // property

        // TODO: property?
        writer.startElement( "property" );
        writer.addAttribute( "name", "maven.test.reports" );
        writer.addAttribute( "value", "${maven.build.directory}/test-reports" );
        writer.endElement(); // property

        writer.startElement( "property" );
        writer.addAttribute( "name", "maven.test.output" );
        writer.addAttribute( "value", toRelative( project.getBasedir(), project.getBuild().getTestOutputDirectory() ) );
        writer.endElement(); // property

        writer.startElement( "property" );
        writer.addAttribute( "name", "maven.repo.local" );
        writer.addAttribute( "value", "${user.home}/.m2/repository" );
        writer.endElement(); // property

/* TODO: offline setting
        writer.startElement( "property" );
        writer.addAttribute( "name", "maven.mode.offline" );
        writer.addAttribute( "value", project.getBuild().getOutput() );
        writer.endElement(); // property
*/
    }

    private void close( Writer closeable )
    {
        if ( closeable == null )
        {
            return;
        }

        try
        {
            closeable.close();
        }
        catch ( Exception e )
        {
            // ignore
            // TODO: warn
        }
    }

    // TODO: move to plexus-utils or use something appropriate from there (eclipse plugin too)
    private static String toRelative( File basedir, String absolutePath )
    {
        String relative;

        absolutePath = absolutePath.replace( '\\', '/' );
        String basedirPath = basedir.getAbsolutePath().replace( '\\', '/' );

        if ( absolutePath.startsWith( basedirPath ) )
        {
            relative = absolutePath.substring( basedirPath.length() + 1 );
        }
        else
        {
            relative = absolutePath;
        }

        return relative;
    }

}
