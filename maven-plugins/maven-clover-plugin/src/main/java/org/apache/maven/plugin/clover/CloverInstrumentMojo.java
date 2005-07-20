/*
 * Copyright 2005 The Apache Software Foundation.
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
package org.apache.maven.plugin.clover;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;

import com.cenqua.clover.CloverInstr;

/**
 * @goal instrument
 * @phase generate-sources
 * @requiresDependencyResolution test
 * @description Instrument source roots
 * 
 * @author <a href="mailto:vmassol@apache.org">Vincent Massol</a>
 * @version $Id$
 */
public class CloverInstrumentMojo extends AbstractCloverMojo
{
    /**
     * @parameter
     * @required
     */
	private String cloverOutputDirectory;

    /**
     * @parameter
     * @required
     */
	private String cloverDatabase;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter expression="${plugin.artifacts}"
     * @required
     */
    private List pluginArtifacts;

    /**
     * @parameter expression="${component.org.apache.maven.artifact.factory.ArtifactFactory}"
     * @required
     * @readonly
     */
    private ArtifactFactory factory;

    private String cloverOutputSourceDirectory;

    private void init()
    {
        new File(this.cloverOutputDirectory).mkdirs();

        this.cloverOutputSourceDirectory = new File(this.cloverOutputDirectory, "src").getPath();
    }

    public void execute() throws MojoExecutionException
    {
        init();

        registerLicenseFile();
        
        int result = CloverInstr.mainImpl(createCliArgs());
	    if (result != 0)
		{
			throw new MojoExecutionException("Clover has failed to instrument the source files");
		}

	    addGeneratedSourcesToCompileRoots();
//	    addCloverDependencyToCompileClasspath();

        // Explicitely set the output directory to be the Clover one so that all other plugins executing
        // thereafter output files in the Clover output directory and not in the main output directory.
        // TODO: Ulgy hack below. Changing the directory should be enough for changing the values of all other
        // properties depending on it!
        this.project.getBuild().setDirectory(this.cloverOutputDirectory);
        this.project.getBuild().setOutputDirectory(new File(this.cloverOutputDirectory, "classes").getPath());
        this.project.getBuild().setTestOutputDirectory(new File(this.cloverOutputDirectory, "test-classes").getPath());
    }

    /**
     * @todo handle multiple source roots. At the moment only the first source root is instrumented
     */
    private void addGeneratedSourcesToCompileRoots()
    {
        this.project.getCompileSourceRoots().remove(0);
        this.project.addCompileSourceRoot(this.cloverOutputSourceDirectory);
    }
/*
    private void addCloverDependencyToCompileClasspath()
    {
        Artifact cloverArtifact = null;
        Iterator artifacts = this.pluginArtifacts.iterator();
        while (artifacts.hasNext())
        {
            Artifact artifact = (Artifact) artifacts.next();
            if (artifact.getArtifactId().equalsIgnoreCase("clover"))
            {
                cloverArtifact = artifact;
                break;
            }
        }

        List artifactsToAdd = new ArrayList();
        artifactsToAdd.add(cloverArtifact);
        
        this.project.addArtifacts(artifactsToAdd, this.factory); 
    }
*/

	/**
	 * @return the CLI args to be passed to CloverInstr
	 * @todo handle multiple source roots. At the moment only the first source root is instrumented
	 */
	private String[] createCliArgs()
	{
		String [] cliArgs = {

            // TODO: Temporary while we wait for surefire to be able to fork unit tests. See
            // http://jira.codehaus.org/browse/MNG-441
            "-p", "threaded",
            "-f", "100",
            
            "-i", this.cloverDatabase, 
            "-s", (String) this.project.getCompileSourceRoots().get(0),
            "-d", this.cloverOutputSourceDirectory };

		return cliArgs; 
	}
}
