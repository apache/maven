package org.apache.maven.project.error;

import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.model.RepositoryBase;
import org.apache.maven.profiles.activation.ProfileActivationContext;
import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.profiles.activation.ProfileActivator;
import org.apache.maven.project.DuplicateProjectException;
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

// NOTE: The strange String[] syntax is a backward adaptation from java5 stuff, where
// I was using varargs in listOf(..). I'm not moving them to constants because I'd like
// to go back to this someday...

// TODO: Optimize the String[] instances in here to List constants, and remove listOf(..)
public final class ProjectErrorTips
{

    private ProjectErrorTips()
    {
    }

    public static List getTipsForActivatorError( ProfileActivator activator,
                                                 String projectId,
                                                 File pomFile,
                                                 Profile profile,
                                                 ProfileActivationContext context,
                                                 ProfileActivationException cause )
    {
        return listOf( new String[]{  "If this is a standard profile activator, see "
                                       + "http://maven.apache.org/pom.html#Activation for help configuring profile activation.",
                       "XSD location for pom.xml: http://maven.apache.org/xsd/maven-4.0.0.xsd",
                       "XSD location for settings.xml: http://maven.apache.org/xsd/settings-1.0.0.xsd",
                       "XSD location for profiles.xml: http://maven.apache.org/xsd/profiles-1.0.0.xsd" } );
    }

    public static List getTipsForActivatorLookupError( String projectId,
                                                       File pomFile,
                                                       Profile profile,
                                                       ComponentLookupException cause )
    {
        return listOf( new String[]{  "If this is a custom profile activator, please ensure the activator's "
                                       + "artifact is present in the POM's build/extensions list.",
                       "See http://maven.apache.org/pom.html#Extensions for more on build extensions.",
                       "XSD location for pom.xml: http://maven.apache.org/xsd/maven-4.0.0.xsd",
                       "XSD location for settings.xml: http://maven.apache.org/xsd/settings-1.0.0.xsd",
                       "XSD location for profiles.xml: http://maven.apache.org/xsd/profiles-1.0.0.xsd" } );
    }

    public static List getTipsForErrorLoadingExternalProfilesFromFile( Model model,
                                                                               File pomFile,
                                                                               File projectDir,
                                                                               IOException cause )
    {
        String profilesXmlPath = new File( projectDir, "profiles.xml" ).getAbsolutePath();

        return listOf( new String[]{  "Please ensure the " + profilesXmlPath + " file exists and is readable." } );
    }

    public static List getTipsForErrorLoadingExternalProfilesFromFile( Model model,
                                                                               File pomFile,
                                                                               File projectDir,
                                                                               XmlPullParserException cause )
    {
        return listOf( new String[]{  "XSD location: http://maven.apache.org/xsd/profiles-1.0.0.xsd" } );
    }

    public static List getTipsForInvalidRepositorySpec( RepositoryBase repo,
                                                                String projectId,
                                                                File pomFile,
                                                                InvalidRepositoryException cause )
    {
        return listOf( new String[]{  "See http://maven.apache.org/pom.html#Repositories for more on custom artifact repositories.",
                       "See http://maven.apache.org/pom.html#PluginRepositories for more on custom plugin repositories.",
                       "XSD location for pom.xml: http://maven.apache.org/xsd/maven-4.0.0.xsd",
                       "XSD location for settings.xml: http://maven.apache.org/xsd/settings-1.0.0.xsd",
                       "XSD location for profiles.xml: http://maven.apache.org/xsd/profiles-1.0.0.xsd" } );
    }

    private static List listOf( String[] tips )
    {
        List list = new ArrayList();

        for ( int i = 0; i < tips.length; i++ )
        {
            list.add( tips[i] );
        }

        return list;
    }

    public static List getTipsForProjectValidationFailure( MavenProject project,
                                                                   File pomFile,
                                                                   ModelValidationResult validationResult )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getTipsForBadDependencySpec( MavenProject project,
                                                            File pomFile,
                                                            Dependency dep )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getTipsForBadNonDependencyArtifactSpec( MavenProject project,
                                                                       File pomFile,
                                                                       InvalidProjectVersionException cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getTipsForProjectInterpolationError( MavenProject project,
                                                                    File pomFile,
                                                                    ModelInterpolationException cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getTipsForPomParsingError( String projectId,
                                                  File pomFile,
                                                  Exception cause )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static List getTipsForDuplicateProjectError( List allProjectInstances,
                                                        DuplicateProjectException err )
    {
        // TODO Auto-generated method stub
        return null;
    }

}
