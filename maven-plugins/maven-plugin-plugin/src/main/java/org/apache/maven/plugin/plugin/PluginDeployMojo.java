package org.apache.maven.plugin.plugin;

import java.io.File;

import org.apache.maven.plugin.AbstractPlugin;
import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;
import org.apache.maven.project.MavenProject;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.deployer.ArtifactDeployer;

import org.codehaus.plexus.util.FileUtils;

/*
 * LICENSE
 */

/**
 * @goal deploy
 *
 * @description Installs a plugin into local repository
 *
 * @prereq plugin:plugin
 *
 * @parameter
 *  name="outputDirectory"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#project.build.directory"
 *  description=""
 * 
 * @parameter
 *  name="jarName"
 *  type="String"
 *  required="true"
 *  validator=""
 *  expression="#maven.final.name"
 *  description=""
 *
 * @parameter
 *  name="project"
 *  type="org.apache.maven.project.MavenProject"
 *  required="true"
 *  validator=""
 *  expression="#project"
 *  description=""
 *
 * @parameter
 *  name="deployer"
 *  type="org.apache.maven.artifact.deployer.ArtifactDeployer"
 *  required="true"
 *  validator=""
 *  expression="#component.org.apache.maven.artifact.deployer.ArtifactDeployer"
 *  description=""
 *
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @author <a href="mailto:michal@codehaus.org">Michal Maczka</a>
 * @version $Id$
 */
public class PluginDeployMojo
    extends AbstractPluginMojo
{
    public void execute( PluginExecutionRequest request, PluginExecutionResponse response )
        throws Exception
    {

        File jarFile = getJarFile( request );

        MavenProject project = (MavenProject) request.getParameter( "project" );

        ArtifactDeployer artifactDeployer = ( ArtifactDeployer ) request.getParameter( "deployer" );

        artifactDeployer.deploy( jarFile, "plugin", project );

    }


}
