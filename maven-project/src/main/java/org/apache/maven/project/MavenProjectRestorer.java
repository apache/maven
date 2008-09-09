package org.apache.maven.project;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.project.interpolation.ModelInterpolator;
import org.apache.maven.project.path.PathTranslator;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

final class MavenProjectRestorer
{

    private ModelInterpolator modelInterpolator;

    private PathTranslator pathTranslator;

    private Logger logger;

    MavenProjectRestorer( PathTranslator pathTranslator, ModelInterpolator modelInterpolator, Logger logger )
    {
        this.pathTranslator = pathTranslator;
        this.modelInterpolator = modelInterpolator;
        this.logger = logger;
    }

    Logger getLogger()
    {
        return logger;
    }

    void restoreDynamicState( MavenProject project, ProjectBuilderConfiguration config )
        throws ModelInterpolationException
    {
        if ( !project.isConcrete() )
        {
            return;
        }

        restoreBuildRoots( project, config );
        if ( project.getBuild() != null )
        {
            restoreModelBuildSection( project, config );
        }
        restoreDynamicProjectReferences( project, config );

        MavenProject executionProject = project.getExecutionProject();
        if ( executionProject != null && executionProject != project )
        {
            restoreDynamicState( executionProject, config );
        }

        project.setConcrete( false );
    }

    void calculateConcreteState( MavenProject project, ProjectBuilderConfiguration config )
        throws ModelInterpolationException
    {
        if ( project.isConcrete() )
        {
            return;
        }

        Build build = project.getBuild();
        if ( build != null )
        {
            initResourceMergeIds( build.getResources() );
            initResourceMergeIds( build.getTestResources() );
        }

        Model model = ModelUtils.cloneModel( project.getModel() );

        File basedir = project.getBasedir();

        model = modelInterpolator.interpolate( model, basedir, config, getLogger().isDebugEnabled() );

        List originalInterpolatedCompileSourceRoots =
            interpolateListOfStrings( project.getCompileSourceRoots(), model, project.getBasedir(), config );

        project.preserveCompileSourceRoots( originalInterpolatedCompileSourceRoots );

        project.setCompileSourceRoots( originalInterpolatedCompileSourceRoots == null
            ? null
            : translateListOfPaths( originalInterpolatedCompileSourceRoots, basedir ) );

        List originalInterpolatedTestCompileSourceRoots =
            interpolateListOfStrings( project.getTestCompileSourceRoots(), model, project.getBasedir(), config );

        project.preserveTestCompileSourceRoots( originalInterpolatedTestCompileSourceRoots );
        project.setTestCompileSourceRoots( originalInterpolatedTestCompileSourceRoots == null
            ? null
            : translateListOfPaths( originalInterpolatedTestCompileSourceRoots, basedir ) );

        List originalInterpolatedScriptSourceRoots =
            interpolateListOfStrings( project.getScriptSourceRoots(), model, project.getBasedir(), config );

        project.preserveScriptSourceRoots( originalInterpolatedScriptSourceRoots );
        project.setScriptSourceRoots( originalInterpolatedScriptSourceRoots == null
            ? null
            : translateListOfPaths( originalInterpolatedScriptSourceRoots, basedir ) );

        Model model2 = ModelUtils.cloneModel( model );

        pathTranslator.alignToBaseDirectory( model, basedir );

        project.preserveBuild( model2.getBuild() );
        project.setBuild( model.getBuild() );

        calculateConcreteProjectReferences( project, config );

        MavenProject executionProject = project.getExecutionProject();
        if ( executionProject != null && executionProject != project )
        {
            calculateConcreteState( executionProject, config );
        }

        project.setConcrete( true );
    }


    private void restoreDynamicProjectReferences( MavenProject project, ProjectBuilderConfiguration config )
        throws ModelInterpolationException
    {
        Map projectRefs = project.getProjectReferences();
        if ( projectRefs != null )
        {
            for ( Iterator it = projectRefs.values().iterator(); it.hasNext(); )
            {
                MavenProject projectRef = (MavenProject) it.next();
                restoreDynamicState( projectRef, config );
            }
        }
    }

