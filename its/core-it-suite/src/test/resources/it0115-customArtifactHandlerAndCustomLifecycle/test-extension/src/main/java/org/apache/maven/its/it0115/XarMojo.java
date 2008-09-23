package org.apache.maven.its.it0115;

/*
 * Copyright 2007 The Apache Software Foundation.
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

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

/**
 * Gather all resources in a XAR file (which is actually a ZIP file)
 *
 * @version $Id: $
 * @goal xar
 * @phase package
 */
public class XarMojo extends AbstractMojo
{
    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /** 
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if (this.project.getResources().size() < 1)
        {
            this.getLog().warn("No XAR created as no resources were found");
            return;
        }

        try
        {
            performArchive();
        }
        catch (Exception e)
        {
            throw new MojoExecutionException("Error while creating XAR file", e );
        }
    }

    private void performArchive() throws ArchiverException, IOException
    {
        File xarFile = new File(this.project.getBuild().getDirectory(),
            this.project.getArtifactId() + ".xar" );
        ZipArchiver archiver = new ZipArchiver();
        archiver.setDestFile(xarFile);
        archiver.setIncludeEmptyDirs(false);
        archiver.setCompress(true);
        archiver.addDirectory(new File(this.project.getBuild().getOutputDirectory()));
        archiver.createArchive();
        this.project.getArtifact().setFile(xarFile);
    }
}
