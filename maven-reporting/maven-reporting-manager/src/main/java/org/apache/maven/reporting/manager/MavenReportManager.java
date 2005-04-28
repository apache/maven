package org.apache.maven.reporting.manager;

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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Reports;
import org.apache.maven.reporting.MavenReportConfiguration;
import org.apache.maven.reporting.MavenReportException;

import java.util.List;

/**
 * Manage the set of available reports.
 *
 * @author Brett Porter
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id: MavenReportManager.java 163376 2005-02-23 00:06:06Z brett $
 */
public interface MavenReportManager
{
    String ROLE = MavenReportManager.class.getName();

    void addReports( Reports reports, ArtifactRepository localRepository, List remoteRepositories )
        throws ReportManagerException, ReportNotFoundException;

    void executeReport( String name, MavenReportConfiguration config, String outputDirectory )
        throws MavenReportException;
}
