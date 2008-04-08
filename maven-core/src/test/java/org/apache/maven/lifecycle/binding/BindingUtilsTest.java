package org.apache.maven.lifecycle.binding;

import org.apache.maven.lifecycle.model.BuildBinding;
import org.apache.maven.lifecycle.model.LifecycleBindings;
import org.apache.maven.lifecycle.model.MojoBinding;
import org.apache.maven.lifecycle.model.Phase;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;
import org.apache.maven.project.MavenProject;

import junit.framework.TestCase;

public class BindingUtilsTest
    extends TestCase
{

    public void testInjectProjectConfiguration_CheckReportPluginsForVersionInformation()
    {
        Model model = new Model();
        Build build = new Build();

        String gid = "group";
        String aid = "artifact";
        String version = "1";

        model.setGroupId( gid );
        model.setArtifactId( aid );
        model.setVersion( version );

        model.setBuild( build );

        String pGid = "group.plugins";
        String pAid = "maven-test-plugin";
        String pVersion = "2";

        Plugin plugin = new Plugin();
        plugin.setGroupId( pGid );
        plugin.setArtifactId( pAid );
        plugin.setVersion( pVersion );

        build.addPlugin( plugin );

        Reporting reporting = new Reporting();

        model.setReporting( reporting );

        String rGid = "group.reports";
        String rAid = "maven-report-plugin";
        String rVersion = "3";

        ReportPlugin rPlugin = new ReportPlugin();
        rPlugin.setGroupId( rGid );
        rPlugin.setArtifactId( rAid );
        rPlugin.setVersion( rVersion );

        reporting.addPlugin( rPlugin );

        MavenProject project = new MavenProject( model );

        LifecycleBindings bindings = new LifecycleBindings();

        BuildBinding buildBinding = new BuildBinding();
        bindings.setBuildBinding( buildBinding );

        MojoBinding mb = new MojoBinding();
        mb.setGroupId( rGid );
        mb.setArtifactId( rAid );
        mb.setExecutionId( "test" );
        mb.setGoal( "goal" );

        Phase compile = new Phase();
        compile.addBinding( mb );

        buildBinding.setCompile( compile );

        BindingUtils.injectProjectConfiguration( bindings, project );

        assertEquals( rVersion, mb.getVersion() );
    }

    public void testInjectProjectConfiguration_NormalPluginInformationOverridesReportPluginsInformation()
    {
        Model model = new Model();
        Build build = new Build();

        String gid = "group";
        String aid = "artifact";
        String version = "1";

        model.setGroupId( gid );
        model.setArtifactId( aid );
        model.setVersion( version );

        model.setBuild( build );

        String pAid = "maven-test-plugin";
        String pVersion = "2";

        Plugin plugin = new Plugin();
        plugin.setGroupId( gid );
        plugin.setArtifactId( pAid );
        plugin.setVersion( pVersion );

        build.addPlugin( plugin );

        Reporting reporting = new Reporting();

        model.setReporting( reporting );

        String rVersion = "3";

        ReportPlugin rPlugin = new ReportPlugin();
        rPlugin.setGroupId( gid );
        rPlugin.setArtifactId( pAid );
        rPlugin.setVersion( rVersion );

        reporting.addPlugin( rPlugin );

        MavenProject project = new MavenProject( model );

        LifecycleBindings bindings = new LifecycleBindings();

        BuildBinding buildBinding = new BuildBinding();
        bindings.setBuildBinding( buildBinding );

        MojoBinding mb = new MojoBinding();
        mb.setGroupId( gid );
        mb.setArtifactId( pAid );
        mb.setExecutionId( "test" );
        mb.setGoal( "goal" );

        Phase compile = new Phase();
        compile.addBinding( mb );

        buildBinding.setCompile( compile );

        BindingUtils.injectProjectConfiguration( bindings, project );

        assertEquals( pVersion, mb.getVersion() );
    }

}
