package org.apache.maven.plugin.eclipse;

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
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Writes eclipse .project file.
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:kenney@neonics.com">Kenney Westerhof</a>
 * @author <a href="mailto:fgiust@users.sourceforge.net">Fabrizio Giustina</a>
 * @version $Id$
 */
public class EclipseProjectWriter
{

    private Log log;

    public EclipseProjectWriter( Log log )
    {
        this.log = log;
    }

    protected void write( File projectBaseDir, File basedir, MavenProject project, MavenProject executedProject,
                          List reactorArtifacts, List projectnatures, List buildCommands )
        throws MojoExecutionException
    {
        FileWriter w;

        try
        {
            w = new FileWriter( new File( basedir, ".project" ) ); //$NON-NLS-1$
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.erroropeningfile" ),
                                              ex ); //$NON-NLS-1$
        }

        XMLWriter writer = new PrettyPrintXMLWriter( w );

        writer.startElement( "projectDescription" ); //$NON-NLS-1$

        writer.startElement( "name" ); //$NON-NLS-1$
        writer.writeText( project.getArtifactId() );
        writer.endElement();

        // TODO: this entire element might be dropped if the comment is null.
        // but as the maven1 eclipse plugin does it, it's better to be safe than sorry
        // A eclipse developer might want to look at this.
        writer.startElement( "comment" ); //$NON-NLS-1$

        if ( project.getDescription() != null )
        {
            writer.writeText( project.getDescription() );
        }

        writer.endElement();

        writer.startElement( "projects" ); //$NON-NLS-1$

        for ( Iterator it = reactorArtifacts.iterator(); it.hasNext(); )
        {
            writer.startElement( "project" ); //$NON-NLS-1$
            writer.writeText( ( (Artifact) it.next() ).getArtifactId() );
            writer.endElement();
        }

        writer.endElement(); // projects

        writer.startElement( "buildSpec" ); //$NON-NLS-1$

        for ( Iterator it = buildCommands.iterator(); it.hasNext(); )
        {
            writer.startElement( "buildCommand" ); //$NON-NLS-1$
            writer.startElement( "name" ); //$NON-NLS-1$
            writer.writeText( (String) it.next() );
            writer.endElement(); // name
            writer.startElement( "arguments" ); //$NON-NLS-1$
            writer.endElement(); // arguments
            writer.endElement(); // buildCommand
        }

        writer.endElement(); // buildSpec

        writer.startElement( "natures" ); //$NON-NLS-1$

        for ( Iterator it = projectnatures.iterator(); it.hasNext(); )
        {
            writer.startElement( "nature" ); //$NON-NLS-1$
            writer.writeText( (String) it.next() );
            writer.endElement(); // name
        }

        writer.endElement(); // natures

        if ( !projectBaseDir.equals( basedir ) )
        {
            writer.startElement( "linkedResources" ); //$NON-NLS-1$

            addFileLink( writer, projectBaseDir, basedir, project.getFile() );

            addSourceLinks( writer, projectBaseDir, basedir, executedProject.getCompileSourceRoots() );
            addResourceLinks( writer, projectBaseDir, basedir, executedProject.getBuild().getResources() );

            addSourceLinks( writer, projectBaseDir, basedir, executedProject.getTestCompileSourceRoots() );
            addResourceLinks( writer, projectBaseDir, basedir, executedProject.getBuild().getTestResources() );

            writer.endElement(); // linedResources
        }

        writer.endElement(); // projectDescription

        IOUtil.close( w );
    }

    private void addFileLink( XMLWriter writer, File projectBaseDir, File basedir, File file )
    {
        if ( file.isFile() )
        {
            writer.startElement( "link" ); //$NON-NLS-1$

            writer.startElement( "name" ); //$NON-NLS-1$
            writer.writeText( EclipseUtils.toRelativeAndFixSeparator( projectBaseDir, file.toString(), true ) );
            writer.endElement(); // name

            writer.startElement( "type" ); //$NON-NLS-1$
            writer.writeText( "1" ); //$NON-NLS-1$
            writer.endElement(); // type

            writer.startElement( "location" ); //$NON-NLS-1$
            writer.writeText( file.toString().replaceAll( "\\\\", "/" ) ); //$NON-NLS-1$ //$NON-NLS-2$
            writer.endElement(); // location

            writer.endElement(); // link
        }
        else
        {
            log.warn( Messages.getString( "EclipseProjectWriter.notafile", file ) ); //$NON-NLS-1$
        }
    }

    private void addSourceLinks( XMLWriter writer, File projectBaseDir, File basedir, List sourceRoots )
    {
        for ( Iterator it = sourceRoots.iterator(); it.hasNext(); )
        {
            String sourceRoot = (String) it.next();

            if ( new File( sourceRoot ).isDirectory() )
            {
                writer.startElement( "link" ); //$NON-NLS-1$

                writer.startElement( "name" ); //$NON-NLS-1$
                writer.writeText( EclipseUtils.toRelativeAndFixSeparator( projectBaseDir, sourceRoot, true ) );
                writer.endElement(); // name

                writer.startElement( "type" ); //$NON-NLS-1$
                writer.writeText( "2" ); //$NON-NLS-1$
                writer.endElement(); // type

                writer.startElement( "location" ); //$NON-NLS-1$
                writer.writeText( sourceRoot.replaceAll( "\\\\", "/" ) ); //$NON-NLS-1$ //$NON-NLS-2$
                writer.endElement(); // location

                writer.endElement(); // link
            }
        }
    }

    private void addResourceLinks( XMLWriter writer, File projectBaseDir, File basedir, List sourceRoots )
    {
        for ( Iterator it = sourceRoots.iterator(); it.hasNext(); )
        {
            String resourceDir = ( (Resource) it.next() ).getDirectory();

            if ( new File( resourceDir ).isDirectory() )
            {
                writer.startElement( "link" ); //$NON-NLS-1$

                writer.startElement( "name" ); //$NON-NLS-1$
                writer.writeText( EclipseUtils.toRelativeAndFixSeparator( projectBaseDir, resourceDir, true ) );
                writer.endElement(); // name

                writer.startElement( "type" ); //$NON-NLS-1$
                writer.writeText( "2" ); //$NON-NLS-1$
                writer.endElement(); // type

                writer.startElement( "location" ); //$NON-NLS-1$
                writer.writeText( resourceDir.replaceAll( "\\\\", "/" ) ); //$NON-NLS-1$ //$NON-NLS-2$
                writer.endElement(); // location

                writer.endElement(); // link
            }
        }
    }

}
