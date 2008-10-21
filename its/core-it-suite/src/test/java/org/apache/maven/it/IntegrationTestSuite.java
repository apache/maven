package org.apache.maven.it;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;

import java.io.PrintStream;

import junit.framework.Test;
import junit.framework.TestSuite;

public class IntegrationTestSuite
    extends AbstractMavenIntegrationTestCase
{
    private static PrintStream out = System.out;

    public static Test suite()
        throws VerificationException
    {
        Verifier verifier = null;
        try
        {
            verifier = new Verifier( "" );
            String mavenVersion = verifier.getMavenVersion();

            String executable = verifier.getExecutable();

            out.println( "Running integration tests for Maven " + mavenVersion + "\n\tusing Maven executable: " +
                executable );

            System.setProperty( "maven.version", mavenVersion );
        }
        finally
        {
            if ( verifier != null )
            {
                verifier.resetStreams();
            }
        }

        TestSuite suite = new TestSuite();

        /*
         * This must be the first one to ensure the local repository is properly setup.
         */
        suite.addTestSuite( MavenITBootstrapTest.class );

        /*
         * Add tests in reverse alpha order by number below. This makes testing new
         * ITs quicker and since it counts down to zero, it's easier to judge how close
         * the tests are to finishing. Newer tests are also more likely to fail, so this is
         * a fail fast technique as well.
         */
        
/* Tests to be added
MavenIT0109ReleaseUpdateTest
MavenIT0125NewestConflictResolverTest
MavenIT0127AntrunDependencies
MavenIT0128DistMgmtSiteUrlParentCalculationTest
MavenITmng2883LegacyRepoOfflineTest
MavenITmng3259DepsDroppedInMultiModuleBuild
MavenITmng3415JunkRepositoryMetadataTest
MavenITmng3645POMSyntaxErrorTest
*/

        suite.addTestSuite( MavenITmng3796Test.class );
        suite.addTestSuite( MavenITmng3748BadSettingsXmlTest.class );
        suite.addTestSuite( MavenITmng3747PrefixedPathExpressionTest.class );
        suite.addTestSuite( MavenITmng3746POMPropertyOverrideTest.class );
        suite.addTestSuite( MavenITmng3743ForkWithPluginManagementTest.class );
        suite.addTestSuite( MavenITmng3740SelfReferentialReactorProjectsTest.class );
        suite.addTestSuite( MavenITmng3729MultiForkAggregatorsTest.class );
        suite.addTestSuite( MavenITmng3724ExecutionProjectSyncTest.class );
        suite.addTestSuite( MavenITmng3723ConcreteParentProjectTest.class );
        suite.addTestSuite( MavenITmng3716AggregatorForkingTest.class );
        suite.addTestSuite( MavenITmng3710PollutedClonedPluginsTest.class );
        suite.addTestSuite( MavenITmng3704LifecycleExecutorWrapperTest.class );
        suite.addTestSuite( MavenITmng3703ExecutionProjectWithRelativePathsTest.class );
        suite.addTestSuite( MavenITmng3694ReactorProjectsDynamismTest.class );
        suite.addTestSuite( MavenITmng3693PomFileBasedirChangeTest.class );
        suite.addTestSuite( MavenITmng3684BuildPluginParameterTest.class );
        suite.addTestSuite( MavenITmng3680InvalidDependencyPOMTest.class );
        suite.addTestSuite( MavenITmng3679PluginExecIdInterpolationTest.class );
        suite.addTestSuite( MavenITmng3671PluginLevelDepInterpolationTest.class );
        suite.addTestSuite( MavenITmng3667ResolveDepsWithBadPomVersionTest.class );
        suite.addTestSuite( MavenITmng3652UserAgentHeader.class );
        suite.addTestSuite( MavenITmng3642DynamicResourcesTest.class );
        suite.addTestSuite( MavenITmng3599useHttpProxyForWebDAV.class );
        suite.addTestSuite( MavenITmng3581PluginUsesWagonDependency.class );
        suite.addTestSuite( MavenITmng3545ProfileDeactivation.class );
        suite.addTestSuite( MavenITmng3536AppendedAbsolutePaths.class );
        suite.addTestSuite( MavenITmng3535SelfReferentialProperties.class );
        suite.addTestSuite( MavenITmng3503Xpp3ShadingTest.class );
        suite.addTestSuite( MavenITmng3498ForkToOtherMojoTest.class );
        suite.addTestSuite( MavenITmng3485OverrideWagonExtensionTest.class );
        suite.addTestSuite( MavenITmng3482DependencyPomInterpolationTest.class );
        suite.addTestSuite( MavenITmng3475BaseAlignedDir.class );
        suite.addTestSuite( MavenITmng3441MetadataUpdatedFromDeploymentRepositoryTest.class );
        suite.addTestSuite( MavenITmng3428PluginDescriptorArtifactsIncompleteTest.class );
        suite.addTestSuite( MavenITmng3396DependencyManagementForOverConstrainedRanges.class );
        suite.addTestSuite( MavenITmng3394POMPluginVersionDominanceTest.class );
        suite.addTestSuite( MavenITmng3380ManagedRelocatedTransdepsTest.class );
        suite.addTestSuite( MavenITmng3372DirectInvocationOfPlugins.class );
        suite.addTestSuite( MavenITmng3355TranslatedPathInterpolationTest.class );
        suite.addTestSuite( MavenITmng3331ModulePathNormalization.class );
        suite.addTestSuite( MavenITmng3314OfflineSnapshotsTest.class );
        suite.addTestSuite( MavenITmng3271Test.class );
        suite.addTestSuite( MavenITmng3221InfiniteForking.class );
        suite.addTestSuite( MavenITmng3268MultipleDashPCommandLine.class );
        suite.addTestSuite( MavenITmng3220ImportScopeTest.class );
        suite.addTestSuite( MavenITmng3106ProfileMultipleActivators.class );
        suite.addTestSuite( MavenITmng3099SettingsProfilesWithNoPOM.class );
        suite.addTestSuite( MavenITmng3052DepRepoAggregationTest.class );
        suite.addTestSuite( MavenITmng3012Test.class );
        suite.addTestSuite( MavenITmng2972OverridePluginDependency.class );
        suite.addTestSuite( MavenITmng2892Test.class );
        suite.addTestSuite( MavenITmng2878Test.class );
        suite.addTestSuite( MavenITmng2861RelocationsAndRanges.class );
        suite.addTestSuite( MavenITmng2749Test.class );
        suite.addTestSuite( MavenITmng2744checksumVerificationTest.class );
        suite.addTestSuite( MavenITmng2739RequiredRepositoryElements.class );
        suite.addTestSuite( MavenITmng2695OfflinePluginSnapshotsTest.class );
        suite.addTestSuite( MavenITmng2562Timestamp.class );
        suite.addTestSuite( MavenITmng2339BadProjectInterpolationTest.class );
        suite.addTestSuite( MavenITmng2277AggregatorAndResolutionPluginsTest.class );
        suite.addTestSuite( MavenITmng2254PomEncodingTest.class );
        suite.addTestSuite( MavenITmng2234ActiveProfilesFromSettingsTest.class );
        suite.addTestSuite( MavenITmng2228Test.class );
        suite.addTestSuite( MavenITmng2123VersionRangeDependencyTest.class );
        suite.addTestSuite( MavenITmng2068ReactorRelativeParentsTest.class );
        suite.addTestSuite( MavenITmng2045testJarDependenciesBrokenInReactorTest.class );
        suite.addTestSuite( MavenITmng1999Test.class );
        suite.addTestSuite( MavenITmng1703Test.class );
        suite.addTestSuite( MavenITmng1493NonStandardModulePomNames.class );
        suite.addTestSuite( MavenITmng1491ReactorArtifactIdCollision.class );
        suite.addTestSuite( MavenITmng1412DependenciesOrderTest.class );
        suite.addTestSuite( MavenITmng0469Test.class );
        suite.addTestSuite( MavenIT0141Test.class );
        suite.addTestSuite( MavenIT0140Test.class );
        suite.addTestSuite( MavenIT0139Test.class );
        suite.addTestSuite( MavenIT0138PluginLifecycleTest.class );
        suite.addTestSuite( MavenIT0137EarLifecycleTest.class );
        suite.addTestSuite( MavenIT0136RarLifecycleTest.class );
        suite.addTestSuite( MavenIT0135EjbLifecycleTest.class );
        suite.addTestSuite( MavenIT0134WarLifecycleTest.class );
        suite.addTestSuite( MavenIT0133JarLifecycleTest.class );
        suite.addTestSuite( MavenIT0132PomLifecycleTest.class );
        suite.addTestSuite( MavenIT0131SiteLifecycleTest.class );
        suite.addTestSuite( MavenIT0130CleanLifecycleTest.class );
        suite.addTestSuite( MavenIT0129ResourceProvidedToAPluginAsAPluginDependency.class );
        suite.addTestSuite( MavenIT0119PluginPrefixOrder.class );
        suite.addTestSuite( MavenIT0118AttachedArtifactsInReactor.class );
        suite.addTestSuite( MavenIT0115CustomArtifactHandlerAndCustomLifecycleTest.class );
        suite.addTestSuite( MavenIT0113ServerAuthzAvailableToWagonMgrInPlugin.class );
        suite.addTestSuite( MavenIT0110PluginDependenciesComeFromPluginReposTest.class );
        suite.addTestSuite( MavenIT0107Test.class );
        suite.addTestSuite( MavenIT0106Test.class );
        suite.addTestSuite( MavenIT0104Test.class );
        suite.addTestSuite( MavenIT0103Test.class );
        suite.addTestSuite( MavenIT0102Test.class );
        suite.addTestSuite( MavenIT0101Test.class );
        suite.addTestSuite( MavenIT0100Test.class );
        suite.addTestSuite( MavenIT0099Test.class );
        suite.addTestSuite( MavenIT0098Test.class );
        suite.addTestSuite( MavenIT0097Test.class );
        suite.addTestSuite( MavenIT0096Test.class );
        suite.addTestSuite( MavenIT0095Test.class );
        suite.addTestSuite( MavenIT0092Test.class );
        suite.addTestSuite( MavenIT0091Test.class );
        suite.addTestSuite( MavenIT0090Test.class );
        suite.addTestSuite( MavenIT0088Test.class );
        suite.addTestSuite( MavenIT0087Test.class );
        suite.addTestSuite( MavenIT0086Test.class );
        suite.addTestSuite( MavenIT0085Test.class );
        suite.addTestSuite( MavenIT0084Test.class );
        suite.addTestSuite( MavenIT0083Test.class );
        suite.addTestSuite( MavenIT0082Test.class );
        suite.addTestSuite( MavenIT0081Test.class );
        suite.addTestSuite( MavenIT0080Test.class );
        suite.addTestSuite( MavenIT0079Test.class );
        suite.addTestSuite( MavenIT0078Test.class );
        suite.addTestSuite( MavenIT0077Test.class );
        suite.addTestSuite( MavenIT0076Test.class );
        suite.addTestSuite( MavenIT0075Test.class );
        suite.addTestSuite( MavenIT0074Test.class );
        suite.addTestSuite( MavenIT0073Test.class );
        suite.addTestSuite( MavenIT0072Test.class );
        suite.addTestSuite( MavenIT0071Test.class );
        suite.addTestSuite( MavenIT0069Test.class );
        suite.addTestSuite( MavenIT0068Test.class );
        suite.addTestSuite( MavenIT0067Test.class );
        suite.addTestSuite( MavenIT0066Test.class );
        suite.addTestSuite( MavenIT0065Test.class );
        suite.addTestSuite( MavenIT0064Test.class );
        suite.addTestSuite( MavenIT0063Test.class );
        suite.addTestSuite( MavenIT0062Test.class );
        suite.addTestSuite( MavenIT0061Test.class );
        suite.addTestSuite( MavenIT0060Test.class );
        suite.addTestSuite( MavenIT0059Test.class );
        suite.addTestSuite( MavenIT0058Test.class );
        suite.addTestSuite( MavenIT0057Test.class );
        suite.addTestSuite( MavenIT0056Test.class );
        suite.addTestSuite( MavenIT0053Test.class );
        suite.addTestSuite( MavenIT0052Test.class );
        suite.addTestSuite( MavenIT0051Test.class );
        suite.addTestSuite( MavenIT0049Test.class );
        suite.addTestSuite( MavenIT0048Test.class );
        suite.addTestSuite( MavenIT0047Test.class );
        suite.addTestSuite( MavenIT0046Test.class );
        suite.addTestSuite( MavenIT0045Test.class );
        suite.addTestSuite( MavenIT0044Test.class );
        suite.addTestSuite( MavenIT0043Test.class );
        suite.addTestSuite( MavenIT0042Test.class );
        suite.addTestSuite( MavenIT0041Test.class );
        suite.addTestSuite( MavenIT0040Test.class );
        suite.addTestSuite( MavenIT0039Test.class );
        suite.addTestSuite( MavenIT0038Test.class );
        suite.addTestSuite( MavenIT0037Test.class );
        suite.addTestSuite( MavenIT0036Test.class );
        suite.addTestSuite( MavenIT0035Test.class );
        suite.addTestSuite( MavenIT0034Test.class );
        suite.addTestSuite( MavenIT0032Test.class );
        suite.addTestSuite( MavenIT0031Test.class );
        suite.addTestSuite( MavenIT0030Test.class );
        suite.addTestSuite( MavenIT0029Test.class );
        suite.addTestSuite( MavenIT0028Test.class );
        suite.addTestSuite( MavenIT0027Test.class );
        suite.addTestSuite( MavenIT0026Test.class );
        suite.addTestSuite( MavenIT0025Test.class );
        suite.addTestSuite( MavenIT0024Test.class );
        suite.addTestSuite( MavenIT0023Test.class );
        suite.addTestSuite( MavenIT0022Test.class );
        suite.addTestSuite( MavenIT0021Test.class );
        suite.addTestSuite( MavenIT0019Test.class );
        suite.addTestSuite( MavenIT0018Test.class );
        suite.addTestSuite( MavenIT0014Test.class );
        suite.addTestSuite( MavenIT0012Test.class );
        suite.addTestSuite( MavenIT0011Test.class );
        suite.addTestSuite( MavenIT0010Test.class );
        suite.addTestSuite( MavenIT0009Test.class );
        suite.addTestSuite( MavenIT0008Test.class );
        suite.addTestSuite( MavenIT0007Test.class );
        suite.addTestSuite( MavenIT0005Test.class );
        suite.addTestSuite( MavenIT0004Test.class );
        suite.addTestSuite( MavenIT0003Test.class );
        suite.addTestSuite( MavenIT0002Test.class );
        suite.addTestSuite( MavenIT0001Test.class );
        suite.addTestSuite( MavenIT0000Test.class );

        /*
         * Add tests in reverse alpha order above.
         */

        // ----------------------------------------------------------------------------------------------------
        // Tests that need to be fixed.
        // ----------------------------------------------------------------------------------------------------

        // Pending resolution in code
        // suite.addTestSuite( MavenITmng3391ImportScopeErrorScenariosTest.class );
        // suite.addTestSuite( MavenITmng3284UsingCachedPluginsTest.class );
        // suite.addTestSuite( MavenITmng3530DynamicPOMInterpolationTest.class );

        // -- not passing for 2.0.7 either, looks to be 2.1+ ?
        // suite.addTestSuite( MavenIT0120EjbClientDependency.class );

        // suite.addTestSuite( MavenIT0108SnapshotUpdateTest.class ); -- MNG-3137
        // suite.addTestSuite( MavenIT0121TransitiveDepManVersion.class ); -- MNG-3038
        // suite.addTestSuite( MavenIT0122ReactorDependencyResolutionTest.class ); -- MNG-3023
        // suite.addTestSuite( MavenIT0123SnapshotRangeRepositoryTest.class ); -- MNG-2994
        // suite.addTestSuite( MavenIT0124PomExtensionComponentOverrideTest.class ); -- MNG-2771

        // suite.addTestSuite( MavenIT0126TestJarDependency.class ); // MJAR-75 / MNG-3160

        return suite;
    }
}
