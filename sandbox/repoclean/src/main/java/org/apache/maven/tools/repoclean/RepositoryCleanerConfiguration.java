package org.apache.maven.tools.repoclean;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

/**
 * @author jdcasey
 */
public class RepositoryCleanerConfiguration
{

    private String sourceRepositoryPath;

    private String sourceRepositoryLayout;

    private String sourcePomVersion;

    private String targetRepositoryPath;

    private String targetRepositoryLayout;

    private String reportsPath;

    private boolean reportOnly;

    public void setSourceRepositoryPath( String sourceRepositoryPath )
    {
        this.sourceRepositoryPath = sourceRepositoryPath;
    }

    public String getSourceRepositoryPath()
    {
        return sourceRepositoryPath;
    }

    public void setSourceRepositoryLayout( String sourceRepositoryLayout )
    {
        this.sourceRepositoryLayout = sourceRepositoryLayout;
    }

    public String getSourceRepositoryLayout()
    {
        return sourceRepositoryLayout;
    }

    public void setSourcePomVersion( String sourcePomVersion )
    {
        this.sourcePomVersion = sourcePomVersion;
    }

    public String getSourcePomVersion()
    {
        return sourcePomVersion;
    }

    public void setTargetRepositoryPath( String targetRepositoryPath )
    {
        this.targetRepositoryPath = targetRepositoryPath;
    }

    public String getTargetRepositoryPath()
    {
        return targetRepositoryPath;
    }

    public void setTargetRepositoryLayout( String targetRepositoryLayout )
    {
        this.targetRepositoryLayout = targetRepositoryLayout;
    }

    public String getTargetRepositoryLayout()
    {
        return targetRepositoryLayout;
    }

    public void setReportsPath( String reportsPath )
    {
        this.reportsPath = reportsPath;
    }

    public String getReportsPath()
    {
        return reportsPath;
    }

    public void setReportOnly( boolean reportOnly )
    {
        this.reportOnly = reportOnly;
    }

    public boolean reportOnly()
    {
        return reportOnly;
    }

}