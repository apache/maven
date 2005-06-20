package org.apache.maven.plugin.idea;

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
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;

/**
 * Goal for generating IDEA files from a POM.
 *
 * @goal idea
 * @execute phase="generate-sources"
 * @requiresDependencyResolution test
 * @todo use dom4j or something. Xpp3Dom can't cope properly with entities and so on
 */
public class IdeaMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${executedProject}"
     * @required
     * @readonly
     */
    private MavenProject executedProject;

    public void execute()
        throws MojoExecutionException
    {
        rewriteModule();

        rewriteProject();

        rewriteWorkspace();
    }

    private void rewriteWorkspace()
        throws MojoExecutionException
    {
        File workspaceFile = new File( project.getBasedir(), project.getArtifactId() + ".iws" );
        if ( !workspaceFile.exists() )
        {
            FileWriter w = null;
            try
            {
                w = new FileWriter( workspaceFile );
                IOUtil.copy( getClass().getResourceAsStream( "/templates/default/workspace.xml" ), w );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Unable to create workspace file", e );
            }
            finally
            {
                IOUtil.close( w );
            }

        }
    }

    private void rewriteProject()
        throws MojoExecutionException
    {
        try
        {
            File projectFile = new File( project.getBasedir(), project.getArtifactId() + ".ipr" );
            Reader reader;
            if ( projectFile.exists() )
            {
                reader = new FileReader( projectFile );
            }
            else
            {
                reader = new InputStreamReader( getClass().getResourceAsStream( "/templates/default/project.xml" ) );
            }

            Xpp3Dom module;
            try
            {
                module = Xpp3DomBuilder.build( reader );
            }
            finally
            {
                IOUtil.close( reader );
            }

            Xpp3Dom component = findComponent( module, "ProjectModuleManager" );
            Xpp3Dom modules = findElement( component, "modules" );

            if ( project.getCollectedProjects().size() > 0 )
            {
                removeOldElements( modules, "module" );

                for ( Iterator i = project.getCollectedProjects().iterator(); i.hasNext(); )
                {
                    MavenProject p = (MavenProject) i.next();

                    Xpp3Dom m = createElement( modules, "module" );
                    String modulePath = new File( p.getBasedir(), p.getArtifactId() + ".iml" ).getAbsolutePath();
                    m.setAttribute( "filepath", "$PROJECT_DIR$/" + toRelative( project.getBasedir(), modulePath ) );
                }
            }
            else
            {
                Xpp3Dom m = createElement( modules, "module" );
                String modulePath = new File( project.getBasedir(), project.getArtifactId() + ".iml" ).getAbsolutePath();
                m.setAttribute( "filepath", "$PROJECT_DIR$/" + toRelative( project.getBasedir(), modulePath ) );
            }

            FileWriter writer = new FileWriter( projectFile );
            try
            {
                Xpp3DomWriter.write( writer, module );
            }
            finally
            {
                IOUtil.close( writer );
            }
        }
        catch ( XmlPullParserException e )
        {
            throw new MojoExecutionException( "Error parsing existing IML file", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error parsing existing IML file", e );
        }
    }

    private void rewriteModule()
        throws MojoExecutionException
    {
        try
        {
            File moduleFile = new File( project.getBasedir(), project.getArtifactId() + ".iml" );
            Reader reader;
            if ( moduleFile.exists() )
            {
                reader = new FileReader( moduleFile );
            }
            else
            {
                reader = new InputStreamReader( getClass().getResourceAsStream( "/templates/default/module.xml" ) );
            }

            Xpp3Dom module;
            try
            {
                module = Xpp3DomBuilder.build( reader );
            }
            finally
            {
                IOUtil.close( reader );
            }

            // TODO: how can we let the WAR/EJBs plugin hook in and provide this?
            // TODO: merge in ejb-module, etc.
            if ( project.getPackaging().equals( "war" ) )
            {
                addWebModule( module );
            }
            else if ( project.getPackaging().equals( "ejb" ) )
            {
                module.setAttribute( "type", "J2EE_EJB_MODULE" );
            }

            Xpp3Dom component = findComponent( module, "NewModuleRootManager" );
            Xpp3Dom output = findElement( component, "output" );
            output.setAttribute( "url", getModuleFileUrl( project.getBuild().getOutputDirectory() ) );
            Xpp3Dom outputTest = findElement( component, "output-test" );
            outputTest.setAttribute( "url", getModuleFileUrl( project.getBuild().getTestOutputDirectory() ) );

            Xpp3Dom content = findElement( component, "content" );

            removeOldElements( content, "sourceFolder" );

            for ( Iterator i = executedProject.getCompileSourceRoots().iterator(); i.hasNext(); )
            {
                String directory = (String) i.next();
                addSourceFolder( content, directory, false );
            }
            for ( Iterator i = executedProject.getTestCompileSourceRoots().iterator(); i.hasNext(); )
            {
                String directory = (String) i.next();
                addSourceFolder( content, directory, true );
            }

            for ( Iterator i = project.getBuild().getResources().iterator(); i.hasNext(); )
            {
                Resource resource = (Resource) i.next();
                String directory = resource.getDirectory();
                addSourceFolder( content, directory, false );
            }

            for ( Iterator i = project.getBuild().getTestResources().iterator(); i.hasNext(); )
            {
                Resource resource = (Resource) i.next();
                String directory = resource.getDirectory();
                addSourceFolder( content, directory, true );
            }

            removeOldDependencies( component );

            // Must loop artifacts, not dependencies to resolve transitivity
            for ( Iterator i = project.getArtifacts().iterator(); i.hasNext(); )
            {
                Artifact a = (Artifact) i.next();
                // TODO: resolve projects in reactor as references

                Xpp3Dom dep = createElement( component, "orderEntry" );
                dep.setAttribute( "type", "module-library" );

                dep = createElement( dep, "library" );
                dep.setAttribute( "name", a.getArtifactId() );

                Xpp3Dom el = createElement( dep, "CLASSES" );
                el = createElement( el, "root" );
                el.setAttribute( "url", "jar://" + a.getFile().getAbsolutePath().replace( '\\', '/' ) + "!/" );

                createElement( dep, "JAVADOC" );
                createElement( dep, "SOURCES" );
            }

            FileWriter writer = new FileWriter( moduleFile );
            try
            {
                Xpp3DomWriter.write( writer, module );
            }
            finally
            {
                IOUtil.close( writer );
            }
        }
        catch ( XmlPullParserException e )
        {
            throw new MojoExecutionException( "Error parsing existing IML file", e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Error parsing existing IML file", e );
        }
    }

    private void addWebModule( Xpp3Dom module )
    {
        // TODO: this is bad - reproducing war plugin defaults, etc!
        //   --> this is where the OGNL out of a plugin would be helpful as we could run package first and
        //       grab stuff from the mojo

/*
Can't run this anyway as Xpp3Dom is in both classloaders...
                Xpp3Dom configuration = project.getGoalConfiguration( "maven-war-plugin", "war" );
                String warWebapp = configuration.getChild( "webappDirectory" ).getValue();
                if ( warWebapp == null )
                {
                    warWebapp = project.getBuild().getDirectory() + "/" + project.getArtifactId();
                }
                String warSrc = configuration.getChild( "warSrc" ).getValue();
                if ( warSrc == null )
                {
                    warSrc = "src/main/webapp";
                }
                String webXml = configuration.getChild( "webXml" ).getValue();
                if ( webXml == null )
                {
                    webXml = warSrc + "/WEB-INF/web.xml";
                }
*/
        String warWebapp = project.getBuild().getDirectory() + "/" + project.getArtifactId();
        String warSrc = "src/main/webapp";
        String webXml = warSrc + "/WEB-INF/web.xml";

        module.setAttribute( "type", "J2EE_WEB_MODULE" );

        Xpp3Dom component = findComponent( module, "WebModuleBuildComponent" );
        Xpp3Dom setting = findSetting( component, "EXPLODED_URL" );
        setting.setAttribute( "value", getModuleFileUrl( warWebapp ) );

        component = findComponent( module, "WebModuleProperties" );
        Xpp3Dom element = findElement( component, "deploymentDescriptor" );
        if ( element.getAttribute( "version" ) == null )
        {
            // TODO: should derive from web.xml - does IDEA do this if omitted?
//                    element.setAttribute( "version", "2.3" );
        }
        if ( element.getAttribute( "name" ) == null )
        {
            element.setAttribute( "name", "web.xml" );
        }

        element.setAttribute( "url", getModuleFileUrl( webXml ) );

        element = findElement( component, "webroots" );
        removeOldElements( element, "root" );

        element = createElement( element, "root" );
        element.setAttribute( "relative", "/" );
        element.setAttribute( "url", getModuleFileUrl( warSrc ) );
    }

    private void addSourceFolder( Xpp3Dom content, String directory, boolean isTest )
    {
        if ( !StringUtils.isEmpty( directory ) && new File( directory ).isDirectory() )
        {
            Xpp3Dom sourceFolder = createElement( content, "sourceFolder" );
            sourceFolder.setAttribute( "url", getModuleFileUrl( directory ) );
            sourceFolder.setAttribute( "isTestSource", Boolean.toString( isTest ) );
        }
    }

    // TODO: to FileUtils

    private static String toRelative( File basedir, String absolutePath )
    {
        String relative;

        if ( absolutePath.startsWith( basedir.getAbsolutePath() ) )
        {
            relative = absolutePath.substring( basedir.getAbsolutePath().length() + 1 );
        }
        else
        {
            relative = absolutePath;
        }

        relative = StringUtils.replace( relative, "\\", "/" );

        return relative;
    }

    private String getModuleFileUrl( String file )
    {
        return "file://$MODULE_DIR$/" + toRelative( project.getBasedir(), file );
    }

    // TODO: some xpath may actually be more appropriate here

    private void removeOldElements( Xpp3Dom content, String name )
    {
        Xpp3Dom[] children = content.getChildren();
        for ( int i = children.length - 1; i >= 0; i-- )
        {
            Xpp3Dom child = children[i];
            if ( child.getName().equals( name ) )
            {
                content.removeChild( i );
            }
        }
    }

    private void removeOldDependencies( Xpp3Dom component )
    {
        Xpp3Dom[] children = component.getChildren();
        for ( int i = children.length - 1; i >= 0; i-- )
        {
            Xpp3Dom child = children[i];
            if ( child.getName().equals( "orderEntry" ) && child.getAttribute( "type" ).equals( "module-library" ) )
            {
                component.removeChild( i );
            }
        }
    }

    private Xpp3Dom findComponent( Xpp3Dom module, String name )
    {
        Xpp3Dom[] components = module.getChildren( "component" );
        for ( int i = 0; i < components.length; i++ )
        {
            if ( name.equals( components[i].getAttribute( "name" ) ) )
            {
                return components[i];
            }
        }

        Xpp3Dom component = createElement( module, "component" );
        component.setAttribute( "name", name );
        return component;
    }

    private Xpp3Dom findSetting( Xpp3Dom component, String name )
    {
        Xpp3Dom[] settings = component.getChildren( "setting" );
        for ( int i = 0; i < settings.length; i++ )
        {
            if ( name.equals( settings[i].getAttribute( "name" ) ) )
            {
                return settings[i];
            }
        }

        Xpp3Dom setting = createElement( component, "setting" );
        setting.setAttribute( "name", name );
        return setting;
    }

    private static Xpp3Dom createElement( Xpp3Dom module, String name )
    {
        Xpp3Dom component = new Xpp3Dom( name );
        module.addChild( component );
        return component;
    }

    private Xpp3Dom findElement( Xpp3Dom component, String name )
    {
        Xpp3Dom element = component.getChild( name );

        if ( element == null )
        {
            element = createElement( component, name );
        }
        return element;
    }
}
