package org.apache.maven.extension;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.context.BuildContextManager;
import org.apache.maven.model.Build;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.activation.CustomActivatorAdvice;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.build.model.ModelLineage;
import org.apache.maven.project.build.model.ModelLineageBuilder;
import org.apache.maven.project.build.model.ModelLineageIterator;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.project.interpolation.ModelInterpolator;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class DefaultBuildExtensionScanner
    implements BuildExtensionScanner, LogEnabled
{

    private Logger logger;

    private BuildContextManager buildContextManager;

    private ExtensionManager extensionManager;

    private MavenProjectBuilder projectBuilder;

    private ModelLineageBuilder modelLineageBuilder;
    
    private ModelInterpolator modelInterpolator;

    public void scanForBuildExtensions( File pom, ArtifactRepository localRepository,
                                        ProfileManager globalProfileManager, Map pomFilesById )
        throws ExtensionScanningException
    {
        // setup the CustomActivatorAdvice to fail quietly while we discover extensions...then, we'll
        // reset it.
        CustomActivatorAdvice activatorAdvice = CustomActivatorAdvice.getCustomActivatorAdvice( buildContextManager );
        activatorAdvice.setFailQuietly( true );
        activatorAdvice.store( buildContextManager );

        try
        {
            List originalRemoteRepositories = getInitialRemoteRepositories( localRepository, globalProfileManager );

            getLogger().debug( "Pre-scanning POM lineage of: " + pom + " for build extensions." );

            ModelLineage lineage = buildModelLineage( pom, localRepository, originalRemoteRepositories,
                                                      globalProfileManager, pomFilesById );

            Map inheritedInterpolationValues = new HashMap();
            
            for ( ModelLineageIterator lineageIterator = lineage.reversedLineageIterator(); lineageIterator.hasNext(); )
            {
                Model model = (Model) lineageIterator.next();

                getLogger().debug( "Checking: " + model.getId() + " for extensions." );
                
                if ( inheritedInterpolationValues == null )
                {
                    inheritedInterpolationValues = new HashMap();
                }
                
                model = modelInterpolator.interpolate( model, inheritedInterpolationValues, false );

                checkModelBuildForExtensions( model, localRepository, lineageIterator.getArtifactRepositories() );

                checkModulesForExtensions( pom, model, localRepository, originalRemoteRepositories, globalProfileManager,
                                           pomFilesById );
                
                Properties modelProps = model.getProperties();
                if ( modelProps != null )
                {
                    inheritedInterpolationValues.putAll( modelProps );
                }
            }

            getLogger().debug( "Finished pre-scanning: " + pom + " for build extensions." );
        }
        catch ( ModelInterpolationException e )
        {
            throw new ExtensionScanningException( "Failed to interpolate model from: " + pom + " prior to scanning for extensions.", e );
        }
        finally
        {
            activatorAdvice.reset();
            activatorAdvice.store( buildContextManager );
        }
    }

    private void checkModulesForExtensions( File containingPom, Model model, ArtifactRepository localRepository,
                                            List originalRemoteRepositories, ProfileManager globalProfileManager,
                                            Map pomFilesById )
        throws ExtensionScanningException
    {
        // FIXME: This gets a little sticky, because modules can be added by profiles that require
        // an extension in place before they can be activated.
        List modules = model.getModules();

        if ( modules != null )
        {
            File basedir = containingPom.getParentFile();

            for ( Iterator it = modules.iterator(); it.hasNext(); )
            {
                // TODO: change this if we ever find a way to replace module definitions with g:a:v
                String moduleSubpath = (String) it.next();
                
                getLogger().debug( "Scanning module: " + moduleSubpath );

                File modulePom = new File( basedir, moduleSubpath );

                if ( modulePom.isDirectory() )
                {
                    getLogger().debug(
                                       "Assuming POM file 'pom.xml' in module: " + moduleSubpath + " under basedir: "
                                           + basedir );
                    modulePom = new File( modulePom, "pom.xml" );
                }

                if ( !modulePom.exists() )
                {
                    getLogger().debug(
                                       "Cannot find POM for module: " + moduleSubpath
                                           + "; continuing scan with next module." );
                    continue;
                }

                scanForBuildExtensions( modulePom, localRepository, globalProfileManager, pomFilesById );
            }
        }
    }

    private void checkModelBuildForExtensions( Model model, ArtifactRepository localRepository, List remoteRepositories )
        throws ExtensionScanningException
    {
        Build build = model.getBuild();

        if ( build != null )
        {
            List extensions = build.getExtensions();

            if ( extensions != null && !extensions.isEmpty() )
            {
                // thankfully, we don't have to deal with dependencyManagement here, yet.
                // TODO Revisit if/when extensions are made to use the info in dependencyManagement
                for ( Iterator extensionIterator = extensions.iterator(); extensionIterator.hasNext(); )
                {
                    Extension extension = (Extension) extensionIterator.next();

                    getLogger().debug(
                                       "Adding extension: "
                                           + ArtifactUtils.versionlessKey( extension.getGroupId(), extension
                                               .getArtifactId() ) + " from model: " + model.getId() );

                    try
                    {
                        extensionManager.addExtension( extension, model, remoteRepositories, localRepository );
                    }
                    catch ( ArtifactResolutionException e )
                    {
                        throw new ExtensionScanningException( "Cannot resolve pre-scanned extension artifact: "
                            + extension.getGroupId() + ":" + extension.getArtifactId() + ": " + e.getMessage(), e );
                    }
                    catch ( ArtifactNotFoundException e )
                    {
                        throw new ExtensionScanningException( "Cannot find pre-scanned extension artifact: "
                            + extension.getGroupId() + ":" + extension.getArtifactId() + ": " + e.getMessage(), e );
                    }
                    catch ( PlexusContainerException e )
                    {
                        throw new ExtensionScanningException( "Failed to add pre-scanned extension: "
                            + extension.getGroupId() + ":" + extension.getArtifactId() + ": " + e.getMessage(), e );
                    }
                }
            }
        }
    }

    private ModelLineage buildModelLineage( File pom, ArtifactRepository localRepository,
                                            List originalRemoteRepositories, ProfileManager globalProfileManager,
                                            Map cache )
        throws ExtensionScanningException
    {
        ModelLineage lineage;
        try
        {
            getLogger().debug( "Building model-lineage for: " + pom + " to pre-scan for extensions." );

            lineage = modelLineageBuilder.buildModelLineage( pom, localRepository, originalRemoteRepositories,
                                                             globalProfileManager, cache );
        }
        catch ( ProjectBuildingException e )
        {
            throw new ExtensionScanningException( "Error building model lineage in order to pre-scan for extensions: "
                + e.getMessage(), e );
        }

        return lineage;
    }

    private List getInitialRemoteRepositories( ArtifactRepository localRepository, ProfileManager globalProfileManager )
        throws ExtensionScanningException
    {
        MavenProject superProject;
        try
        {
            superProject = projectBuilder.buildStandaloneSuperProject( localRepository, globalProfileManager );
        }
        catch ( ProjectBuildingException e )
        {
            throw new ExtensionScanningException(
                                                  "Error building super-POM for retrieving the default remote repository list: "
                                                      + e.getMessage(), e );
        }

        return superProject.getRemoteArtifactRepositories();
    }

    protected Logger getLogger()
    {
        if ( logger == null )
        {
            logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "DefaultBuildExtensionScanner:internal" );
        }

        return logger;
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

}
