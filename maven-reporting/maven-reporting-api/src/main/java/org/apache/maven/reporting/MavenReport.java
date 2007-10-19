package org.apache.maven.reporting;

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

import org.codehaus.doxia.sink.Sink;

import java.io.File;
import java.util.Locale;

/**
 * The basis for a Maven report.
 *
 * @author Brett Porter
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id$
 * @since 2.0
 */
public interface MavenReport
{
    /** Plexus lookup name */
    String ROLE = MavenReport.class.getName();

    /** Category for project information reports */
    String CATEGORY_PROJECT_INFORMATION = "Project Info";

    /** Category for project reports */
    String CATEGORY_PROJECT_REPORTS = "Project Reports";

    /**
     * Generate the report depending the wanted locale.
     * <br/>
     * Mainly used for external reports like javadoc.
     *
     * @param sink the sink to use for the generation.
     * @param locale the wanted locale to generate the report, could be null.
     * @throws MavenReportException if any
     */
    void generate( Sink sink, Locale locale )
        throws MavenReportException;

    /**
     * @return the output name of this report.
     */
    String getOutputName();

    /**
     * Get the category name for this report.
     *
     * @return the category name of this report. Should be <code>CATEGORY_PROJECT_INFORMATION</code>
     * or <code>CATEGORY_PROJECT_REPORTS</code>
     * {@link #CATEGORY_PROJECT_INFORMATION}
     * {@link #CATEGORY_PROJECT_REPORTS}
     */
    String getCategoryName();

    /**
     * @param locale the wanted locale to return the report's name, could be null.
     * @return the name of this report.
     */
    String getName( Locale locale );

    /**
     * @param locale the wanted locale to return the report's description, could be null.
     * @return the description of this report.
     */
    String getDescription( Locale locale );

    /**
     * Set a new output directory. Useful for staging.
     *
     * @param outputDirectory the new output directory
     */
    void setReportOutputDirectory( File outputDirectory );

    /**
     * @return the current report output directory.
     */
    File getReportOutputDirectory();

    /**
     * An external report is a report which calls a third party program which generates some reports too.
     * A good example is javadoc tool.
     *
     * @return <tt>true</tt> if this report is external, <tt>false</tt> otherwise.
     * Default should <tt>false</tt>.
     */
    boolean isExternalReport();

    /**
     * Verify some conditions before generate the report.
     *
     * @return <tt>true</tt> if this report could be generated, <tt>false</tt> otherwise.
     * Default should <tt>true</tt>.
     */
    boolean canGenerateReport();
}
