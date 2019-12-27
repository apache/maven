package org.apache.maven.plugin.coreit;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;

import org.codehaus.doxia.sink.Sink;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Properties;

/**
 * Creates a properties file in the site output directory.
 * 
 * @goal info
 * 
 * @author Benjamin Bentmann
 *
 */
public class InfoReport
    extends AbstractMojo
    implements MavenReport
{

    /**
     * The base directory of the current Maven project.
     * 
     * @parameter default-value="${basedir}"
     * @required
     * @readonly
     */
    private File basedir;

    /**
     * The path to the properties file, relative to the output directory of the site. The keys
     * <code>locale.language</code>, <code>locale.country</code> and <code>locale.variant</code> indicate the report's
     * locale.
     * 
     * @parameter default-value="info.properties"
     */
    private String infoFile = "info.properties";

    /**
     * The path to the output directory of the site.
     * 
     * @parameter default-value="${project.reporting.outputDirectory}"
     */
    private File outputDirectory;

    /**
     * The locale for the report.
     */
    private Locale locale;

    /**
     * Runs this mojo.
     * 
     * @throws MojoExecutionException If the output file could not be created.
     * @throws MojoFailureException If the output file has not been set.
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "[MAVEN-CORE-IT-LOG] Using output file path: " + infoFile );

        if ( infoFile == null || infoFile.length() <= 0 )
        {
            throw new MojoFailureException( "Path name for output file has not been specified" );
        }

        File outputFile = new File( outputDirectory, infoFile );
        if ( !outputFile.isAbsolute() )
        {
            outputFile = new File( new File( basedir, outputDirectory.getPath() ), infoFile ).getAbsoluteFile();
        }

        Properties props = new Properties();
        props.setProperty( "site.output.directory", outputDirectory.getPath() );
        if ( locale != null )
        {
            props.setProperty( "locale.language", locale.getLanguage() );
            props.setProperty( "locale.country", locale.getCountry() );
            props.setProperty( "locale.variant", locale.getVariant() );
        }

        getLog().info( "[MAVEN-CORE-IT-LOG] Creating output file: " + outputFile );

        OutputStream out = null;
        try
        {
            outputFile.getParentFile().mkdirs();
            out = new FileOutputStream( outputFile );
            props.store( out, "MAVEN-CORE-IT-LOG" );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Output file could not be created: " + outputFile, e );
        }
        finally
        {
            if ( out != null )
            {
                try
                {
                    out.close();
                }
                catch ( IOException e )
                {
                    // just ignore
                }
            }
        }

        getLog().info( "[MAVEN-CORE-IT-LOG] Created output file: " + outputFile );
    }

    /**
     * Runs this report.
     * 
     * @throws MavenReportException If the report could not be created.
     */
    public void generate( Sink sink, Locale locale )
        throws MavenReportException
    {
        this.locale = locale;
        try
        {
            execute();
        }
        catch ( Exception e )
        {
            throw new MavenReportException( "Report could not be created", e );
        }
    }

    public String getOutputName()
    {
        return "info";
    }

    public String getCategoryName()
    {
        return "Project Reports";
    }

    public String getName( Locale locale )
    {
        return "name";
    }

    public String getDescription( Locale locale )
    {
        return "description";
    }

    public void setReportOutputDirectory( File outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    public File getReportOutputDirectory()
    {
        return outputDirectory;
    }

    public boolean isExternalReport()
    {
        return true;
    }

    public boolean canGenerateReport()
    {
        return true;
    }

}
