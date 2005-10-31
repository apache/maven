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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:fgiust@users.sourceforge.net">Fabrizio Giustina</a>
 * @version $Id$
 */
public class EclipseUtils
{

    private EclipseUtils()
    {
        // don't instantiate
    }

    public static String toRelativeAndFixSeparator( File basedir, String absolutePath, boolean replaceSlashes )
    {
        String relative;

        if ( absolutePath.equals( basedir.getAbsolutePath() ) )
        {
            relative = ".";
        }
        else if ( absolutePath.startsWith( basedir.getAbsolutePath() ) )
        {
            relative = absolutePath.substring( basedir.getAbsolutePath().length() + 1 );
        }
        else
        {
            relative = absolutePath;
        }

        relative = StringUtils.replace( relative, "\\", "/" ); //$NON-NLS-1$ //$NON-NLS-2$

        if ( replaceSlashes )
        {
            relative = StringUtils.replace( relative, "/", "-" ); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return relative;
    }

    /**
     * @todo there should be a better way to do this
     */
    public static String getPluginSetting( MavenProject project, String artifactId, String optionName,
                                          String defaultValue )
    {
        for ( Iterator it = project.getModel().getBuild().getPlugins().iterator(); it.hasNext(); )
        {
            Plugin plugin = (Plugin) it.next();

            if ( plugin.getArtifactId().equals( artifactId ) )
            {
                Xpp3Dom o = (Xpp3Dom) plugin.getConfiguration();

                if ( o != null && o.getChild( optionName ) != null )
                {
                    return o.getChild( optionName ).getValue();
                }
            }
        }

        return defaultValue;
    }

    public static EclipseSourceDir[] buildDirectoryList( MavenProject project, File basedir, Log log,
                                                        String outputDirectory )
    {
        File projectBaseDir = project.getFile().getParentFile();

        // avoid duplicated entries
        Set directories = new TreeSet();

        EclipseUtils.extractSourceDirs( directories, project.getCompileSourceRoots(), basedir, projectBaseDir, false,
                                        null );

        EclipseUtils.extractResourceDirs( directories, project.getBuild().getResources(), project, basedir,
                                          projectBaseDir, false, null, log );

        // If using the standard output location, don't mix the test output into it.
        String testOutput = outputDirectory.equals( project.getBuild().getOutputDirectory() ) ? EclipseUtils
            .toRelativeAndFixSeparator( projectBaseDir, project.getBuild().getTestOutputDirectory(), false ) : null;

        EclipseUtils.extractSourceDirs( directories, project.getTestCompileSourceRoots(), basedir, projectBaseDir,
                                        true, testOutput );

        EclipseUtils.extractResourceDirs( directories, project.getBuild().getTestResources(), project, basedir,
                                          projectBaseDir, true, testOutput, log );

        return (EclipseSourceDir[]) directories.toArray( new EclipseSourceDir[directories.size()] );
    }

    private static void extractSourceDirs( Set directories, List sourceRoots, File basedir, File projectBaseDir,
                                          boolean test, String output )
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

    private static void extractResourceDirs( Set directories, List resources, MavenProject project, File basedir,
                                            File projectBaseDir, boolean test, String output, Log log )
    {
        for ( Iterator it = resources.iterator(); it.hasNext(); )
        {

            Resource resource = (Resource) it.next();
            String includePattern = null;
            String excludePattern = null;

            if ( resource.getIncludes().size() != 0 )
            {
                // @todo includePattern = ?
                log.warn( Messages.getString( "EclipsePlugin.includenotsupported" ) ); //$NON-NLS-1$
            }

            if ( resource.getExcludes().size() != 0 )
            {
                // @todo excludePattern = ?
                log.warn( Messages.getString( "EclipsePlugin.excludenotsupported" ) ); //$NON-NLS-1$
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
     * Utility method that locates a project producing the given artifact.
     *
     * @param reactorProjects a list of projects to search.
     * @param artifact the artifact a project should produce.
     * @return null or the first project found producing the artifact.
     */
    public static MavenProject findReactorProject( List reactorProjects, Artifact artifact )
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

    /**
     * Returns the list of referenced artifacts produced by reactor projects.
     * @return List of Artifacts
     */
    public static List resolveReactorArtifacts( MavenProject project, List reactorProjects )
    {
        List referencedProjects = new ArrayList();

        Set artifacts = project.getArtifacts();

        for ( Iterator it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();

            MavenProject refProject = EclipseUtils.findReactorProject( reactorProjects, artifact );

            if ( refProject != null )
            {
                referencedProjects.add( artifact );
            }
        }

        return referencedProjects;
    }

    public static void fixSystemScopeArtifacts( Collection artifacts, Collection dependencies )
    {
        // fix path for system dependencies.Artifact.getFile() returns a wrong path in mvn 2.0
        for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
        {
            Artifact artifact = (Artifact) iter.next();
            if ( Artifact.SCOPE_SYSTEM.equals( artifact.getScope() ) )
            {
                String groupid = artifact.getGroupId();
                String artifactId = artifact.getArtifactId();

                for ( Iterator depIt = dependencies.iterator(); depIt.hasNext(); )
                {
                    Dependency dep = (Dependency) depIt.next();
                    if ( Artifact.SCOPE_SYSTEM.equals( dep.getScope() ) && groupid.equals( dep.getGroupId() )
                        && artifactId.equals( dep.getArtifactId() ) )
                    {
                        artifact.setFile( new File( dep.getSystemPath() ) );
                        break;
                    }
                }
            }
        }
    }

}
