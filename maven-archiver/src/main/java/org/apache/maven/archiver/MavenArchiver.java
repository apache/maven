package org.apache.maven.archiver;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.archiver.jar.ManifestException;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Revision$ $Date$
 * @todo improve the use of this now that plugin fields are used instead of a request object - add an <archive> element to configuration?
 */
public class MavenArchiver
{
    private JarArchiver archiver = new JarArchiver();

    private File archiveFile;

    /**
     * Return a pre-configured manifest
     *
     * @todo Add user attributes list and user groups list
     */
    public Manifest getManifest( MavenProject project, ManifestConfiguration config )
        throws ManifestException
    {
        // Added basic entries
        Manifest m = new Manifest();
        Manifest.Attribute buildAttr = new Manifest.Attribute( "Built-By", System.getProperty( "user.name" ) );
        m.addConfiguredAttribute( buildAttr );
        Manifest.Attribute createdAttr = new Manifest.Attribute( "Created-By", "Apache Maven" );
        m.addConfiguredAttribute( createdAttr );

        if ( config.getPackageName() != null )
        {
            Manifest.Attribute packageAttr = new Manifest.Attribute( "Package", config.getPackageName() );
            m.addConfiguredAttribute( packageAttr );
        }

        Manifest.Attribute buildJdkAttr = new Manifest.Attribute( "Build-Jdk", System.getProperty( "java.version" ) );
        m.addConfiguredAttribute( buildJdkAttr );

        if ( config.isAddClasspath() )
        {
            StringBuffer classpath = new StringBuffer();
            Set artifacts = project.getArtifacts();

            for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
            {
                Artifact artifact = (Artifact) iter.next();
                if ( "jar".equals( artifact.getType() ) && Artifact.SCOPE_RUNTIME.equals( artifact.getScope() ) )
                {
                    if ( classpath.length() > 0 )
                    {
                        classpath.append( " " );
                    }

                    classpath.append( artifact.getArtifactId() + "-" + artifact.getVersion() + ".jar" );
                }
            }

            if ( classpath.length() > 0 )
            {
                Manifest.Attribute classpathAttr = new Manifest.Attribute( "Class-Path", classpath.toString() );
                m.addConfiguredAttribute( classpathAttr );
            }
        }

        // Added supplementary entries
        Manifest.Attribute extensionNameAttr = new Manifest.Attribute( "Extension-Name", project.getArtifactId() );
        m.addConfiguredAttribute( extensionNameAttr );

        if ( project.getDescription() != null )
        {
            Manifest.Attribute specificationTitleAttr = new Manifest.Attribute( "Specification-Title",
                                                                                project.getDescription() );
            m.addConfiguredAttribute( specificationTitleAttr );
        }

        if ( project.getOrganization() != null )
        {
            Manifest.Attribute specificationVendor = new Manifest.Attribute( "Specification-Vendor",
                                                                             project.getOrganization().getName() );
            m.addConfiguredAttribute( specificationVendor );
            Manifest.Attribute implementationVendorAttr = new Manifest.Attribute( "Implementation-Vendor",
                                                                                  project.getOrganization().getName() );
            m.addConfiguredAttribute( implementationVendorAttr );
        }

        Manifest.Attribute implementationTitleAttr = new Manifest.Attribute( "Implementation-Title",
                                                                             project.getArtifactId() );
        m.addConfiguredAttribute( implementationTitleAttr );
        Manifest.Attribute implementationVersionAttr = new Manifest.Attribute( "Implementation-Version",
                                                                               project.getVersion() );
        m.addConfiguredAttribute( implementationVersionAttr );

        String mainClass = config.getMainClass();
        if ( mainClass != null && !"".equals( mainClass ) )
        {
            Manifest.Attribute mainClassAttr = new Manifest.Attribute( "Main-Class", mainClass );
            m.addConfiguredAttribute( mainClassAttr );
        }

        // Added extensions
        if ( config.isAddExtensions() )
        {
            StringBuffer extensionsList = new StringBuffer();
            Set artifacts = project.getArtifacts();

            for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
            {
                Artifact artifact = (Artifact) iter.next();
                if ( "jar".equals( artifact.getType() ) )
                {
                    if ( extensionsList.length() > 0 )
                    {
                        extensionsList.append( " " );
                    }
                    extensionsList.append( artifact.getArtifactId() );
                }
            }

            if ( extensionsList.length() > 0 )
            {
                Manifest.Attribute extensionsListAttr = new Manifest.Attribute( "Extension-List",
                                                                                extensionsList.toString() );
                m.addConfiguredAttribute( extensionsListAttr );
            }

            for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
            {
                Artifact artifact = (Artifact) iter.next();
                if ( "jar".equals( artifact.getType() ) )
                {
                    Manifest.Attribute archExtNameAttr = new Manifest.Attribute( artifact.getArtifactId() +
                                                                                 "-Extension-Name",
                                                                                 artifact.getArtifactId() );
                    m.addConfiguredAttribute( archExtNameAttr );
                    String name = artifact.getArtifactId() + "-Implementation-Version";
                    Manifest.Attribute archImplVersionAttr = new Manifest.Attribute( name, artifact.getVersion() );
                    m.addConfiguredAttribute( archImplVersionAttr );

                    if ( artifact.getRepository() != null )
                    {
                        // TODO: is this correct
                        name = artifact.getArtifactId() + "-Implementation-URL";
                        String url = artifact.getRepository().getUrl() + "/" + artifact.toString();
                        Manifest.Attribute archImplUrlAttr = new Manifest.Attribute( name, url );
                        m.addConfiguredAttribute( archImplUrlAttr );
                    }
                }
            }
        }

        return m;
    }

    public JarArchiver getArchiver()
    {
        return archiver;
    }

    public void setArchiver( JarArchiver archiver )
    {
        this.archiver = archiver;
    }

    public void setOutputFile( File outputFile )
    {
        archiveFile = outputFile;
    }

    public void createArchive( MavenProject project, MavenArchiveConfiguration archiveConfiguration )
        throws ArchiverException, ManifestException, IOException
    {
        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        archiver.addFile( project.getFile(), "META-INF/maven/pom.xml" );

        String manifestFile = archiveConfiguration.getManifestFile();
        if ( manifestFile != null && !"".equals( manifestFile ) )
        {
            archiver.setManifest( new File( manifestFile ) );
        }

        Manifest manifest = getManifest( project, archiveConfiguration.getManifest() );

        // Configure the jar
        archiver.addConfiguredManifest( manifest );

        archiver.setCompress( archiveConfiguration.isCompress() );
        archiver.setIndex( archiveConfiguration.isIndex() );
        archiver.setDestFile( archiveFile );

        // create archive
        archiver.createArchive();
    }
}
