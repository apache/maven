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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * Writes eclipse .classpath file.
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:kenney@neonics.com">Kenney Westerhof</a>
 * @author <a href="mailto:fgiust@users.sourceforge.net">Fabrizio Giustina</a>
 * @version $Id$
 */
public class EclipseClasspathWriter
{

    private Log log;

    public EclipseClasspathWriter( Log log )
    {
        this.log = log;
    }

    /**
     * @param outputDirectory TODO
     * @todo the list of needed parameters is really long, maybe this should become a Plexus component
     */
    protected void write( File projectBaseDir, File basedir, MavenProject project, List referencedReactorArtifacts,
                         EclipseSourceDir[] sourceDirs, List classpathContainers, ArtifactRepository localRepository,
                         ArtifactResolver artifactResolver, ArtifactFactory artifactFactory,
                         List remoteArtifactRepositories, boolean downloadSources, String outputDirectory )
        throws MojoExecutionException
    {

        FileWriter w;

        try
        {
            w = new FileWriter( new File( basedir, ".classpath" ) ); //$NON-NLS-1$
        }
        catch ( IOException ex )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.erroropeningfile" ), ex ); //$NON-NLS-1$
        }

        XMLWriter writer = new PrettyPrintXMLWriter( w );

        writer.startElement( "classpath" ); //$NON-NLS-1$

        // ----------------------------------------------------------------------
        // Source roots and resources
        // ----------------------------------------------------------------------

        for ( int j = 0; j < sourceDirs.length; j++ )
        {
            EclipseSourceDir dir = sourceDirs[j];

            writer.startElement( "classpathentry" ); //$NON-NLS-1$

            writer.addAttribute( "kind", "src" ); //$NON-NLS-1$ //$NON-NLS-2$
            writer.addAttribute( "path", dir.getPath() ); //$NON-NLS-1$
            if ( dir.getOutput() != null )
            {
                writer.addAttribute( "output", dir.getOutput() ); //$NON-NLS-1$
            }

            writer.endElement();

        }

        // ----------------------------------------------------------------------
        // The default output
        // ----------------------------------------------------------------------

        writer.startElement( "classpathentry" ); //$NON-NLS-1$
        writer.addAttribute( "kind", "output" ); //$NON-NLS-1$ //$NON-NLS-2$
        writer.addAttribute( "path", EclipseUtils.toRelativeAndFixSeparator( projectBaseDir, //$NON-NLS-1$  
                                                                             outputDirectory, false ) );
        writer.endElement();

        // ----------------------------------------------------------------------
        // The JRE reference
        // ----------------------------------------------------------------------

        writer.startElement( "classpathentry" ); //$NON-NLS-1$
        writer.addAttribute( "kind", "con" ); //$NON-NLS-1$ //$NON-NLS-2$
        writer.addAttribute( "path", "org.eclipse.jdt.launching.JRE_CONTAINER" ); //$NON-NLS-1$ //$NON-NLS-2$
        writer.endElement();

        // ----------------------------------------------------------------------
        // The dependencies
        // ----------------------------------------------------------------------

        List artifacts = project.getTestArtifacts();

        // @todo direct optional artifacts are not included in the list returned by project.getTestArtifacts()
        // .classpath should include ANY direct dependency, and optional dependencies are required to compile
        Set depArtifacts = project.getDependencyArtifacts();
        for ( Iterator it = depArtifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            if ( artifact.isOptional() && !artifacts.contains( artifact ) )
            {
                try
                {
                    artifactResolver.resolve( artifact, remoteArtifactRepositories, localRepository );
                }
                catch ( ArtifactResolutionException e )
                {
                    log.error( "Unable to resolve optional artifact " + artifact.getId() );
                    continue;
                }
                catch ( ArtifactNotFoundException e )
                {
                    log.error( "Unable to resolve optional artifact " + artifact.getId() );
                    continue;
                }
                artifacts.add( artifact );
            }
        }

        EclipseUtils.fixSystemScopeArtifacts( artifacts, project.getDependencies() );

        for ( Iterator it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            if ( artifact.getArtifactHandler().isAddedToClasspath() )
            {
                addDependency( writer, artifact, referencedReactorArtifacts, localRepository, artifactResolver,
                               artifactFactory, remoteArtifactRepositories, downloadSources );
            }
        }

        // ----------------------------------------------------------------------
        // Additional container classpath entries
        // ----------------------------------------------------------------------

        for ( Iterator it = classpathContainers.iterator(); it.hasNext(); )
        {
            writer.startElement( "classpathentry" ); //$NON-NLS-1$
            writer.addAttribute( "kind", "con" ); //$NON-NLS-1$ //$NON-NLS-2$
            writer.addAttribute( "path", (String) it.next() ); //$NON-NLS-1$
            writer.endElement(); // name
        }

        writer.endElement();

