package org.apache.maven.project.inheritance;

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

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PostGoal;
import org.apache.maven.model.PreGoal;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Scm;
import org.codehaus.plexus.util.StringUtils;

import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id: DefaultModelInheritanceAssembler.java,v 1.4 2004/08/23 20:24:54
 *          jdcasey Exp $
 * @todo generate this with modello to keep it in sync with changes in the
 *       model.
 */
public class DefaultModelInheritanceAssembler
    implements ModelInheritanceAssembler
{
    public void assembleModelInheritance( Model child, Model parent )
    {
        // Pom version
        if ( child.getModelVersion() == null )
        {
            child.setModelVersion( parent.getModelVersion() );
        }

        // Group id
        if ( child.getGroupId() == null )
        {
            child.setGroupId( parent.getGroupId() );
        }

        // artifactId
        if ( child.getArtifactId() == null )
        {
            child.setArtifactId( parent.getArtifactId() );
        }

        // name
        if ( child.getName() == null )
        {
            child.setName( parent.getName() );
        }

        // currentVersion
        if ( child.getVersion() == null )
        {
            child.setVersion( parent.getVersion() );
        }

        // inceptionYear
        if ( child.getInceptionYear() == null )
        {
            child.setInceptionYear( parent.getInceptionYear() );
        }

        // Name
        if ( child.getPackage() == null )
        {
            child.setPackage( parent.getPackage() );
        }

        // url
        if ( child.getUrl() == null )
        {
            child.setUrl( parent.getUrl() );
        }

        // ----------------------------------------------------------------------
        // Distribution
        // ----------------------------------------------------------------------

        if ( child.getDistributionManagement() == null )
        {
            child.setDistributionManagement( parent.getDistributionManagement() );
        }

        // issueManagement
        if ( child.getIssueManagement() == null )
        {
            child.setIssueManagement( parent.getIssueManagement() );
        }

        // Short description
        if ( child.getShortDescription() == null )
        {
            child.setShortDescription( parent.getShortDescription() );
        }

        // Short description
        if ( child.getDescription() == null )
        {
            child.setDescription( parent.getDescription() );
        }

        // Organization
        if ( child.getOrganization() == null )
        {
            child.setOrganization( parent.getOrganization() );
        }

        // Scm
        if ( parent.getScm() != null )
        {
            Scm parentScm = parent.getScm();

            Scm childScm = child.getScm();

            if ( childScm == null )
            {
                childScm = new Scm();

                child.setScm( childScm );
            }

            if ( StringUtils.isEmpty( childScm.getConnection() ) && 
                !StringUtils.isEmpty( parentScm.getConnection() ) )
            {
                childScm.setConnection( parentScm.getConnection() + "/" + child.getArtifactId() );
            }

            if ( StringUtils.isEmpty( childScm.getDeveloperConnection() ) &&
                !StringUtils.isEmpty( parentScm.getDeveloperConnection() ) )
            {
                childScm.setDeveloperConnection( parentScm.getDeveloperConnection() + "/" + child.getArtifactId() );
            }

            if ( StringUtils.isEmpty( childScm.getUrl() ) )
            {
                childScm.setUrl( parentScm.getUrl() );
            }

            if ( parentScm.getBranches() != null )
            {
                childScm.getBranches().addAll( parentScm.getBranches() );
            }
        }

        // ciManagement
        if ( child.getCiManagement() == null )
        {
            child.setCiManagement( parent.getCiManagement() );
        }

        // developers
        if ( child.getDevelopers().size() == 0 )
        {
            child.setDevelopers( parent.getDevelopers() );
        }

        // developers
        if ( child.getContributors().size() == 0 )
        {
            child.setContributors( parent.getContributors() );
        }

        // mailingLists
        if ( child.getMailingLists().size() == 0 )
        {
            child.setMailingLists( parent.getMailingLists() );
        }

        // reports
        if ( child.getReports().size() == 0 )
        {
            child.setReports( parent.getReports() );
        }

        // Build
        if ( child.getBuild() == null )
        {
            child.setBuild( parent.getBuild() );
        }
        else
        {
            // The build has been set but we want to step in here and fill in
            // values
            // that have not been set by the child.

            if ( child.getBuild().getDirectory() == null )
            {
                child.getBuild().setDirectory( parent.getBuild().getDirectory() );
            }

            if ( child.getBuild().getSourceDirectory() == null )
            {
                child.getBuild().setSourceDirectory( parent.getBuild().getSourceDirectory() );
            }

            if ( child.getBuild().getUnitTestSourceDirectory() == null )
            {
                child.getBuild().setUnitTestSourceDirectory( parent.getBuild().getUnitTestSourceDirectory() );
            }

            if ( child.getBuild().getAspectSourceDirectory() == null )
            {
                child.getBuild().setAspectSourceDirectory( parent.getBuild().getAspectSourceDirectory() );
            }

            if ( child.getBuild().getOutput() == null )
            {
                child.getBuild().setOutput( parent.getBuild().getOutput() );
            }

            if ( child.getBuild().getTestOutput() == null )
            {
                child.getBuild().setTestOutput( parent.getBuild().getTestOutput() );
            }

            List resources = child.getBuild().getResources();
            if ( resources == null || resources.isEmpty() )
            {
                child.getBuild().setResources( parent.getBuild().getResources() );
            }

            if ( child.getBuild().getUnitTest() == null )
            {
                child.getBuild().setUnitTest( parent.getBuild().getUnitTest() );
            }
            else
            {
                if ( child.getBuild().getUnitTest().getIncludes().size() == 0 )
                {
                    child.getBuild().getUnitTest().setIncludes( parent.getBuild().getUnitTest().getIncludes() );
                }

                if ( child.getBuild().getUnitTest().getExcludes().size() == 0 )
                {
                    child.getBuild().getUnitTest().setExcludes( parent.getBuild().getUnitTest().getExcludes() );
                }

                List testResources = child.getBuild().getUnitTest().getResources();
                if ( testResources == null || testResources.isEmpty() )
                {
                    child.getBuild().getUnitTest().setResources( parent.getBuild().getUnitTest().getResources() );
                }
            }
        }

        // Dependencies :: aggregate
        List dependencies = parent.getDependencies();

        for ( Iterator iterator = dependencies.iterator(); iterator.hasNext(); )
        {
            Dependency dependency = (Dependency) iterator.next();

            child.addDependency( dependency );

        }

        // PreGoals :: aggregate
        List preGoals = parent.getPreGoals();

        for ( Iterator iterator = preGoals.iterator(); iterator.hasNext(); )
        {
            PreGoal preGoal = (PreGoal) iterator.next();

            child.addPreGoal( preGoal );

        }

        // PostGoals :: aggregate
        List postGoals = parent.getPostGoals();

        for ( Iterator iterator = postGoals.iterator(); iterator.hasNext(); )
        {
            PostGoal postGoal = (PostGoal) iterator.next();

            child.addPostGoal( postGoal );

        }

        // Repositories :: aggregate
        List parentRepositories = parent.getRepositories();

        List childRepositories = child.getRepositories();

        for ( Iterator iterator = parentRepositories.iterator(); iterator.hasNext(); )
        {
            Repository repository = (Repository) iterator.next();

            if ( !childRepositories.contains( repository ) )
            {
                child.addRepository( repository );
            }
        }

        // Plugins :: aggregate
        List parentPlugins = parent.getPlugins();

        List childPlugins = child.getPlugins();

        for ( Iterator iterator = parentPlugins.iterator(); iterator.hasNext(); )
        {
            Plugin plugin = (Plugin) iterator.next();

            if ( !childPlugins.contains( plugin ) )
            {
                child.addPlugin( plugin );
            }
        }
    }
}
