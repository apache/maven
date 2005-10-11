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

import org.xml.sax.SAXException;

import com.meterware.httpunit.WebLink;
import com.meterware.httpunit.WebResponse;

/**
 * Abstract class to test reports with <a href="http://www.httpunit.org/">HTTPUnit</a> framework.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id $
 */
public abstract class AbstractHttpUnitReportTestCase
    extends AbstractMavenReportTestCase
{
    /**
     * Test links in a generated report
     *
     * @param response HTTPUnit response object
     * @throws SAXException if any
     */
    protected void testLinks( WebResponse response )
        throws SAXException
    {
        // Test links
        WebLink[] links = response.getLinks();

        assertTrue( links.length > 9 );

        // Header
        assertEquals( getTestMavenProject().getUrl(), links[0].getURLString() );
        // NavBar
        assertEquals( "index.html", links[1].getURLString() );
        assertEquals( "project-info.html", links[2].getURLString() );
        assertEquals( "integration.html", links[3].getURLString() );
        assertEquals( "dependencies.html", links[4].getURLString() );
        assertEquals( "issue-tracking.html", links[5].getURLString() );
        assertEquals( "license.html", links[6].getURLString() );
        assertEquals( "mail-lists.html", links[7].getURLString() );
        assertEquals( "source-repository.html", links[8].getURLString() );
        assertEquals( "team-list.html", links[9].getURLString() );
        // Content skipped
    }
}