        IOUtil.close( w );
    }

    private void addDependency( XMLWriter writer, Artifact artifact, List referencedReactorArtifacts,
                               ArtifactRepository localRepository, ArtifactResolver artifactResolver,
                               ArtifactFactory artifactFactory, List remoteArtifactRepositories, boolean downloadSources )
        throws MojoExecutionException
    {

        String path;
        String kind;
        String sourcepath = null;

        if ( referencedReactorArtifacts.contains( artifact ) )
        {
            path = "/" + artifact.getArtifactId(); //$NON-NLS-1$
            kind = "src"; //$NON-NLS-1$
        }
        else
        {
            File artifactPath = artifact.getFile();

            if ( artifactPath == null )
            {
                log.error( Messages.getString( "EclipsePlugin.artifactpathisnull", artifact.getId() ) ); //$NON-NLS-1$
                return;
            }

            if ( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
            {
                try
                {
                    path = StringUtils.replace( artifactPath.getCanonicalPath(), "\\", "/" );
                }
                catch ( IOException e )
                {
                    String message = Messages.getString( "EclipsePlugin.cantcanonicalize", artifactPath );

                    throw new MojoExecutionException( message, e );
                }

                log.info( Messages.getString( "EclipsePlugin.artifactissystemscoped", //$NON-NLS-1$
                                              new Object[] { artifact.getArtifactId(), path } ) );

                log.info( Messages.getString( "EclipseClasspathWriter.sourcesnotavailable", //$NON-NLS-1$
                                              artifact.getArtifactId() ) );

                kind = "lib"; //$NON-NLS-1$
            }
            else
            {
                File localRepositoryFile = new File( localRepository.getBasedir() );

                String fullPath = artifactPath.getPath();

                path = "M2_REPO/" //$NON-NLS-1$
                    + EclipseUtils.toRelativeAndFixSeparator( localRepositoryFile, fullPath, false );

                Artifact sourceArtifact = retrieveSourceArtifact( artifact, remoteArtifactRepositories,
                                                                  localRepository, artifactResolver, artifactFactory,
                                                                  downloadSources );

                if ( !sourceArtifact.isResolved() )
                {
                    if ( downloadSources )
                    {
                        log.info( Messages.getString( "EclipseClasspathWriter.sourcesnotavailable", //$NON-NLS-1$
                                                      sourceArtifact.getId() ) );
                    }
                    else
                    {
                        log.info( Messages.getString( "EclipseClasspathWriter.sourcesnotdownloaded", //$NON-NLS-1$
                                                      sourceArtifact.getId() ) );
                    }
                }
                else
                {
                    log.debug( Messages.getString( "EclipseClasspathWriter.sourcesavailable", //$NON-NLS-1$
                                                   new Object[] {
                                                       sourceArtifact.getId(),
                                                       sourceArtifact.getFile().getAbsolutePath() } ) );

                    sourcepath = "M2_REPO/" //$NON-NLS-1$
                        + EclipseUtils.toRelativeAndFixSeparator( localRepositoryFile, sourceArtifact.getFile()
                            .getAbsolutePath(), false );

                }

                kind = "var"; //$NON-NLS-1$
            }
        }

        writer.startElement( "classpathentry" ); //$NON-NLS-1$
        writer.addAttribute( "kind", kind ); //$NON-NLS-1$
        writer.addAttribute( "path", path ); //$NON-NLS-1$

        if ( sourcepath != null )
        {
            writer.addAttribute( "sourcepath", sourcepath ); //$NON-NLS-1$
        }

        writer.endElement();

    }

    private Artifact retrieveSourceArtifact( Artifact artifact, List remoteArtifactRepositories,
                                            ArtifactRepository localRepository, ArtifactResolver artifactResolver,
                                            ArtifactFactory artifactFactory, boolean downloadSources )
        throws MojoExecutionException
    {
        // source artifact: use the "sources" classifier added by the source plugin
        Artifact sourceArtifact = artifactFactory.createArtifactWithClassifier( artifact.getGroupId(), artifact
            .getArtifactId(), artifact.getVersion(), "java-source", "sources" ); //$NON-NLS-1$ //$NON-NLS-2$

        try
        {
            log.debug( Messages.getString( "EclipseClasspathWriter.lookingforsources", //$NON-NLS-1$
                                           sourceArtifact.getArtifactId() ) );

            artifactResolver.resolve( sourceArtifact, downloadSources ? remoteArtifactRepositories : new ArrayList(),
                                      localRepository );
        }
        catch ( ArtifactNotFoundException e )
        {
            // ignore, the jar has not been found
            if ( log.isDebugEnabled() )
            {
                String message = Messages.getString( "EclipseClasspathWriter.cantresolvesources", //$NON-NLS-1$
                                                     new Object[] { sourceArtifact.getId(), e.getMessage() } );

                log.debug( message, e );
            }
        }
        catch ( ArtifactResolutionException e )
        {
            String message = Messages.getString( "EclipseClasspathWriter.errorresolvingsources", //$NON-NLS-1$
                                                 new Object[] { sourceArtifact.getId(), e.getMessage() } );

            throw new MojoExecutionException( message, e );
        }

        return sourceArtifact;
    }
}
