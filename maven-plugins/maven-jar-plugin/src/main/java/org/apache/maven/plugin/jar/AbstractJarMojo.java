package org.apache.maven.plugin.jar;

/**
 *
 * Copyright 2004-2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.project.MavenProject;

import org.codehaus.plexus.archiver.jar.Manifest;
import org.codehaus.plexus.util.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Base class for tasks that build archives in JAR file format.
 *
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Revision$ $Date$
 */
public abstract class AbstractJarMojo
    extends AbstractPlugin
{
    /**
     * Return a pre-configured manifest
     * @todo Add user attributes list and user groups list
     */
    public Manifest getManifest( PluginExecutionRequest request )
        throws Exception
    {
        MavenProject project = (MavenProject)request.getParameter("project");

        String mainClass = (String) request.getParameter( "mainClass" );

        boolean addClasspath = new Boolean( (String) request.getParameter( "addClasspath" ) ).booleanValue();

        boolean addExtensions = new Boolean( (String) request.getParameter( "addExtensions" ) ).booleanValue();

        // Added basic entries
        Manifest m = new Manifest();
        Manifest.Attribute buildAttr = new Manifest.Attribute( "Built-By", System.getProperty( "user.name" ) );
        m.addConfiguredAttribute( buildAttr );
        Manifest.Attribute createdAttr = new Manifest.Attribute( "Created-By", "Apache Maven" );
        m.addConfiguredAttribute( createdAttr );
        Manifest.Attribute packageAttr = new Manifest.Attribute( "Package", project.getPackage() );
        m.addConfiguredAttribute( packageAttr );
        Manifest.Attribute buildJdkAttr = new Manifest.Attribute( "Build-Jdk", System.getProperty( "java.version" ) );
        m.addConfiguredAttribute( buildJdkAttr );

        if ( addClasspath )
        {
            StringBuffer classpath = new StringBuffer();
            List dependencies = project.getDependencies();
    
            for ( Iterator iter = dependencies.iterator(); iter.hasNext(); )
            {
                Dependency dependency = (Dependency) iter.next();
                Properties properties = dependency.getProperties();
                if ( Boolean.valueOf(properties.getProperty("jar.manifest.classpath")).booleanValue())
                {
                    if (classpath.length() > 0 )
                    {
                        classpath.append( " " );
                    }

                    // TODO replace dependency by artifact
                    classpath.append( dependency.getArtifactId() + "-" + dependency.getVersion() + ".jar");
                }
            }
            
            Manifest.Attribute classpathAttr = new Manifest.Attribute( "Class-Path", classpath.toString() );
            m.addConfiguredAttribute( classpathAttr );
        }

        // Added supplementary entries
        Manifest.Attribute extensionNameAttr = new Manifest.Attribute( "Extension-Name", project.getArtifactId() );
        m.addConfiguredAttribute( extensionNameAttr );
        
        if ( project.getShortDescription() != null )
        {
            Manifest.Attribute specificationTitleAttr = new Manifest.Attribute( "Specification-Title", project.getShortDescription() );
            m.addConfiguredAttribute( specificationTitleAttr );
        }
        
        if ( project.getOrganization() != null )
        {
            Manifest.Attribute specificationVendor = new Manifest.Attribute( "Specification-Vendor", project.getOrganization().getName() );
            m.addConfiguredAttribute( specificationVendor );
            Manifest.Attribute implementationVendorAttr = new Manifest.Attribute( "Implementation-Vendor", project.getOrganization().getName() );
            m.addConfiguredAttribute( implementationVendorAttr );
        }

        Manifest.Attribute implementationTitleAttr = new Manifest.Attribute( "Implementation-Title", project.getArtifactId() );
        m.addConfiguredAttribute( implementationTitleAttr );
        Manifest.Attribute implementationVersionAttr = new Manifest.Attribute( "Implementation-Version", project.getVersion() );
        m.addConfiguredAttribute( implementationVersionAttr );

        if ( mainClass != null && ! "".equals( mainClass ) )
        {
            Manifest.Attribute mainClassAttr = new Manifest.Attribute( "Main-Class", mainClass );
            m.addConfiguredAttribute( mainClassAttr );
        }

        // Added extensions
        if ( addExtensions )
        {
            StringBuffer extensionsList = new StringBuffer();
            Set artifacts = project.getArtifacts();

            for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
            {
                Artifact artifact = (Artifact) iter.next();
                if ( "jar".equals( artifact.getType() ) )
                {
                    if (extensionsList.length() > 0 )
                    {
                        extensionsList.append( " " );
                    }
                    extensionsList.append( artifact.getArtifactId() );
                }
            }

            if (extensionsList.length() > 0 )
            {
                Manifest.Attribute extensionsListAttr = new Manifest.Attribute( "Extension-List", extensionsList.toString() );
                m.addConfiguredAttribute( extensionsListAttr );
            }
            
            for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
            {
                Artifact artifact = (Artifact) iter.next();
                if ( "jar".equals( artifact.getType() ) )
                {
                    Manifest.Attribute archExtNameAttr = new Manifest.Attribute( artifact.getArtifactId()
                        + "-Extension-Name", artifact.getArtifactId() );
                    m.addConfiguredAttribute( archExtNameAttr );
                    Manifest.Attribute archImplVersionAttr = new Manifest.Attribute( artifact.getArtifactId()
                        + "-Implementation-Version", artifact.getVersion() );
                    m.addConfiguredAttribute( archImplVersionAttr );
                    Manifest.Attribute archImplUrlAttr = new Manifest.Attribute( artifact.getArtifactId()
                        + "-Implementation-URL", "http://www.ibiblio.org/maven/" + artifact.toString() );
                    m.addConfiguredAttribute( archImplUrlAttr );
                }
            }
        }

        return m;
    }
}
