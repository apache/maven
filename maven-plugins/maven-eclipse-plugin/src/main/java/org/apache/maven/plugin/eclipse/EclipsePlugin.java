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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

/**
 * A Maven2 plugin which integrates the use of Maven2 with Eclipse.
 *
 * @goal eclipse
 * @requiresDependencyResolution test
 * @execute phase="generate-sources"
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class EclipsePlugin
    extends AbstractMojo
{

    /**
     * Separator used for natures, builders, etc. (can't use space since conclasspath entries can contain spaces).
     */
    private static final String LIST_SEPARATOR = ","; //$NON-NLS-1$

    /**
     * The project whose project files to create.
     * @parameter expression="${project}"
     * @required
     */
    private MavenProject project;

    /**
     * The currently executed project (can be a reactor project).
     * @parameter expression="${executedProject}"
     */
    private MavenProject executedProject;

    /**
     * Local maven repository.
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * If the executed project is a reactor project, this will contains the full list of projects in the reactor.
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    private List reactorProjects;

    /**
     * Artifact resolver, needed to download source jars for inclusion in classpath.
     * @parameter expression="${component.org.apache.maven.artifact.resolver.ArtifactResolver}"
     * @required
     * @readonly
     * @todo waiting for the component tag
     */
    private ArtifactResolver artifactResolver;

    /**
     * Artifact factory, needed to download source jars for inclusion in classpath.
     * @parameter expression="${component.org.apache.maven.artifact.factory.ArtifactFactory}"
     * @required
     * @readonly
     * @todo waiting for the component tag
     */
    private ArtifactFactory artifactFactory;

    /**
     * Remote repositories which will be searched for source attachments.
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @required
     * @readonly
     */
    private List remoteArtifactRepositories;

    /**
     * List of eclipse project natures. By default the <code>org.eclipse.jdt.core.javanature</code> nature is added.
     * Configuration example:
     * <pre>
     *    &lt;projectnatures>
     *      &lt;java.lang.String>org.eclipse.jdt.core.javanature&lt;/java.lang.String>
     *      &lt;java.lang.String>org.eclipse.wst.common.modulecore.ModuleCoreNature&lt;/java.lang.String>
     *    &lt;/projectnatures>
     * </pre>
     * @parameter
     * @todo default-value="<java.lang.String>org.eclipse.jdt.core.javanature</java.lang.String>"
     */
    private List projectnatures;

    /**
     * List of eclipse build commands. By default the <code>org.eclipse.jdt.core.javabuilder</code> nature is added.
     * Configuration example:
     * <pre>
     *    &lt;buildcommands>
     *      &lt;java.lang.String>org.eclipse.wst.common.modulecore.ComponentStructuralBuilder&lt;/java.lang.String>
     *      &lt;java.lang.String>org.eclipse.jdt.core.javabuilder&lt;/java.lang.String>
     *      &lt;java.lang.String>org.eclipse.wst.common.modulecore.ComponentStructuralBuilderDependencyResolver&lt;/java.lang.String>
     *    &lt;/buildcommands>
     * </pre>
     * @parameter
     * @todo default-value="org.eclipse.jdt.core.javabuilder"
     */
    private List buildcommands;

    /**
     * List of container classpath entries. No classpath container is added by default.
     * Configuration example:
     * <pre>
     *    &lt;classpathContainers>
     *      &lt;java.lang.String>org.eclipse.jst.server.core.container/org.eclipse.jst.server.tomcat.runtimeTarget/Apache Tomcat v5.5&lt;/java.lang.String>
     *      &lt;java.lang.String>org.eclipse.jst.j2ee.internal.web.container/artifact&lt;/java.lang.String>
     *    &lt;/classpathContainers>
     * </pre>
     * @parameter
     * @todo default-value=empty list
     */
    private List classpathContainers;

    /**
     * Eclipse workspace directory.
     * @parameter expression="${eclipse.workspace}"
     */
    private File outputDir;

    /**
     * Setter for <code>project</code>. Needed for tests.
     * @param project The MavenProject to set.
     */
    protected void setProject( MavenProject project )
    {
        this.project = project;
    }

    /**
     * Setter for <code>localRepository</code>. Needed for tests.
     * @param localRepository The ArtifactRepository to set.
     */
    protected void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    /**
     * Setter for <code>artifactFactory</code>. Needed for tests.
     * @param artifactFactory The artifactFactory to set.
     */
    protected void setArtifactFactory( ArtifactFactory artifactFactory )
    {
        this.artifactFactory = artifactFactory;
    }

    /**
     * Setter for <code>artifactResolver</code>. Needed for tests.
     * @param artifactResolver The artifactResolver to set.
     */
    protected void setArtifactResolver( ArtifactResolver artifactResolver )
    {
        this.artifactResolver = artifactResolver;
    }

    /**
     * Setter for <code>remoteArtifactRepositories</code>. Needed for tests.
     * @param remoteArtifactRepositories The remoteArtifactRepositories to set.
     */
    protected void setRemoteArtifactRepositories( List remoteArtifactRepositories )
    {
        this.remoteArtifactRepositories = remoteArtifactRepositories;
    }

    /**
     * Setter for <code>buildcommands</code>. Needed for tests.
     * @param buildcommands The buildcommands to set.
     */
    protected void setBuildcommands( List buildcommands )
    {
        this.buildcommands = buildcommands;
    }

    /**
     * Setter for <code>classpathContainers</code>. Needed for tests.
     * @param classpathContainers The classpathContainers to set.
     */
    protected void setClasspathContainers( List classpathContainers )
    {
        this.classpathContainers = classpathContainers;
    }

    /**
     * Setter for <code>projectnatures</code>. Needed for tests.
     * @param projectnatures The projectnatures to set.
     */
    protected void setProjectnatures( List projectnatures )
    {
        this.projectnatures = projectnatures;
    }

    /**
     * Setter for <code>outputDir</code>. Needed for tests.
     * @param outputDir The outputDir to set.
     */
    public void setOutputDir( File outputDir )
    {
        this.outputDir = outputDir;
    }

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
        throws MojoExecutionException
    {

        assertNotEmpty( project.getGroupId(), "groupId" ); //$NON-NLS-1$
        assertNotEmpty( project.getArtifactId(), "artifactId" ); //$NON-NLS-1$

        // defaults
        // @todo how set List values in @default-value??
        if ( projectnatures == null )
        {
            projectnatures = new ArrayList();
            projectnatures.add( "org.eclipse.jdt.core.javanature" );
        }
        if ( buildcommands == null )
        {
            buildcommands = new ArrayList();
            buildcommands.add( "org.eclipse.jdt.core.javabuilder" );
        }
        if ( classpathContainers == null )
        {
            classpathContainers = new ArrayList();
        }
        // end defaults

        if ( project.getFile() == null || !project.getFile().exists() )
        {
            throw new MojoExecutionException( Messages.getString( "EclipsePlugin.missingpom" ) ); //$NON-NLS-1$
        }

        if ( "pom".equals( project.getPackaging() ) ) //$NON-NLS-1$
        {
            getLog().info( Messages.getString( "EclipsePlugin.pompackaging" ) ); //$NON-NLS-1$
            return;
        }

        if ( outputDir == null )
        {
            outputDir = project.getFile().getParentFile();
        }
        else if ( !outputDir.equals( project.getFile().getParentFile() ) )
        {
            if ( !outputDir.isDirectory() )
            {
                throw new MojoExecutionException( Messages.getString( "EclipsePlugin.notadir", outputDir ) ); //$NON-NLS-1$
            }

            outputDir = new File( outputDir, project.getArtifactId() );

            if ( !outputDir.isDirectory() && !outputDir.mkdir() )
            {
                throw new MojoExecutionException( Messages.getString( "EclipsePlugin.cantcreatedir", outputDir ) ); //$NON-NLS-1$
            }
        }

        if ( executedProject == null )
        {
            // backwards compat with alpha-2 only
            executedProject = project;
        }

        // ready to start
        write();

    }

    public void write()
        throws EclipsePluginException
    {

        File projectBaseDir = project.getFile().getParentFile();

        // build the list of referenced ARTIFACTS produced by reactor projects
        List reactorArtifacts = resolveReactorArtifacts();

        // build a list of UNIQUE source dirs (both src and resources) to be used in classpath and wtpmodules
        EclipseSourceDir[] sourceDirs = buildDirectoryList( project, outputDir );

        new EclipseClasspathWriter( getLog() ).write( projectBaseDir, outputDir, project, reactorArtifacts, sourceDirs,
                                                      classpathContainers, localRepository, artifactResolver,
                                                      artifactFactory, remoteArtifactRepositories );

        new EclipseProjectWriter( getLog() ).write( projectBaseDir, outputDir, project, executedProject,
                                                    reactorArtifacts, projectnatures, buildcommands );

        new EclipseSettingsWriter( getLog() ).write( projectBaseDir, outputDir, project, executedProject );

        new EclipseWtpmodulesWriter( getLog() ).write( outputDir, project, reactorArtifacts, sourceDirs,
                                                       localRepository );

        getLog().info( Messages.getString( "EclipsePlugin.wrote", //$NON-NLS-1$
                                           new Object[] { project.getArtifactId(), outputDir.getAbsolutePath() } ) );
    }

    private void assertNotEmpty( String string, String elementName )
        throws EclipsePluginException
    {
        if ( string == null )
        {
            throw new EclipsePluginException( Messages.getString( "EclipsePlugin.missingelement", elementName ) ); //$NON-NLS-1$
        }
    }

    private EclipseSourceDir[] buildDirectoryList( MavenProject project, File basedir )
    {
        File projectBaseDir = project.getFile().getParentFile();

        // avoid duplicated entries
        Set directories = new TreeSet();

        extractSourceDirs( directories, executedProject.getCompileSourceRoots(), basedir, projectBaseDir, false, null );

        extractResourceDirs( directories, project.getBuild().getResources(), project, basedir, projectBaseDir, false,
                             null );

        extractSourceDirs( directories, executedProject.getTestCompileSourceRoots(), basedir, projectBaseDir, true,
                           EclipseUtils.toRelativeAndFixSeparator( projectBaseDir, project.getBuild()
                               .getTestOutputDirectory(), false ) );

        extractResourceDirs( directories, project.getBuild().getTestResources(), project, basedir, projectBaseDir,
                             true, EclipseUtils.toRelativeAndFixSeparator( projectBaseDir, project.getBuild()
                                 .getTestOutputDirectory(), false ) );

        return (EclipseSourceDir[]) directories.toArray( new EclipseSourceDir[directories.size()] );
    }

    private void extractSourceDirs( Set directories, List sourceRoots, File basedir, File projectBaseDir, boolean test,
                                   String output )
    {
        for ( Iterator it = sourceRoots.iterator(); it.hasNext(); )
        {
            String sourceRoot = (String) it.next();

            if ( new File( sourceRoot ).isDirectory() )
            {
                sourceRoot = EclipseUtils.toRelativeAndFixSeparator( projectBaseDir, sourceRoot, !projectBaseDir
                    .equals( basedir ) );

                directories.add( new EclipseSourceDir( sourceRoot, output, test, null, null ) );
            }
        }
    }

    private void extractResourceDirs( Set directories, List resources, MavenProject project, File basedir,
                                     File projectBaseDir, boolean test, String output )
    {
        for ( Iterator it = resources.iterator(); it.hasNext(); )
        {

            Resource resource = (Resource) it.next();
            String includePattern = null;
            String excludePattern = null;

            if ( resource.getIncludes().size() != 0 )
            {
                // @todo includePattern = ?
                getLog().warn( Messages.getString( "EclipsePlugin.includenotsupported" ) ); //$NON-NLS-1$
            }

            if ( resource.getExcludes().size() != 0 )
            {
                // @todo excludePattern = ?
                getLog().warn( Messages.getString( "EclipsePlugin.excludenotsupported" ) ); //$NON-NLS-1$
            }

            //          Example of setting include/exclude patterns for future reference.
            //
            //          TODO: figure out how to merge if the same dir is specified twice
            //          with different in/exclude patterns. We can't write them now,
            //                      since only the the first one would be included.
            //
            //          if ( resource.getIncludes().size() != 0 )
            //          {
            //              writer.addAttribute(
            //                      "including", StringUtils.join( resource.getIncludes().iterator(), "|" )
            //                      );
            //          }
            //
            //          if ( resource.getExcludes().size() != 0 )
            //          {
            //              writer.addAttribute(
            //                      "excluding", StringUtils.join( resource.getExcludes().iterator(), "|" )
            //              );
            //          }

            if ( !StringUtils.isEmpty( resource.getTargetPath() ) )
            {
                output = resource.getTargetPath();
            }

            File resourceDirectory = new File( resource.getDirectory() );

            if ( !resourceDirectory.exists() || !resourceDirectory.isDirectory() )
            {
                continue;
            }

            String resourceDir = resource.getDirectory();
            resourceDir = EclipseUtils.toRelativeAndFixSeparator( projectBaseDir, resourceDir, !projectBaseDir
                .equals( basedir ) );

            if ( output != null )
            {
                output = EclipseUtils.toRelativeAndFixSeparator( projectBaseDir, output, false );
            }

            directories.add( new EclipseSourceDir( resourceDir, output, test, includePattern, excludePattern ) );
        }
    }

    /**
     * Returns the list of referenced artifacts produced by reactor projects.
     * @return List of Artifacts
     */
    private List resolveReactorArtifacts()
    {
        List referencedProjects = new ArrayList();

        Set artifacts = project.getArtifacts();

        for ( Iterator it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();

            MavenProject refProject = findReactorProject( reactorProjects, artifact );

            if ( refProject != null )
            {
                referencedProjects.add( artifact );
            }
        }

        return referencedProjects;
    }

    /**
     * Utility method that locates a project producing the given artifact.
     *
     * @param reactorProjects a list of projects to search.
     * @param artifact the artifact a project should produce.
     * @return null or the first project found producing the artifact.
     */
    private static MavenProject findReactorProject( List reactorProjects, Artifact artifact )
    {
        if ( reactorProjects == null )
        {
            return null; // we're a single project
        }

        for ( Iterator it = reactorProjects.iterator(); it.hasNext(); )
        {
            MavenProject project = (MavenProject) it.next();

            if ( project.getGroupId().equals( artifact.getGroupId() )
                && project.getArtifactId().equals( artifact.getArtifactId() )
                && project.getVersion().equals( artifact.getVersion() ) )
            {
                return project;
            }
        }

        return null;
    }

}