    private void restoreBuildRoots( MavenProject project, ProjectBuilderConfiguration config )
        throws ModelInterpolationException
    {
        project.setCompileSourceRoots( restoreListOfStrings( project.getDynamicCompileSourceRoots(),
                                                             project.getOriginalInterpolatedCompileSourceRoots(),
                                                             project.getCompileSourceRoots(), project, config ) );

        project.setTestCompileSourceRoots( restoreListOfStrings( project.getDynamicTestCompileSourceRoots(),
                                                                 project.getOriginalInterpolatedTestCompileSourceRoots(),
                                                                 project.getTestCompileSourceRoots(), project,
                                                                 config ) );

        project.setScriptSourceRoots( restoreListOfStrings( project.getDynamicScriptSourceRoots(),
                                                            project.getOriginalInterpolatedScriptSourceRoots(),
                                                            project.getScriptSourceRoots(), project, config ) );

        project.clearRestorableRoots();
    }

    private void restoreModelBuildSection( MavenProject project, ProjectBuilderConfiguration config )
        throws ModelInterpolationException
    {
        Build changedBuild = project.getBuild();
        Build dynamicBuild = project.getDynamicBuild();
        Build originalInterpolatedBuild = project.getOriginalInterpolatedBuild();

        dynamicBuild.setResources( restoreResources( dynamicBuild.getResources(),
                                                     originalInterpolatedBuild.getResources(),
                                                     changedBuild.getResources(), project, config ) );

        dynamicBuild.setTestResources( restoreResources( dynamicBuild.getTestResources(),
                                                         originalInterpolatedBuild.getTestResources(),
                                                         changedBuild.getTestResources(), project, config ) );

        dynamicBuild.setFilters( restoreListOfStrings( dynamicBuild.getFilters(),
                                                       originalInterpolatedBuild.getFilters(),
                                                       changedBuild.getFilters(), project, config ) );

        dynamicBuild.setFinalName( restoreString( dynamicBuild.getFinalName(), originalInterpolatedBuild.getFinalName(),
                                                  changedBuild.getFinalName(), project, config ) );

        dynamicBuild.setDefaultGoal( restoreString( dynamicBuild.getDefaultGoal(),
                                                    originalInterpolatedBuild.getDefaultGoal(),
                                                    changedBuild.getDefaultGoal(), project, config ) );

        dynamicBuild.setSourceDirectory( restoreString( dynamicBuild.getSourceDirectory(),
                                                        originalInterpolatedBuild.getSourceDirectory(),
                                                        changedBuild.getSourceDirectory(), project, config ) );

        dynamicBuild.setTestSourceDirectory( restoreString( dynamicBuild.getTestSourceDirectory(),
                                                            originalInterpolatedBuild.getTestSourceDirectory(),
                                                            changedBuild.getTestSourceDirectory(), project, config ) );

        dynamicBuild.setScriptSourceDirectory( restoreString( dynamicBuild.getScriptSourceDirectory(),
                                                              originalInterpolatedBuild.getScriptSourceDirectory(),
                                                              changedBuild.getScriptSourceDirectory(), project,
                                                              config ) );

        dynamicBuild.setOutputDirectory( restoreString( dynamicBuild.getOutputDirectory(),
                                                        originalInterpolatedBuild.getOutputDirectory(),
                                                        changedBuild.getOutputDirectory(), project, config ) );

        dynamicBuild.setTestOutputDirectory( restoreString( dynamicBuild.getTestOutputDirectory(),
                                                            originalInterpolatedBuild.getTestOutputDirectory(),
                                                            changedBuild.getTestOutputDirectory(), project, config ) );

        dynamicBuild.setDirectory( restoreString( dynamicBuild.getDirectory(), originalInterpolatedBuild.getDirectory(),
                                                  changedBuild.getDirectory(), project, config ) );

        project.setBuild( dynamicBuild );

        project.clearRestorableBuild();
    }

