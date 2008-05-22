package org.apache.maven.project.aspect.compat;

import org.apache.maven.model.Model;
import org.apache.maven.project.build.model.ModelLineage;
import org.apache.maven.project.build.model.ModelLineageIterator;
import org.apache.maven.project.build.model.ModelLineageBuilder;
import org.apache.maven.project.DefaultMavenProjectBuilder;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.apache.maven.profiles.build.DefaultProfileAdvisor;
import org.apache.maven.model.Profile;
import org.apache.maven.profiles.ProfileManager;

import java.util.Iterator;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

public privileged aspect Maven20xProjectCompatAspect
{

    // FIXME: Re-enable this when we're closer to a 2.1 release.
//    private pointcut reactorProjectBuilds():
//        cflow( execution( * DefaultMavenProjectBuilder.buildFromSourceFileInternal( .. ) ) )
//        && !cflow( execution( * MavenMetadataSource.*( .. ) ) );
//
//    private pointcut lineageBuildResumed( DefaultMavenProjectBuilder projectBuilder, ModelLineage lineage ):
//        call( * ModelLineageBuilder.resumeBuildingModelLineage( ModelLineage, .. ) )
//        && this( projectBuilder )
//        && args( lineage, .. );
//
//    after( DefaultMavenProjectBuilder projectBuilder, ModelLineage lineage ):
//        reactorProjectBuilds()
//        && lineageBuildResumed( projectBuilder, lineage )
//    {
//        for ( ModelLineageIterator it = lineage.lineageIterator(); it.hasNext(); )
//        {
//            Model model = (Model) it.next();
//            List pluginRepos = model.getPluginRepositories();
//
//            if ( pluginRepos != null && !pluginRepos.isEmpty() )
//            {
//                StringBuffer message = new StringBuffer();
//                message.append( "The <pluginRepositories/> section of the POM has been deprecated. Please update your POM (" );
//                message.append( model.getId() );
//                message.append( ")." );
//
//                projectBuilder.logger.warn( message.toString() );
//            }
//        }
//    }
//
//    private pointcut externalProfilesApplied( DefaultProfileAdvisor advisor, ProfileManager profileManager ):
//        execution( * DefaultProfileAdvisor.applyActivatedExternalProfiles( .., ProfileManager+ ) )
//        && this( advisor )
//        && args( .., profileManager );
//
//
//    private boolean settingsProfilesChecked = false;
//
//    before( DefaultProfileAdvisor advisor, ProfileManager profileManager ):
//        reactorProjectBuilds()
//        && externalProfilesApplied( advisor, profileManager )
//    {
//        if ( profileManager == null )
//        {
//            return;
//        }
//
//        Map profilesById = profileManager.getProfilesById();
//        Set invalidProfiles = new HashSet();
//
//        boolean settingsProfilesEncountered = false;
//        for ( Iterator it = profilesById.values().iterator(); it.hasNext(); )
//        {
//            Profile profile = (Profile) it.next();
//
//            if ( "settings.xml".equals( profile.getSource() ) )
//            {
//                settingsProfilesEncountered = true;
//
//                if ( settingsProfilesChecked )
//                {
//                    continue;
//                }
//            }
//
//            List pluginRepos = profile.getPluginRepositories();
//            if ( pluginRepos != null && !pluginRepos.isEmpty() )
//            {
//                invalidProfiles.add( profile );
//            }
//        }
//
//        if ( !invalidProfiles.isEmpty() )
//        {
//            StringBuffer message = new StringBuffer();
//            message.append( "The <pluginRepositories/> section of the POM has been deprecated. Please update the following profiles:\n" );
//
//            for ( Iterator it = invalidProfiles.iterator(); it.hasNext(); )
//            {
//                Profile profile = (Profile) it.next();
//                message.append( "\n- " ).append( profile.getId() ).append( " (source: " ).append( profile.getSource() ).append( ")" );
//            }
//
//            message.append( "\n" );
//
//            advisor.logger.warn( message.toString() );
//        }
//
//        settingsProfilesChecked = settingsProfilesChecked || settingsProfilesEncountered;
//    }

}
