package org.apache.maven.project.inheritance;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;

import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 * @todo generate this with modello to keep it in sync with changes in the model.
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
        if ( child.getScm() == null )
        {
            child.setScm( parent.getScm() );
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
            // The build has been set but we want to step in here and fill in values
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
            }
        }

        // Dependencies :: aggregate
        List dependencies = parent.getDependencies();

        for ( Iterator iterator = dependencies.iterator(); iterator.hasNext(); )
        {
            Dependency dependency = (Dependency) iterator.next();

            child.addDependency( dependency );

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
    }
}
