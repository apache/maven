package org.apache.maven.report.projectinfo;

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

import java.net.URL;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.WebTable;

/**
 * Test the <code>Scm Report</code> generation for defined projects in the <code>PROJECTS_DIR</code> directory.
 * <p>Testing only section title and links with <a href="http://www.httpunit.org/">HTTPUnit</a> framework.</p>
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id $
 */
public class DependenciesReportTest
    extends AbstractHttpUnitReportTestCase
{
    private static final String TEST1 = "project-info-reports-plugin-test1";

    /** WebConversation object */
    private static final WebConversation webConversation = new WebConversation();

    /**
     * @see org.apache.maven.report.projectinfo.AbstractMavenReportTestCase#getReportName()
     */
    protected String getReportName()
    {
        return "dependencies.html";
    }

    /**
     * Test a the <code>ClearCase</code> SCM report
     */
    public void testClearCaseScmReport()
    {
        if ( skip )
        {
            return;
        }

        try
        {
            loadTestMavenProject( TEST1 );

            assertNotNull( getTestMavenProject() );
            assertNotNull( getTestMavenProject().getDependencies() );

            executeMaven2CommandLine();

            URL reportURL = getGeneratedReport().toURL();
            assertNotNull( reportURL );

            // HTTPUnit
            WebRequest request = new GetMethodWebRequest( reportURL.toString() );
            WebResponse response = webConversation.getResponse( request );

            // Basic HTML tests
            assertTrue( response.isHTML() );
            assertTrue( response.getContentLength() > 0 );

            // Test the Page title
            assertEquals( getString( "report.dependencies.title" ), response.getTitle() );

            // Test the tables
            WebTable[] webTables = response.getTables();
            assertEquals( webTables.length, 2 );

            assertEquals( webTables[0].getColumnCount(), 5);
            assertEquals( webTables[0].getRowCount(), 1 + getTestMavenProject().getDependencies().size());

            assertEquals( webTables[1].getColumnCount(), 5);

            testLinks( response );
        }
        catch ( Exception e )
        {
            assertFalse( true );
        }
    }
}
