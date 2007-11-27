package org.apache.maven.project.error;

import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.RepositoryBase;
import org.apache.maven.profiles.activation.ProfileActivationContext;
import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.profiles.activation.ProfileActivator;
import org.apache.maven.project.InvalidProjectVersionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.project.validation.ModelValidationResult;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ProjectErrorTips
{

    private ProjectErrorTips()
    {
    }

    public static List<String> getTipsForActivatorErrorWhileApplyingProfiles( ProfileActivator activator,
                                                                              Model model,
                                                                              File pomFile,
                                                                              Profile profile,
                                                                              ProfileActivationContext context,
                                                                              ProfileActivationException cause )
    {
        return listOf( "If this is a standard profile activator, see "
                                       + "http://maven.apache.org/pom.html#Activation for help configuring profile activation.",
                       "XSD location for pom.xml: http://maven.apache.org/xsd/maven-4.0.0.xsd",
                       "XSD location for settings.xml: http://maven.apache.org/xsd/settings-1.0.0.xsd",
                       "XSD location for profiles.xml: http://maven.apache.org/xsd/profiles-1.0.0.xsd" );
    }

    public static List<String> getTipsForActivatorErrorWhileGettingRepositoriesFromProfiles( ProfileActivator activator,
                                                                                             String projectId,
                                                                                             File pomFile,
                                                                                             Profile profile,
                                                                                             ProfileActivationContext context,
                                                                                             ProfileActivationException cause )
    {
        return listOf( "If this is a standard profile activator, see "
                                       + "http://maven.apache.org/pom.html#Activation for help configuring profile activation.",
                       "XSD location for pom.xml: http://maven.apache.org/xsd/maven-4.0.0.xsd",
                       "XSD location for settings.xml: http://maven.apache.org/xsd/settings-1.0.0.xsd",
                       "XSD location for profiles.xml: http://maven.apache.org/xsd/profiles-1.0.0.xsd" );
    }

    public static List<String> getTipsForActivatorLookupErrorWhileApplyingProfiles( Model model,
                                                                                    File pomFile,
                                                                                    Profile profile,
                                                                                    ComponentLookupException cause )
    {
        return listOf( "If this is a custom profile activator, please ensure the activator's "
                                       + "artifact is present in the POM's build/extensions list.",
                       "See http://maven.apache.org/pom.html#Extensions for more on build extensions.",
                       "XSD location for pom.xml: http://maven.apache.org/xsd/maven-4.0.0.xsd",
                       "XSD location for settings.xml: http://maven.apache.org/xsd/settings-1.0.0.xsd",
                       "XSD location for profiles.xml: http://maven.apache.org/xsd/profiles-1.0.0.xsd" );
    }

    public static List<String> getTipsForActivatorLookupErrorWhileGettingRepositoriesFromProfiles( String projectId,
                                                                                                   File pomFile,
                                                                                                   Profile profile,
                                                                                                   ComponentLookupException cause )
    {
        return listOf( "If this is a custom profile activator, please ensure the activator's "
                                       + "artifact is present in the POM's build/extensions list.",
                       "See http://maven.apache.org/pom.html#Extensions for more on build extensions.",
                       "XSD location for pom.xml: http://maven.apache.org/xsd/maven-4.0.0.xsd",
                       "XSD location for settings.xml: http://maven.apache.org/xsd/settings-1.0.0.xsd",
                       "XSD location for profiles.xml: http://maven.apache.org/xsd/profiles-1.0.0.xsd" );
    }

    public static List<String> getTipsForErrorLoadingExternalProfilesFromFile( Model model,
                                                                               File pomFile,
                                                                               File projectDir,
                                                                               IOException cause )
    {
        String profilesXmlPath = new File( projectDir, "profiles.xml" ).getAbsolutePath();

        return listOf( "Please ensure the " + profilesXmlPath + " file exists and is readable." );
    }

    public static List<String> getTipsForErrorLoadingExternalProfilesFromFile( Model model,
                                                                               File pomFile,
                                                                               File projectDir,
                                                                               XmlPullParserException cause )
    {
        return listOf( "XSD location: http://maven.apache.org/xsd/profiles-1.0.0.xsd" );
    }

    public static List<String> getTipsForInvalidRepositorySpec( RepositoryBase repo,
                                                                String projectId,
                                                                File pomFile,
                                                                InvalidRepositoryException cause )
    {
        return listOf( "See http://maven.apache.org/pom.html#Repositories for more on custom artifact repositories.",
                       "See http://maven.apache.org/pom.html#PluginRepositories for more on custom plugin repositories.",
                       "XSD location for pom.xml: http://maven.apache.org/xsd/maven-4.0.0.xsd",
                       "XSD location for settings.xml: http://maven.apache.org/xsd/settings-1.0.0.xsd",
                       "XSD location for profiles.xml: http://maven.apache.org/xsd/profiles-1.0.0.xsd" );
    }

    private static List<String> listOf( String... tips )
    {
        List<String> list = new ArrayList<String>();

        for ( String tip : tips )
        {
            list.add( tip );
        }

        return list;
    }

    public static List<String> getTipsForProjectValidationFailure( MavenProject project,
                                                                   File pomFile,
                                                                   ModelValidationResult validationResult )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List<String> getTipsForBadDependencySpec( MavenProject project,
                                                            File pomFile,
                                                            Dependency dep )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List<String> getTipsForBadNonDependencyArtifactSpec( MavenProject project,
                                                                       File pomFile,
                                                                       InvalidProjectVersionException cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List<String> getTipsForProjectInterpolationError( MavenProject project,
                                                                    File pomFile,
                                                                    ModelInterpolationException cause )
    {
        // TODO Auto-generated method stub
        return null;
    }
}
