package org.apache.maven.project.helpers;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import junit.framework.TestCase;
import org.apache.maven.model.Build;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Developer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Resource;
import org.apache.maven.model.Site;
import org.apache.maven.model.UnitTest;
import org.apache.maven.project.MavenProject;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class ProjectTestHelper
    extends TestCase
{
    public static void testProjectFieldRetrieval( MavenProject project )
        throws Exception
    {
        // ----------------------------------------------------------------------
        // Top-level elements
        // ----------------------------------------------------------------------

        assertEquals( "4.0.0", project.getModelVersion() );

        assertEquals( "maven", project.getGroupId() );

        assertEquals( "Maven", project.getName() );

        assertEquals( "1.0-beta-9", project.getVersion() );

        assertEquals( "2001", project.getInceptionYear() );

        //assertEquals( "org.apache.maven", project.getPackage() );

        assertEquals( "Description", project.getDescription() );

        assertEquals( "shortDescription", project.getShortDescription() );

        assertEquals( "http://maven.apache.org/", project.getUrl() );



        // ----------------------------------------------------------------------
        // Distribution
        // ----------------------------------------------------------------------

        DistributionManagement distributionManagement = project.getDistributionManagement();

        assertNotNull( distributionManagement   );

        Site site = distributionManagement.getSite();

        assertNotNull( site  );

        Repository repository = distributionManagement.getRepository();

        assertNotNull( repository  );



        // ----------------------------------------------------------------------
        // Organization
        // ----------------------------------------------------------------------

        assertEquals( "Apache Software Foundation", project.getOrganization().getName() );

        assertEquals( "http://apache.org/", project.getOrganization().getUrl() );

        assertEquals( "/images/jakarta-logo-blue.gif", project.getOrganization().getLogo() );

        // ----------------------------------------------------------------------
        // Repository
        // ----------------------------------------------------------------------

        assertEquals( "anon-connection", project.getScm().getConnection() );

        assertEquals( "developer-connection", project.getScm().getDeveloperConnection() );

        assertEquals( "repository-url", project.getScm().getUrl() );

        // ----------------------------------------------------------------------
        // MailingLists
        // ----------------------------------------------------------------------

        MailingList ml = (MailingList) project.getMailingLists().get( 0 );

        assertEquals( "Maven User List", ml.getName() );

        assertEquals( "subscribe", ml.getSubscribe() );

        assertEquals( "unsubscribe", ml.getUnsubscribe() );

        assertEquals( "archive", ml.getArchive() );

        // ----------------------------------------------------------------------
        // Developers
        // ----------------------------------------------------------------------

        Developer d = (Developer) project.getDevelopers().get( 0 );

        assertEquals( "Jason van Zyl", d.getName() );

        assertEquals( "jvanzyl", d.getId() );

        assertEquals( "jason@maven.org", d.getEmail() );

        assertEquals( "Zenplex", d.getOrganization() );

        assertEquals( "Founder", (String) d.getRoles().get( 0 ) );

        // ----------------------------------------------------------------------
        // Contributors
        // ----------------------------------------------------------------------

        Contributor c = (Contributor) project.getContributors().get( 0 );

        assertEquals( "Martin van dem Bemt", c.getName() );

        assertEquals( "mvdb@mvdb.com", c.getEmail() );

        // ----------------------------------------------------------------------
        // Dependencies
        // ----------------------------------------------------------------------

        Dependency dep = (Dependency) project.getDependencies().get( 0 );

        assertEquals( "g1", dep.getGroupId() );

        assertEquals( "d1", dep.getArtifactId() );

        assertEquals( "1.0", dep.getVersion() );

        assertEquals( "d1-url", dep.getUrl() );

        Dependency dep2 = (Dependency) project.getDependencies().get( 1 );

        assertEquals( "g2", dep2.getGroupId() );

        assertEquals( "d2", dep2.getArtifactId() );

        assertEquals( "2.0", dep2.getVersion() );

        assertEquals( "d2-url", dep2.getUrl() );

        // ----------------------------------------------------------------------
        // Build
        // ----------------------------------------------------------------------

        Build build = project.getBuild();

        assertEquals( "/sourceDirectory", build.getSourceDirectory() );

        assertEquals( "/unitTestSourceDirectory", build.getUnitTestSourceDirectory() );

        UnitTest unitTest = build.getUnitTest();

        assertEquals( "**/*Test.java", (String) unitTest.getIncludes().get( 0 ) );

        assertEquals( "**/RepositoryTest.java", (String) unitTest.getExcludes().get( 0 ) );

        Resource resource0 = (Resource) unitTest.getResources().get( 0 );
        //assertEquals( "${basedir}/src/test", resource0.getDirectory() );
        assertEquals( "**/*.xml", (String) resource0.getIncludes().get( 0 ) );

        Resource resource1 = (Resource) build.getResources().get( 0 );

        assertEquals( "/src/conf", resource1.getDirectory() );

        assertEquals( "*.xsd", (String) resource1.getIncludes().get( 0 ) );

        Resource resource2 = (Resource) build.getResources().get( 1 );

        assertEquals( "/src/messages", resource2.getDirectory() );

        assertEquals( "org/apache/maven/messages", resource2.getTargetPath() );

        assertEquals( "messages*.properties", (String) resource2.getIncludes().get( 0 ) );

        // ----------------------------------------------------------------------
        // Reports
        // ----------------------------------------------------------------------

        assertEquals( "maven-jdepend-plugin", project.getReports().get( 0 ) );

        assertEquals( "maven-checkstyle-plugin", project.getReports().get( 1 ) );
    }
}