    private List interpolateListOfStrings( List originalStrings, Model model, File projectDir,
                                           ProjectBuilderConfiguration config )
        throws ModelInterpolationException
    {
        if ( originalStrings == null )
        {
            return null;
        }

        List result = new ArrayList();

        for ( Iterator it = originalStrings.iterator(); it.hasNext(); )
        {
            String original = (String) it.next();
            String interpolated =
                modelInterpolator.interpolate( original, model, projectDir, config, getLogger().isDebugEnabled() );

            result.add( interpolated );
        }

        return result;
    }

    private String restoreString( String originalString, String originalInterpolatedString, String changedString,
                                  MavenProject project, ProjectBuilderConfiguration config )
        throws ModelInterpolationException
    {
        if ( originalString == null )
        {
            return changedString;
        }
        else if ( changedString == null )
        {
            return originalString;
        }

        Model model = project.getModel();

        String relativeChangedString;
        if ( project.getBasedir() != null )
        {
            relativeChangedString = pathTranslator.unalignFromBaseDirectory( changedString, project.getBasedir() );
        }
        else
        {
            relativeChangedString = changedString;
        }

        String interpolatedOriginal = modelInterpolator.interpolate( originalString, model, project.getBasedir(),
                                                                     config, getLogger().isDebugEnabled() );
        String interpolatedOriginal2 = modelInterpolator.interpolate( originalInterpolatedString, model,
                                                                      project.getBasedir(), config,
                                                                      getLogger().isDebugEnabled() );

        String interpolatedChanged = modelInterpolator.interpolate( changedString, model, project.getBasedir(), config,
                                                                    getLogger().isDebugEnabled() );
        String relativeInterpolatedChanged = modelInterpolator.interpolate( relativeChangedString, model,
                                                                            project.getBasedir(), config,
                                                                            getLogger().isDebugEnabled() );

        if ( interpolatedOriginal.equals( interpolatedChanged ) || interpolatedOriginal2.equals( interpolatedChanged ) )
        {
            return originalString;
        }
        else if ( interpolatedOriginal.equals( relativeInterpolatedChanged ) ||
            interpolatedOriginal2.equals( relativeInterpolatedChanged ) )
        {
            return originalString;
        }

        return relativeChangedString;
    }

    private List restoreListOfStrings( List originalStrings, List originalInterpolatedStrings, List changedStrings,
                                       MavenProject project, ProjectBuilderConfiguration config )
        throws ModelInterpolationException
    {
        if ( originalStrings == null )
        {
            return changedStrings;
        }
        else if ( changedStrings == null )
        {
            return originalStrings;
        }

        List result = new ArrayList();

        Map orig = new HashMap();
        for ( int idx = 0; idx < originalStrings.size(); idx++ )
        {
            String[] permutations = new String[2];

            permutations[0] = (String) originalInterpolatedStrings.get( idx );
            permutations[1] = (String) originalStrings.get( idx );

            orig.put( permutations[0], permutations );
        }

        for ( Iterator it = changedStrings.iterator(); it.hasNext(); )
        {
            String changedString = (String) it.next();
            String relativeChangedString;
            if ( project.getBasedir() != null )
            {
                relativeChangedString = pathTranslator.unalignFromBaseDirectory( changedString, project.getBasedir() );
            }
            else
            {
                relativeChangedString = changedString;
            }

            String interpolated = modelInterpolator.interpolate( changedString, project.getModel(),
                                                                 project.getBasedir(), config,
                                                                 getLogger().isDebugEnabled() );

            String relativeInterpolated = modelInterpolator.interpolate( relativeChangedString, project.getModel(),
                                                                         project.getBasedir(), config,
                                                                         getLogger().isDebugEnabled() );

            String[] original = (String[]) orig.get( interpolated );
            if ( original == null )
            {
                original = (String[]) orig.get( relativeInterpolated );
            }

            if ( original == null )
            {
                result.add( relativeChangedString );
            }
            else
            {
                result.add( original[1] );
            }
        }

        return result;
    }

