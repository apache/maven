package org.apache.maven.reports.projectinfo;

/*
 * Copyright 2004-2005 The Apache Software Foundation.
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

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;

import java.io.IOException;
import java.util.Iterator;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id: DependenciesReport.java,v 1.2 2005/02/23 00:08:02 brett Exp $
 * @plexus.component
 */
public class DependenciesReport
    extends AbstractMavenReport
{
    public void execute()
        throws MavenReportException
    {
        try
        {
            DependenciesRenderer r = new DependenciesRenderer( getSink(), getConfiguration().getModel() );

            r.render();
        }
        catch( IOException e )
        {
            throw new MavenReportException( "Can't write the report " + getOutputName(), e );
        }
    }

    public String getOutputName()
    {
        return "dependencies";
    }

    static class DependenciesRenderer
        extends AbstractMavenReportRenderer
    {
        private Model model;

        public DependenciesRenderer( Sink sink, Model model )
        {
            super( sink );

            this.model = model;
        }

        // How to i18n these ...
        public String getTitle()
        {
            return "Project Dependencies";
        }

        public void renderBody()
        {
            startSection( getTitle() );

            startTable();

            tableCaption( "Declared Dependencies" );

            tableHeader( new String[]{"GroupId", "ArtifactId", "Version"} );

            for ( Iterator i = model.getDependencies().iterator(); i.hasNext(); )
            {
                Dependency d = (Dependency) i.next();

                tableRow( new String[]{d.getGroupId(), d.getArtifactId(), d.getVersion()} );
            }

            endTable();

            endSection();
        }

    }
}
