package org.apache.maven.reporting;

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

import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;

import java.io.File;
import java.util.List;

/**
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id: MavenReport.java 163376 2005-02-23 00:06:06Z brett $
 */
public class MavenReportConfiguration
{
    private Model model;

    private File outputDirectory;

    private File basedir;

    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    public void setOutputDirectory( File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    public File getBasedir()
    {
        return basedir;
    }

    public void setBasedir( File basedir )
    {
        this.basedir = basedir;
    }

    public Model getModel()
    {
        if ( model == null )
        {
            model = new Model();
        }

        return model;
    }

    public void setModel( Model model )
    {
        this.model = model;
    }

    public List getDependencies()
    {
        return getModel().getDependencies();
    }

    public List getMailingLists()
    {
        return getModel().getMailingLists();
    }

    public Scm getScm()
    {
        return getModel().getScm();
    }

    public void setScm( Scm scm )
    {
        getModel().setScm( scm );
    }

    public String getSourceDirectory()
    {
        return getModel().getBuild().getSourceDirectory();
    }

    public void setSourceDirectory( String sourceDirectory )
    {
        getModel().getBuild().setSourceDirectory( sourceDirectory );
    }

    public String getScriptSourceDirectory()
    {
        return getModel().getBuild().getScriptSourceDirectory();
    }

    public void setScriptSourceDirectory( String scriptSourceDirectory )
    {
        getModel().getBuild().setScriptSourceDirectory( scriptSourceDirectory );
    }

    public String getTestSourceDirectory()
    {
        return getModel().getBuild().getTestSourceDirectory();
    }

    public void setTestSourceDirectory( String testSourceDirectory )
    {
        getModel().getBuild().setTestSourceDirectory( testSourceDirectory );
    }
}