    // TODO: Convert this to use the mergeId on each resource...
    private List restoreResources( List<Resource> originalResources, List<Resource> originalInterpolatedResources,
                                   List<Resource> changedResources, MavenProject project,
                                   ProjectBuilderConfiguration config )
        throws ModelInterpolationException
    {
        if ( originalResources == null || changedResources == null )
        {
            return originalResources;
        }

        List<Resource> result = new ArrayList<Resource>();

        Map<String, Resource[]> originalResourcesByMergeId = new HashMap<String, Resource[]>();
        for ( int idx = 0; idx < originalResources.size(); idx++ )
        {
            Resource[] permutations = new Resource[2];

            permutations[0] = originalInterpolatedResources.get( idx );
            permutations[1] = originalResources.get( idx );

            originalResourcesByMergeId.put( permutations[0].getMergeId(), permutations );
        }

        for ( Resource resource : changedResources )
        {
            String mergeId = resource.getMergeId();
            if ( mergeId == null || !originalResourcesByMergeId.containsKey( mergeId ) )
            {
                result.add( resource );
            }
            else
            {
                Resource originalInterpolatedResource = originalResourcesByMergeId.get( mergeId )[0];
                Resource originalResource = originalResourcesByMergeId.get( mergeId )[1];

                String dir = modelInterpolator.interpolate( resource.getDirectory(), project.getModel(),
                                                            project.getBasedir(), config,
                                                            getLogger().isDebugEnabled() );
                String oDir = originalInterpolatedResource.getDirectory();

                if ( !dir.equals( oDir ) )
                {
                    originalResource.setDirectory(
                        pathTranslator.unalignFromBaseDirectory( dir, project.getBasedir() ) );
                }

                if ( resource.getTargetPath() != null )
                {
                    String target = modelInterpolator.interpolate( resource.getTargetPath(), project.getModel(),
                                                                   project.getBasedir(), config,
                                                                   getLogger().isDebugEnabled() );

                    String oTarget = originalInterpolatedResource.getTargetPath();

                    if ( !target.equals( oTarget ) )
                    {
                        originalResource.setTargetPath(
                            pathTranslator.unalignFromBaseDirectory( target, project.getBasedir() ) );
                    }
                }

                originalResource.setFiltering( resource.isFiltering() );

                originalResource.setExcludes( collectRestoredListOfPatterns( resource.getExcludes(),
                                                                             originalResource.getExcludes(),
                                                                             originalInterpolatedResource.getExcludes() ) );

                originalResource.setIncludes( collectRestoredListOfPatterns( resource.getIncludes(),
                                                                             originalResource.getIncludes(),
                                                                             originalInterpolatedResource.getIncludes() ) );

                result.add( originalResource );
            }
        }

        return result;
    }

    private List<String> collectRestoredListOfPatterns( List<String> patterns, List<String> originalPatterns,
                                                        List<String> originalInterpolatedPatterns )
    {
        LinkedHashSet<String> collectedPatterns = new LinkedHashSet<String>();

        collectedPatterns.addAll( originalPatterns );

        for ( String pattern : patterns )
        {
            if ( !originalInterpolatedPatterns.contains( pattern ) )
            {
                collectedPatterns.add( pattern );
            }
        }

        return (List<String>) ( collectedPatterns.isEmpty()
            ? Collections.emptyList()
            : new ArrayList<String>( collectedPatterns ) );
    }


    private void initResourceMergeIds( List<Resource> resources )
    {
        if ( resources != null )
        {
            for ( Resource resource : resources )
            {
                resource.initMergeId();
            }
        }
    }

    private void calculateConcreteProjectReferences( MavenProject project, ProjectBuilderConfiguration config )
        throws ModelInterpolationException
    {
        Map projectRefs = project.getProjectReferences();

        if ( projectRefs != null )
        {
            for ( Iterator it = projectRefs.values().iterator(); it.hasNext(); )
            {
                MavenProject reference = (MavenProject) it.next();
                calculateConcreteState( reference, config );
            }
        }
    }

    private List translateListOfPaths( List paths, File basedir )
    {
        if ( paths == null )
        {
            return null;
        }
        else if ( basedir == null )
        {
            return paths;
        }

        List result = new ArrayList( paths.size() );
        for ( Iterator it = paths.iterator(); it.hasNext(); )
        {
            String path = (String) it.next();

            String aligned = pathTranslator.alignToBaseDirectory( path, basedir );

            result.add( aligned );
        }

        return result;
    }


}
