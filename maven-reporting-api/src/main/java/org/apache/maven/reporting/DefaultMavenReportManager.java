package org.apache.maven.reporting;

import org.apache.maven.model.Model;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;

import java.util.Map;

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

/**
 * Manage the set of available reports.
 *
 * @author Brett Porter
 * @version $Id$
 * @plexus.component
 */
public class DefaultMavenReportManager
    implements MavenReportManager
{
    private Map mavenReports;

    /**
     * @todo we need some type of response
     * @todo don't want to be passing in model - use report configuration
     */
    public void executeReport( String name, Model model, SiteRenderer siteRenderer, String outputDirectory )
        throws Exception
    {
        MavenReport report = (MavenReport) mavenReports.get( name );

        Sink sink = siteRenderer.createSink( outputDirectory, report.getOutputName() );
        report.execute( model, sink );
    }
}
