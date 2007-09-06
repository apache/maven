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
 * @version $Id$
 */
public interface MavenReport
{
    String ROLE = MavenReport.class.getName();

    /** @deprecated For removal in Maven 3.0 or when reporting-api is decoupled from the core, as categories are dynamic. */
    String CATEGORY_PROJECT_INFORMATION = "Project Info";

    /** @deprecated For removal in Maven 3.0 or when reporting-api is decoupled from the core, as categories are dynamic. */
    String CATEGORY_PROJECT_REPORTS = "Project Reports";

    // eventually, we must replace this with the o.a.m.d.s.Sink class as a parameter
    void generate( Sink sink, Locale locale )
        throws MavenReportException;

    String getOutputName();

    String getName( Locale locale );

    String getCategoryName();

    String getDescription( Locale locale );

    // TODO: remove?
    void setReportOutputDirectory( File outputDirectory );

    File getReportOutputDirectory();

    boolean isExternalReport();

    boolean canGenerateReport();
}
