package org.apache.maven.project;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.MavenConstants;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.MavenMetadataSource;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.inheritance.ModelInheritanceAssembler;
import org.apache.maven.project.injection.ModelDefaultsInjector;
import org.apache.maven.project.interpolation.ModelInterpolator;
import org.apache.maven.project.path.PathTranslator;
import org.apache.maven.project.validation.ModelValidationResult;
import org.apache.maven.project.validation.ModelValidator;
import org.apache.maven.repository.RepositoryUtils;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultMavenProjectBuilder
    extends AbstractLogEnabled
    implements MavenProjectBuilder, Initializable
{
    private ArtifactResolver artifactResolver;

    private ArtifactFactory artifactFactory;

    private ModelInheritanceAssembler modelInheritanceAssembler;

    private ModelValidator validator;

    private MavenXpp3Reader modelReader;

    private PathTranslator pathTranslator;

    private ModelDefaultsInjector modelDefaultsInjector;

    private ModelInterpolator modelInterpolator;

    public void initialize()
        throws Exception
    {
        modelReader = new MavenXpp3Reader();
    }

    // ----------------------------------------------------------------------
    // MavenProjectBuilder Implementation
    // ----------------------------------------------------------------------

    public MavenProject build( File projectDescriptor, ArtifactRepository localRepository )
        throws ProjectBuildingException
    {
        return build( projectDescriptor, localRepository, false );
    }

    public MavenProject build( File projectDescriptor, ArtifactRepository localRepository, boolean resolveDependencies )
        throws ProjectBuildingException
    {
        try
        {
            Model superModel = getSuperModel();

            LinkedList lineage = new LinkedList();

            Set aggregatedRemoteWagonRepositories = RepositoryUtils.mavenToWagon( superModel.getRepositories() );
            MavenProject project = assembleLineage( projectDescriptor, localRepository, lineage,
                                                    aggregatedRemoteWagonRepositories );

            Model previous = superModel;

            for ( Iterator i = lineage.iterator(); i.hasNext(); )
            {
                Model current = ( (MavenProject) i.next() ).getModel();

                modelInheritanceAssembler.assembleModelInheritance( current, previous );

                previous = current;
            }

            Model model = modelInterpolator.interpolate( project.getModel() );

            // interpolation is before injection, because interpolation is off-limits in the injected variables
            modelDefaultsInjector.injectDefaults( model );

            MavenProject parentProject = project.getParent();

            project = new MavenProject( model );
            project.setFile( projectDescriptor );
            project.setParent( parentProject );
            project.setArtifacts( artifactFactory.createArtifacts( project.getDependencies(), localRepository ) );

            // ----------------------------------------------------------------------
            // Typically when the project builder is being used from maven proper
            // the transitive dependencies will not be resolved here because this
            // requires a lot of work when we may only be interested in running
            // something simple like 'm2 clean'. So the artifact collector is used
            // in the dependency resolution phase if it is required by any of the
            // goals being executed. But when used as a component in another piece
            // of code people may just want to build maven projects and have the
            // dependencies resolved for whatever reason: this is why we keep
            // this snippet of code here.
            // ----------------------------------------------------------------------

            if ( resolveDependencies )
            {
                Set repos = RepositoryUtils.mavenToWagon( project.getRepositories() );

                MavenMetadataSource sourceReader = new MavenMetadataSource( artifactResolver, this );

                ArtifactResolutionResult result = artifactResolver.resolveTransitively( project.getArtifacts(), repos,
                                                                                        localRepository, sourceReader );

                project.getArtifacts().addAll( result.getArtifacts().values() );
            }

            ModelValidationResult validationResult = validator.validate( project.getModel() );

            if ( validationResult.getMessageCount() > 0 )
            {
                throw new ProjectBuildingException( "Exception while building project: " + validationResult.toString() );
            }

            project.setFile( projectDescriptor );

            pathTranslator.alignToBaseDirectory( project.getModel(), projectDescriptor );

            return project;
        }
        catch ( Exception e )
        {
            throw new ProjectBuildingException( "Error building project from " + projectDescriptor, e );
        }
    }

    private MavenProject assembleLineage( File projectDescriptor, ArtifactRepository localRepository,
                                          LinkedList lineage, Set aggregatedRemoteWagonRepositories )
        throws Exception
    {
        Model model = readModel( projectDescriptor );

        MavenProject project = new MavenProject( model );

        lineage.addFirst( project );

        project.setFile( projectDescriptor );

        Parent parentModel = model.getParent();

        if ( parentModel != null )
        {
            if ( StringUtils.isEmpty( parentModel.getGroupId() ) )
            {
                throw new ProjectBuildingException( "Missing groupId element from parent element" );
            }
            else if ( StringUtils.isEmpty( parentModel.getArtifactId() ) )
            {
                throw new ProjectBuildingException( "Missing artifactId element from parent element" );
            }
            else if ( StringUtils.isEmpty( parentModel.getVersion() ) )
            {
                throw new ProjectBuildingException( "Missing version element from parent element" );
            }

            //!! (**)
            // ----------------------------------------------------------------------
            // Do we have the necessary information to actually find the parent
            // POMs here?? I don't think so ... Say only one remote repository is
            // specified and that is ibiblio then this model that we just read doesn't
            // have any repository information ... I think we might have to inherit
            // as we go in order to do this.
            // ----------------------------------------------------------------------

            aggregatedRemoteWagonRepositories.addAll( RepositoryUtils.mavenToWagon( model.getRepositories() ) );

            File parentPom = findParentModel( parentModel, aggregatedRemoteWagonRepositories, localRepository );

            MavenProject parent = assembleLineage( parentPom, localRepository, lineage,
                                                   aggregatedRemoteWagonRepositories );

            project.setParent( parent );
        }

        return project;
    }

    private Model readModel( File projectDescriptor )
        throws ProjectBuildingException
    {
        try
        {
            return readModel( new FileReader( projectDescriptor ) );
        }
        catch ( FileNotFoundException ex )
        {
            throw new ProjectBuildingException( "Error while building model.", ex );
        }
    }

    private Model readModel( Reader reader )
        throws ProjectBuildingException
    {
        try
        {
            return modelReader.read( reader );
        }
        catch ( Exception ex )
        {
            throw new ProjectBuildingException( "Error while building model.", ex );
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    private File findParentModel( Parent parent, Set remoteArtifactRepositories, ArtifactRepository localRepository )
        throws ProjectBuildingException
    {
        Artifact artifact = new DefaultArtifact( parent.getGroupId(), parent.getArtifactId(), parent.getVersion(),
                                                 "pom" );

        try
        {
            artifactResolver.resolve( artifact, remoteArtifactRepositories, localRepository );
        }
        catch ( ArtifactResolutionException e )
        {
            // @todo use parent.toString() if modello could generate it, or specify in a code segment
            throw new ProjectBuildingException( "Missing parent POM: " + parent.getGroupId() + ":" +
                                                parent.getArtifactId() + "-" + parent.getVersion(), e );
        }

        return artifact.getFile();
    }

    /**
     * Sort a list of projects.
     * <ul>
     * <li>collect all the vertices for the projects that we want to build.</li>
     * <li>iterate through the deps of each project and if that dep is within
     * the set of projects we want to build then add an edge, otherwise throw
     * the edge away because that dependency is not within the set of projects
     * we are trying to build. we assume a closed set.</li>
     * <li>do a topo sort on the graph that remains.</li>
     * </ul>
     */
    public List getSortedProjects( List projects )
        throws Exception
    {
        DAG dag = new DAG();

        Map projectMap = new HashMap();

        for ( Iterator i = projects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();

            String artifactId = project.getArtifactId();

            dag.addVertex( artifactId );

            projectMap.put( artifactId, project );
        }

        for ( Iterator i = projects.iterator(); i.hasNext(); )
        {
            MavenProject project = (MavenProject) i.next();

            String artifactId = project.getArtifactId();

            for ( Iterator j = project.getDependencies().iterator(); j.hasNext(); )
            {
                Dependency dependency = (Dependency) j.next();

                String dependencyArtifactId = dependency.getArtifactId();

                if ( dag.getVertex( dependencyArtifactId ) != null )
                {
                    dag.addEdge( artifactId, dependency.getArtifactId() );
                }
            }
        }

        List sortedProjects = new ArrayList();

        for ( Iterator i = TopologicalSorter.sort( dag ).iterator(); i.hasNext(); )
        {
            String artifactId = (String) i.next();

            sortedProjects.add( projectMap.get( artifactId ) );
        }

        return sortedProjects;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private Model getSuperModel()
        throws ProjectBuildingException
    {
        return readModel( new InputStreamReader( DefaultMavenProjectBuilder.class.getResourceAsStream(
            "pom-" + MavenConstants.MAVEN_MODEL_VERSION + ".xml" ) ) );
    }
}