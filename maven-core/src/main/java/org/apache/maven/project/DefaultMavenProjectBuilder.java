package org.apache.maven.project;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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
import org.apache.maven.artifact.MavenMetadataSource;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.inheritance.ModelInheritanceAssembler;
import org.apache.maven.project.path.PathTranslator;
import org.apache.maven.project.validation.ModelValidationResult;
import org.apache.maven.project.validation.ModelValidator;
import org.apache.maven.repository.RepositoryUtils;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.util.CollectionUtils;
import org.codehaus.plexus.util.PropertyUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.dag.DAG;
import org.codehaus.plexus.util.dag.TopologicalSorter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Collection;
import java.util.Collections;

public class DefaultMavenProjectBuilder
    extends AbstractLogEnabled
    implements MavenProjectBuilder, Initializable
{
    public static final String MAVEN_PROPERTIES = "maven.properties";

    private ArtifactResolver artifactResolver;

    private ArtifactFactory artifactFactory;

    private ModelInheritanceAssembler modelInheritanceAssembler;

    private ModelValidator validator;

    private MavenXpp3Writer modelWriter;

    private MavenXpp3Reader modelReader;

    private PathTranslator pathTranslator;

    private Model superModel;

    public void initialize()
        throws Exception
    {
        modelWriter = new MavenXpp3Writer();

        modelReader = new MavenXpp3Reader();
    }

    public MavenProject build( File projectDescriptor, ArtifactRepository localRepository )
        throws ProjectBuildingException
    {
        return build( projectDescriptor, localRepository, false );
    }

    /** @todo can we move the super model reading to the initialize method? what about the user/site? Is it reused?
     *  @todo we should be passing in some more configuration here so that maven.home.local can be used for user properties. Then, the new stuff should be unit tested.
     */
    public MavenProject build( File projectDescriptor, ArtifactRepository localRepository, boolean resolveDependencies )
        throws ProjectBuildingException
    {
        try
        {
            superModel = modelReader.read( new InputStreamReader( DefaultMavenProjectBuilder.class.getResourceAsStream( "pom.xml" ) ) );

            LinkedList lineage = new LinkedList();

            MavenProject project = assembleLineage( projectDescriptor,
                                                    localRepository, 
                                                    lineage, 
                                                    superModel.getRepositories() );

            Model previous = superModel;

            for ( Iterator i = lineage.iterator(); i.hasNext(); )
            {
                Model current = ( (MavenProject) i.next() ).getModel();
                modelInheritanceAssembler.assembleModelInheritance( current, previous );
                previous = current;
            }

            // TODO: use maven.home.local instead of user.home/.m2
            File userModelFile = new File( System.getProperty( "user.home" ) + "/.m2", "pom.xml" );
            if ( userModelFile.exists() )
            {
                Model userModel = modelReader.read( new FileReader( userModelFile ) );
                modelInheritanceAssembler.assembleModelInheritance( userModel, previous );

                if ( userModel.getParent() != null )
                {
                    throw new ProjectBuildingException( "Inheritence not supported in the user override POM" );
                }

                MavenProject parent = project;
                project = new MavenProject( userModel );
                project.setFile( parent.getFile() );
                project.setParent( parent );

                // Note that we don't currently support maven.properties here: this might be a better place to do
                // the overrides though, if it is kept. If so, would need to process this regardless of the existence
                // of pom.xml
                project.setProperties( parent.getProperties() );
            }

            project.setArtifacts( artifactFactory.createArtifacts( project.getDependencies(), localRepository ) );

            setupMavenFinalName( project );

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

                MavenMetadataSource sourceReader = new MavenMetadataSource( repos, localRepository, artifactResolver );

                ArtifactResolutionResult result = artifactResolver.resolveTransitively( project.getArtifacts(),
                                                                                        repos,
                                                                                        localRepository,
                                                                                        sourceReader );

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

    /** @todo loading of project properties could be handled much more effeciently as they are never loaded from
        the repository. However, I believe they should be removedC anyway and use the POM -- Brett. */
    private MavenProject assembleLineage( File projectDescriptor, 
                                          ArtifactRepository localRepository, 
                                          LinkedList lineage,
                                          List remoteRepositories )
        throws Exception
    {
        Map properties = createProjectProperties( projectDescriptor.getParentFile() );

        Model model = readModel( projectDescriptor );

        MavenProject project = new MavenProject( model );

        lineage.addFirst( project );

        project.setFile( projectDescriptor );

        Parent parentModel = model.getParent();

        if ( parentModel != null )
        {
            if ( isEmpty( parentModel.getGroupId() ) )
            {
                throw new ProjectBuildingException( "Missing groupId element from parent element" );
            }
            else if ( isEmpty( parentModel.getArtifactId() ) )
            {
                throw new ProjectBuildingException( "Missing artifactId element from parent element" );
            }
            else if ( isEmpty( parentModel.getVersion() ) )
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

            remoteRepositories.addAll( model.getRepositories() );

            File parentPom = findParentModel( parentModel,
                                              RepositoryUtils.mavenToWagon( remoteRepositories ),
                                              localRepository );

            MavenProject parent = assembleLineage( parentPom, localRepository, lineage, remoteRepositories );

            project.setParent( parent );
        }

        project.setProperties( properties );

        return project;
    }

    private Model readModel( File projectDescriptor )
        throws Exception
    {
        Reader reader = null;

        try
        {
            reader = new FileReader( projectDescriptor );

            Model model = modelReader.read( reader );

            reader.close();

            return model;
        }
        finally
        {
            if ( reader != null )
            {
                try
                {
                    reader.close();
                }
                catch ( IOException e )
                {
                    // ignore
                }
            }
        }
    }

    private File findParentModel( Parent parent, Set remoteRepositories, ArtifactRepository localRepository )
        throws ProjectBuildingException
    {
        Artifact artifact = new DefaultArtifact( parent.getGroupId(), 
                                                 parent.getArtifactId(),
                                                 parent.getVersion(),
                                                 "pom" );

        try
        {
            artifactResolver.resolve( artifact, remoteRepositories, localRepository );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ProjectBuildingException( "Missing parent POM: ", e );

        }

        return artifact.getFile();
    }

    private void setupMavenFinalName( MavenProject project )
    {
        String mavenFinalName = project.getProperty( "maven.final.name" );

        if ( mavenFinalName == null || mavenFinalName.indexOf( "${" ) >= 0 )
        {
            project.setProperty( "maven.final.name", project.getArtifactId() + "-" + project.getVersion() );
        }
    }

    private Map createProjectProperties( File descriptorDirectory )
    {
        File f;

        Properties systemProperties = System.getProperties();

        f = new File( System.getProperty( "user.home" ), MAVEN_PROPERTIES );

        Properties mavenProperties = PropertyUtils.loadProperties( f );

        // project build properties
        Properties userOverridesMavenProperties = null;

        if ( descriptorDirectory != null )
        {
            f = new File( descriptorDirectory, MAVEN_PROPERTIES );

            userOverridesMavenProperties = PropertyUtils.loadProperties( f );
        }

        Map result = CollectionUtils.mergeMaps( new Map[]
        {
            systemProperties,
            mavenProperties,
            userOverridesMavenProperties,
        } );

        // Set the basedir value in the context.
        result.put( "basedir", descriptorDirectory.getAbsolutePath() );

        for ( Iterator i = result.keySet().iterator(); i.hasNext(); )
        {
            String key = (String) i.next();

            String value = (String) result.get( key );

            result.put( key, StringUtils.interpolate( value, result ) );
        }

        return result;
    }

    private Model interpolateModel( Model model, Map map )
        throws Exception
    {
        return modelReader.read( new StringReader( StringUtils.interpolate( getProjectString( model ), map ) ) );
    }

    private String getProjectString( Model project )
        throws Exception
    {
        StringWriter writer = new StringWriter();

        modelWriter.write( writer, project );

        return writer.toString();
    }

    // ----------------------------------------------------------------------
    //
    // 1. collect all the vertices for the projects that we want to build.
    //
    // 2. iterate through the deps of each project and if that dep is within
    //    the set of projects we want to build then add an edge, otherwise throw
    //    the edge away because that dependency is not within the set of projects
    //    we are trying to build. we assume a closed set.
    //
    // 3. do a topo sort on the graph that remains.
    //
    // ----------------------------------------------------------------------

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

    private boolean isEmpty( String string )
    {
        return string == null || string.trim().length() == 0;
    }
}
