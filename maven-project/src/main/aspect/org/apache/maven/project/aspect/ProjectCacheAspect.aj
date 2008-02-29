package org.apache.maven.project.aspect;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.DefaultMavenProjectBuilder;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.build.model.ModelLineage;
import org.apache.maven.project.build.model.DefaultModelLineageBuilder;
import org.apache.maven.project.build.model.ModelAndFile;

import org.codehaus.plexus.util.StringUtils;

import java.io.File;

public privileged aspect ProjectCacheAspect
{

    private pointcut mavenProjectBuilder( DefaultMavenProjectBuilder builder ):
        execution( * DefaultMavenProjectBuilder+.*( .. ) )
        && this( builder );

//    private pointcut setMavenProjectParent( MavenProject child, MavenProject parent, DefaultMavenProjectBuilder builder ):
//        call( void MavenProject.setParent( MavenProject ) )
//        && cflow( mavenProjectBuilder( builder ) )
//        && within( DefaultMavenProjectBuilder+ )
//        && !within( ProjectCacheAspect )
//        && args( parent )
//        && target( child );
//
//    void around( MavenProject child, MavenProject parent, DefaultMavenProjectBuilder builder ): setMavenProjectParent( child, parent, builder )
//    {
//        // if the incoming project is null, don't get involved.
//        if ( parent != null )
//        {
//            String key = createCacheKey( parent.getGroupId(), parent.getArtifactId(), parent.getVersion() );
//
//            builder.logger.debug( "Checking cache for parent project instance: " + key );
//            MavenProject cachedProject = (MavenProject) builder.projectCache.get( key );
//
//            // if the cached project is null, don't get involved.
//            if ( cachedProject != null )
//            {
//                builder.logger.debug( "Using cached parent project instance in child: " + child.getId() );
//                proceed( child, cachedProject, builder );
//                return;
//            }
//        }
//
//        builder.logger.debug( "Using original (passed) parent project instance in child: " + child.getId() );
//        proceed( child, parent, builder );
//    }

    private pointcut pbBuildFromRepository( Artifact artifact, DefaultMavenProjectBuilder builder ):
        execution( MavenProject DefaultMavenProjectBuilder+.buildFromRepository( Artifact, .. ) )
        && args( artifact, .. )
        && this( builder );

    MavenProject around( Artifact artifact, DefaultMavenProjectBuilder builder )
        throws ProjectBuildingException:
            pbBuildFromRepository( artifact, builder )
    {
        String key = createCacheKey( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() );
        MavenProject project = null;

        boolean skipCache = false;
        if ( !Artifact.LATEST_VERSION.equals( artifact.getVersion() ) && !Artifact.RELEASE_VERSION.equals( artifact.getVersion() ) )
        {
            builder.logger.debug( "Checking cache for project (in buildFromRepository): " + key );
            project = (MavenProject) builder.projectWorkspace.getProject( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion() );
        }

        if ( project == null )
        {
            builder.logger.debug( "Allowing buildFromRepository to proceed for: " + key );
            project = proceed( artifact, builder );

            if ( !skipCache )
            {
                builder.logger.debug( "Caching result for: " + key + " (also keyed by file: " + project.getFile() + ")" );
                builder.projectWorkspace.storeProjectByCoordinate( project );
                builder.projectWorkspace.storeProjectByFile( project );
            }
        }
        else
        {
            builder.logger.debug( "Returning cached project: " + project );
        }

        return project;
    }

    private pointcut pbBuildFromFile( File pomFile, DefaultMavenProjectBuilder builder ):
        execution( MavenProject DefaultMavenProjectBuilder.buildFromSourceFileInternal( File, .. ) )
        && args( pomFile, .. )
        && this( builder );

    MavenProject around( File pomFile, DefaultMavenProjectBuilder builder )
        throws ProjectBuildingException:
            pbBuildFromFile( pomFile, builder )
    {
        builder.logger.debug( "Checking cache-hit on project (in build*): " + pomFile );

        MavenProject project = (MavenProject) builder.projectWorkspace.getProject( pomFile );

        if ( project == null )
        {
            builder.logger.debug( "Allowing project-build to proceed for: " + pomFile );
            project = proceed( pomFile, builder );

            String key = createCacheKey( project.getGroupId(), project.getArtifactId(), project.getVersion() );

            builder.logger.debug( "Caching result for: " + key + " (also keyed by file: " + pomFile + ")" );
            builder.projectWorkspace.storeProjectByFile( project );
            builder.projectWorkspace.storeProjectByCoordinate( project );
        }
        else
        {
            builder.logger.debug( "Returning cached project: " + project );
        }

        builder.logger.debug( "Project: " + project.getId() + " has basedir: " + project.getBasedir() );

        return project;
    }

    private String createCacheKey( String groupId, String artifactId, String version )
    {
        return groupId + ":" + artifactId + ":" + version;
    }

    private pointcut mlbResolveParentPom( ModelAndFile child, DefaultModelLineageBuilder builder ):
        execution( private ModelAndFile DefaultModelLineageBuilder.resolveParentPom( ModelAndFile, .. ) )
        && args( child, .. )
        && this( builder );

    ModelAndFile around( ModelAndFile child, DefaultModelLineageBuilder builder )
        throws ProjectBuildingException:
            mlbResolveParentPom( child, builder )
    {
        Model childModel = child.getModel();
        Parent parentRef = childModel.getParent();

        ModelAndFile parent = null;

        if ( parentRef != null
             && !StringUtils.isEmpty( parentRef.getGroupId() )
             && !StringUtils.isEmpty( parentRef.getArtifactId() )
             && !StringUtils.isEmpty( parentRef.getVersion() ) )
        {
            String key = createCacheKey( parentRef.getGroupId(), parentRef.getArtifactId(), parentRef.getVersion() );

            builder.logger.debug( "Checking cache for parent model-and-file instance: " + key );
            parent = (ModelAndFile) builder.projectWorkspace.getModelAndFile( parentRef.getGroupId(), parentRef.getArtifactId(), parentRef.getVersion() );

            if ( parent == null )
            {
                builder.logger.debug( "Allowing parent-model resolution to proceed for: " + key + " (child is: " + childModel.getId() + ")" );
                parent = proceed( child, builder );

                if ( parent != null )
                {
                    builder.logger.debug( "Caching parent model-and-file under: " + key + " and file: " + parent.getFile() + " (child is: " + childModel.getId() + ")" );
                    builder.projectWorkspace.storeModelAndFile( parent );
                }
            }
            else
            {
                builder.logger.debug( "Returning cached instance." );
            }
        }

        return parent;
    }

    private pointcut mlbReadModelCacheHit( File pomFile, DefaultModelLineageBuilder builder ):
        call( Model DefaultModelLineageBuilder.readModel( File ) )
        && withincode( ModelLineage DefaultModelLineageBuilder.buildModelLineage( .. ) )
        && args( pomFile )
        && this( builder );

    Model around( File pomFile, DefaultModelLineageBuilder builder )
        throws ProjectBuildingException:
            mlbReadModelCacheHit( pomFile, builder )
    {
        builder.logger.debug( "Checking cache for model-and-file instance for pom in file: " + pomFile );
        ModelAndFile cached = (ModelAndFile) builder.projectWorkspace.getModelAndFile( pomFile );
        if ( cached != null )
        {
            builder.logger.debug( "Returning cached pom instance." );
            return cached.getModel();
        }

        builder.logger.debug( "Allowing readModel(..) to proceed for pom in file: " + pomFile );
        return proceed( pomFile, builder );
    }

    private pointcut mlbCacheableModelAndFileConstruction( Model model, File pomFile, DefaultModelLineageBuilder builder ):
        call( ModelAndFile.new( Model, File, .. ) )
        && withincode( ModelLineage DefaultModelLineageBuilder.buildModelLineage( .. ) )
        && args( model, pomFile, .. )
        && this( builder );

    ModelAndFile around( Model model, File pomFile, DefaultModelLineageBuilder builder ):
        mlbCacheableModelAndFileConstruction( model, pomFile, builder )
    {
        builder.logger.debug( "Checking cache for model-and-file instance for file: " + pomFile );
        ModelAndFile cached = (ModelAndFile) builder.projectWorkspace.getModelAndFile( pomFile );
        if ( cached == null )
        {
            builder.logger.debug( "Allowing construction to proceed for model-and-file with model: " + model.getId() + " and file: " + pomFile );
            cached = proceed( model, pomFile, builder );

            builder.logger.debug( "Storing: " + cached );
            builder.projectWorkspace.storeModelAndFile( cached );
        }
        else
        {
            builder.logger.debug( "Returning cached model-and-file instance." );
        }

        return cached;
    }
}
