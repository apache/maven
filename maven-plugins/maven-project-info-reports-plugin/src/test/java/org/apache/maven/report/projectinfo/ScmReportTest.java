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
import com.meterware.httpunit.TextBlock;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

/**
 * Test the <code>Scm Report</code> generation for defined projects in the <code>PROJECTS_DIR</code> directory.
 * <p>Testing only section title and links with <a href="http://www.httpunit.org/">HTTPUnit</a> framework.</p>
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id $
 */
public class ScmReportTest
    extends AbstractHttpUnitReportTestCase
{
    private static final String CLEARCASE_PROJECT = "project-info-reports-plugin-scm-ClearCase";

    private static final String CVS_PROJECT = "project-info-reports-plugin-scm-CVS";

    private static final String PERFORCE_PROJECT = "project-info-reports-plugin-scm-Perforce";

    private static final String STARTEAM_PROJECT = "project-info-reports-plugin-scm-Starteam";

    private static final String SVN_PROJECT = "project-info-reports-plugin-scm-SVN";

    private static final String UNKNOWN_PROJECT = "project-info-reports-plugin-scm-unknown";

    /** WebConversation object */
    private static final WebConversation webConversation = new WebConversation();

    /**
     * @see org.apache.maven.report.projectinfo.AbstractMavenReportTestCase#getReportName()
     */
    protected String getReportName()
    {
        return "source-repository.html";
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
            loadTestMavenProject( CLEARCASE_PROJECT );

            assertNotNull( getTestMavenProject() );
            assertNotNull( getTestMavenProject().getScm() );

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
            assertEquals( getString( "report.scm.title" ), response.getTitle() );

            // Test the sections
            TextBlock[] textBlocks = response.getTextBlocks();

            assertEquals( textBlocks.length, 8 );

            assertEquals( getString( "report.scm.overview.title" ), textBlocks[1].getText() );
            assertEquals( getString( "report.scm.webaccess.title" ), textBlocks[2].getText() );
            assertEquals( getString( "report.scm.webaccess.url" ), textBlocks[3].getText() );
            assertEquals( getString( "report.scm.devaccess.title" ), textBlocks[4].getText() );
            assertEquals( getString( "report.scm.devaccess.clearcase.intro" ), textBlocks[5].getText() );
            assertEquals( getString( "report.scm.accessbehindfirewall.title" ), textBlocks[6].getText() );
            assertEquals( getString( "report.scm.accessbehindfirewall.general.intro" ), textBlocks[7].getText() );

            testLinks( response );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            assertFalse( true );
        }
    }

    /**
     * Test a the <code>CVS</code> SCM report
     */
    public void testCVSScmReport()
    {
        if ( skip )
        {
            return;
        }

        try
        {
            loadTestMavenProject( CVS_PROJECT );

            assertNotNull( getTestMavenProject() );
            assertNotNull( getTestMavenProject().getScm() );

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
            assertEquals( getString( "report.scm.title" ), response.getTitle() );

            // Test the sections
            TextBlock[] textBlocks = response.getTextBlocks();

            assertEquals( textBlocks.length, 9 );

            assertEquals( getString( "report.scm.overview.title" ), textBlocks[1].getText() );
            assertEquals( getString( "report.scm.webaccess.title" ), textBlocks[2].getText() );
            assertEquals( getString( "report.scm.webaccess.url" ), textBlocks[3].getText() );
            assertEquals( getString( "report.scm.anonymousaccess.title" ), textBlocks[4].getText() );
            assertEquals( getString( "report.scm.anonymousaccess.cvs.intro" ), textBlocks[5].getText() );
            assertEquals( getString( "report.scm.devaccess.title" ), textBlocks[6].getText() );
            assertEquals( getString( "report.scm.devaccess.cvs.intro" ), textBlocks[7].getText() );
            assertEquals( getString( "report.scm.accessbehindfirewall.title" ), textBlocks[8].getText() );

            testLinks( response );
        }
        catch ( Exception e )
        {
            assertFalse( true );
        }
    }

    /**
     * Test a the <code>Perforce</code> SCM report
     */
    public void testPerforceScmReport()
    {
        if ( skip )
        {
            return;
        }

        try
        {
            loadTestMavenProject( PERFORCE_PROJECT );

            assertNotNull( getTestMavenProject() );
            assertNotNull( getTestMavenProject().getScm() );

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
            assertEquals( getString( "report.scm.title" ), response.getTitle() );

            // Test the sections
            TextBlock[] textBlocks = response.getTextBlocks();

            assertEquals( textBlocks.length, 8 );

            assertEquals( getString( "report.scm.overview.title" ), textBlocks[1].getText() );
            assertEquals( getString( "report.scm.webaccess.title" ), textBlocks[2].getText() );
            assertEquals( getString( "report.scm.webaccess.url" ), textBlocks[3].getText() );
            assertEquals( getString( "report.scm.devaccess.title" ), textBlocks[4].getText() );
            assertEquals( getString( "report.scm.devaccess.perforce.intro" ), textBlocks[5].getText() );
            assertEquals( getString( "report.scm.accessbehindfirewall.title" ), textBlocks[6].getText() );
            assertEquals( getString( "report.scm.accessbehindfirewall.general.intro" ), textBlocks[7].getText() );

            testLinks( response );
        }
        catch ( Exception e )
        {
            assertFalse( true );
        }
    }

    /**
     * Test a the <code>Starteam</code> SCM report
     */
    public void testStarteamScmReport()
    {
        if ( skip )
        {
            return;
        }

        try
        {
            loadTestMavenProject( STARTEAM_PROJECT );

            assertNotNull( getTestMavenProject() );
            assertNotNull( getTestMavenProject().getScm() );

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
            assertEquals( getString( "report.scm.title" ), response.getTitle() );

            // Test the sections
            TextBlock[] textBlocks = response.getTextBlocks();

            assertEquals( textBlocks.length, 8 );

            assertEquals( getString( "report.scm.overview.title" ), textBlocks[1].getText() );
            assertEquals( getString( "report.scm.webaccess.title" ), textBlocks[2].getText() );
            assertEquals( getString( "report.scm.webaccess.url" ), textBlocks[3].getText() );
            assertEquals( getString( "report.scm.devaccess.title" ), textBlocks[4].getText() );
            assertEquals( getString( "report.scm.devaccess.starteam.intro" ), textBlocks[5].getText() );
            assertEquals( getString( "report.scm.accessbehindfirewall.title" ), textBlocks[6].getText() );
            assertEquals( getString( "report.scm.accessbehindfirewall.general.intro" ), textBlocks[7].getText() );

            testLinks( response );
        }
        catch ( Exception e )
        {
            assertFalse( true );
        }
    }

    /**
     * Test a the <code>SVN</code> SCM report
     */
    public void testSVNScmReport()
    {
        if ( skip )
        {
            return;
        }

        try
        {
            loadTestMavenProject( SVN_PROJECT );

            assertNotNull( getTestMavenProject() );
            assertNotNull( getTestMavenProject().getScm() );

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
            assertEquals( getString( "report.scm.title" ), response.getTitle() );

            // Test the sections
            TextBlock[] textBlocks = response.getTextBlocks();

            assertEquals( textBlocks.length, 15 );

            assertEquals( getString( "report.scm.overview.title" ), textBlocks[1].getText() );
            assertEquals( getString( "report.scm.webaccess.title" ), textBlocks[2].getText() );
            assertEquals( getString( "report.scm.webaccess.url" ), textBlocks[3].getText() );
            assertEquals( getString( "report.scm.anonymousaccess.title" ), textBlocks[4].getText() );
            assertEquals( getString( "report.scm.anonymousaccess.svn.intro" ), textBlocks[5].getText() );
            assertEquals( getString( "report.scm.devaccess.title" ), textBlocks[6].getText() );
            assertEquals( getString( "report.scm.devaccess.svn.intro1" ), textBlocks[7].getText() );
            assertEquals( getString( "report.scm.devaccess.svn.intro2" ), textBlocks[8].getText() );
            assertEquals( getString( "report.scm.accessbehindfirewall.title" ), textBlocks[9].getText() );
            assertEquals( getString( "report.scm.accessbehindfirewall.svn.intro" ), textBlocks[10].getText() );
            assertEquals( getString( "report.scm.accessthroughtproxy.title" ), textBlocks[11].getText() );
            assertEquals( getString( "report.scm.accessthroughtproxy.svn.intro1" ), textBlocks[12].getText() );
            assertEquals( getString( "report.scm.accessthroughtproxy.svn.intro2" ), textBlocks[13].getText() );
            assertEquals( getString( "report.scm.accessthroughtproxy.svn.intro3" ), textBlocks[14].getText() );

            testLinks( response );
        }
        catch ( Exception e )
        {
            assertFalse( true );
        }
    }

    /**
     * Test a the <code>unknown</code> SCM report
     */
    public void testUnknownScmReport()
    {
        if ( skip )
        {
            return;
        }

        try
        {
            loadTestMavenProject( UNKNOWN_PROJECT );

            assertNotNull( getTestMavenProject() );
            assertNotNull( getTestMavenProject().getScm() );

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
            assertEquals( getString( "report.scm.title" ), response.getTitle() );

            // Test the sections
            TextBlock[] textBlocks = response.getTextBlocks();

            assertEquals( textBlocks.length, 11 );

            assertEquals( getString( "report.scm.overview.title" ), textBlocks[1].getText() );
            assertEquals( getString( "report.scm.general.intro" ), textBlocks[2].getText() );
            assertEquals( getString( "report.scm.webaccess.title" ), textBlocks[3].getText() );
            assertEquals( getString( "report.scm.webaccess.url" ), textBlocks[4].getText() );
            assertEquals( getString( "report.scm.anonymousaccess.title" ), textBlocks[5].getText() );
            assertEquals( getString( "report.scm.anonymousaccess.general.intro" ), textBlocks[6].getText() );
            assertEquals( getString( "report.scm.devaccess.title" ), textBlocks[7].getText() );
            assertEquals( getString( "report.scm.devaccess.general.intro" ), textBlocks[8].getText() );
            assertEquals( getString( "report.scm.accessbehindfirewall.title" ), textBlocks[9].getText() );
            assertEquals( getString( "report.scm.accessbehindfirewall.general.intro" ), textBlocks[10].getText() );

            testLinks( response );
        }
        catch ( Exception e )
        {
            assertFalse( true );
        }
    }
}
