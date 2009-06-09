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
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class IntegrationTestSuite
    extends TestCase
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

        // -------------------------------------------------------------------------------------------------------------
        // Tests that currently don't pass for any Maven version, i.e. the corresponding issue hasn't been resolved yet
        // -------------------------------------------------------------------------------------------------------------
        // suite.addTestSuite( MavenITmng4048VersionRangeReactorResolutionTest.class );
        // suite.addTestSuite( MavenITmng3890TransitiveDependencyScopeUpdateTest.class );
        // suite.addTestSuite( MavenITmng3814BogusProjectCycleTest.class );
        // suite.addTestSuite( MavenITmng3645POMSyntaxErrorTest.class );
        // suite.addTestSuite( MavenITmng3391ImportScopeErrorScenariosTest.class );
        // suite.addTestSuite( MavenITmng3043BestEffortReactorResolutionTest.class );
        // suite.addTestSuite( MavenITmng3038TransitiveDepManVersionTest.class );
        // suite.addTestSuite( MavenITmng2994SnapshotRangeRepositoryTest.class );
        // suite.addTestSuite( MavenITmng2771PomExtensionComponentOverrideTest.class );
        // suite.addTestSuite( MavenITmng0612NewestConflictResolverTest.class );

        // -------------------------------------------------------------------------------------------------------------
        // Tests that don't run stable and need to be fixed
        // -------------------------------------------------------------------------------------------------------------
        // suite.addTestSuite( MavenIT0109ReleaseUpdateTest.class );
        // suite.addTestSuite( MavenIT0108SnapshotUpdateTest.class ); -- MNG-3137

        suite.addTestSuite( MavenITmng4193UniqueRepoIdTest.class );
        suite.addTestSuite( MavenITmng4180PerDependencyExclusionsTest.class );
        suite.addTestSuite( MavenITmng4172EmptyDependencySetTest.class );
        suite.addTestSuite( MavenITmng4150VersionRangeTest.class );
        suite.addTestSuite( MavenITmng4129PluginExecutionInheritanceTest.class );
        suite.addTestSuite( MavenITmng4126ParentProfilesXmlTest.class );
        suite.addTestSuite( MavenITmng4116UndecodedUrlsTest.class );
        suite.addTestSuite( MavenITmng4107InterpolationUsesDominantProfileSourceTest.class );
        suite.addTestSuite( MavenITmng4106InterpolationUsesDominantProfileTest.class );
        suite.addTestSuite( MavenITmng4102InheritedPropertyInterpolationTest.class );
        suite.addTestSuite( MavenITmng4091BadPluginDescriptorTest.class );
        suite.addTestSuite( MavenITmng4087PercentEncodedFileUrlTest.class );
        suite.addTestSuite( MavenITmng4086ExplicitPluginMetaversionTest.class );
        suite.addTestSuite( MavenITmng4072InactiveProfileReposTest.class );
        suite.addTestSuite( MavenITmng4070WhitespaceTrimmingTest.class );
        suite.addTestSuite( MavenITmng4068AuthenticatedMirrorTest.class );
        suite.addTestSuite( MavenITmng4056ClassifierBasedDepResolutionFromReactorTest.class );
        suite.addTestSuite( MavenITmng4053PluginConfigAttributesTest.class );
        suite.addTestSuite( MavenITmng4040ProfileInjectedModulesTest.class );
        suite.addTestSuite( MavenITmng4036ParentResolutionFromSettingsRepoTest.class );
        suite.addTestSuite( MavenITmng4034ManagedProfileDependencyTest.class );
        suite.addTestSuite( MavenITmng4026ReactorDependenciesOrderTest.class );
        suite.addTestSuite( MavenITmng4023ParentProfileOneTimeInjectionTest.class );
        suite.addTestSuite( MavenITmng4016PrefixedPropertyInterpolationTest.class );
        suite.addTestSuite( MavenITmng4009InheritProfileEffectsTest.class );
        suite.addTestSuite( MavenITmng4008MergedFilterOrderTest.class );
        suite.addTestSuite( MavenITmng4007PlatformFileSeparatorTest.class );
        suite.addTestSuite( MavenITmng4000MultiPluginExecutionsTest.class );
        suite.addTestSuite( MavenITmng3998PluginExecutionConfigTest.class );
        suite.addTestSuite( MavenITmng3983PluginResolutionFromProfileReposTest.class );
        suite.addTestSuite( MavenITmng3979ElementJoinTest.class );
        suite.addTestSuite( MavenITmng3974MirrorOrderingTest.class );
        suite.addTestSuite( MavenITmng3970DepResolutionFromProfileReposTest.class );
        suite.addTestSuite( MavenITmng3955EffectiveSettingsTest.class );
        suite.addTestSuite( MavenITmng3953AuthenticatedDeploymentTest.class );
        suite.addTestSuite( MavenITmng3951AbsolutePathsTest.class );
        suite.addTestSuite( MavenITmng3948ParentResolutionFromProfileReposTest.class );
        suite.addTestSuite( MavenITmng3947PluginDefaultExecutionConfigTest.class );
        suite.addTestSuite( MavenITmng3944BasedirInterpolationTest.class );
        suite.addTestSuite( MavenITmng3943PluginExecutionInheritanceTest.class );
        suite.addTestSuite( MavenITmng3940EnvVarInterpolationTest.class );
        suite.addTestSuite( MavenITmng3938MergePluginExecutionsTest.class );
        suite.addTestSuite( MavenITmng3937MergedPluginExecutionGoalsTest.class );
        suite.addTestSuite( MavenITmng3933ProfilesXmlActivationTest.class );
        suite.addTestSuite( MavenITmng3927PluginDefaultExecutionConfigTest.class );
        suite.addTestSuite( MavenITmng3925MergedPluginExecutionOrderTest.class );
        suite.addTestSuite( MavenITmng3924XmlMarkupInterpolationTest.class );
        suite.addTestSuite( MavenITmng3920PlexusComponentApiTest.class );
        suite.addTestSuite( MavenITmng3916PluginExecutionInheritanceTest.class );
        suite.addTestSuite( MavenITmng3906MergedPluginClassPathOrderingTest.class );
        suite.addTestSuite( MavenITmng3904NestedBuildDirInterpolationTest.class );
        suite.addTestSuite( MavenITmng3900ProfilePropertiesInterpolationTest.class );
        suite.addTestSuite( MavenITmng3899ExtensionInheritanceTest.class );
        suite.addTestSuite( MavenITmng3892ReleaseDeploymentTest.class );
        suite.addTestSuite( MavenITmng3887PluginExecutionOrderTest.class );
        suite.addTestSuite( MavenITmng3886ExecutionGoalsOrderTest.class );
        suite.addTestSuite( MavenITmng3885UniqueVersionFromParentProfileTest.class );
        suite.addTestSuite( MavenITmng3877BasedirAlignedModelTest.class );
        suite.addTestSuite( MavenITmng3873MultipleExecutionGoalsTest.class );
        suite.addTestSuite( MavenITmng3866PluginConfigInheritanceTest.class );
        suite.addTestSuite( MavenITmng3864PerExecPluginConfigTest.class );
        suite.addTestSuite( MavenITmng3863AutoPluginGroupIdTest.class );
        suite.addTestSuite( MavenITmng3853ProfileInjectedDistReposTest.class );
        suite.addTestSuite( MavenITmng3852PluginConfigWithHeterogeneousListTest.class );
        suite.addTestSuite( MavenITmng3846PomInheritanceUrlAdjustmentTest.class );
        suite.addTestSuite( MavenITmng3845LimitedPomInheritanceTest.class );
        suite.addTestSuite( MavenITmng3843PomInheritanceTest.class );
        suite.addTestSuite( MavenITmng3839PomParsingCoalesceTextTest.class );
        suite.addTestSuite( MavenITmng3838EqualPluginDepsTest.class );
        suite.addTestSuite( MavenITmng3836PluginConfigInheritanceTest.class );
        suite.addTestSuite( MavenITmng3833PomInterpolationDataFlowChainTest.class );
        suite.addTestSuite( MavenITmng3831PomInterpolationTest.class );
        suite.addTestSuite( MavenITmng3827PluginConfigTest.class );
        suite.addTestSuite( MavenITmng3822BasedirAlignedInterpolationTest.class );
        suite.addTestSuite( MavenITmng3821EqualPluginExecIdsTest.class );
        suite.addTestSuite( MavenITmng3819PluginDepPlexusUtilsTest.class );
        suite.addTestSuite( MavenITmng3813PluginClassPathOrderingTest.class );
        suite.addTestSuite( MavenITmng3811ReportingPluginConfigurationInheritanceTest.class );
        suite.addTestSuite( MavenITmng3810BadProfileActivationTest.class );
        suite.addTestSuite( MavenITmng3808ReportInheritenceOrderingTest.class );
        suite.addTestSuite( MavenITmng3805ExtensionClassPathOrderingTest.class );
        suite.addTestSuite( MavenITmng3796ClassImportInconsistencyTest.class );
        suite.addTestSuite( MavenITmng3769ExclusionRelocatedTransdepsTest.class );
        suite.addTestSuite( MavenITmng3760BaseUriPropertyTest.class );
        suite.addTestSuite( MavenITmng3748BadSettingsXmlTest.class );
        suite.addTestSuite( MavenITmng3747PrefixedPathExpressionTest.class );
        suite.addTestSuite( MavenITmng3746POMPropertyOverrideTest.class );
        suite.addTestSuite( MavenITmng3743ForkWithPluginManagementTest.class );
        suite.addTestSuite( MavenITmng3740SelfReferentialReactorProjectsTest.class );
        suite.addTestSuite( MavenITmng3732ActiveProfilesTest.class );
        suite.addTestSuite( MavenITmng3729MultiForkAggregatorsTest.class );
        suite.addTestSuite( MavenITmng3724ExecutionProjectSyncTest.class );
        suite.addTestSuite( MavenITmng3723ConcreteParentProjectTest.class );
        suite.addTestSuite( MavenITmng3719PomExecutionOrderingTest.class );
        suite.addTestSuite( MavenITmng3716AggregatorForkingTest.class );
        suite.addTestSuite( MavenITmng3714ToolchainsCliOptionTest.class );
        suite.addTestSuite( MavenITmng3710PollutedClonedPluginsTest.class );
        suite.addTestSuite( MavenITmng3704LifecycleExecutorWrapperTest.class );
        suite.addTestSuite( MavenITmng3703ExecutionProjectWithRelativePathsTest.class );
        suite.addTestSuite( MavenITmng3701ImplicitProfileIdTest.class );
        suite.addTestSuite( MavenITmng3694ReactorProjectsDynamismTest.class );
        suite.addTestSuite( MavenITmng3693PomFileBasedirChangeTest.class );
        suite.addTestSuite( MavenITmng3684BuildPluginParameterTest.class );
        suite.addTestSuite( MavenITmng3680InvalidDependencyPOMTest.class );
        suite.addTestSuite( MavenITmng3679PluginExecIdInterpolationTest.class );
        suite.addTestSuite( MavenITmng3671PluginLevelDepInterpolationTest.class );
        suite.addTestSuite( MavenITmng3667ResolveDepsWithBadPomVersionTest.class );
        suite.addTestSuite( MavenITmng3652UserAgentHeaderTest.class );
        suite.addTestSuite( MavenITmng3642DynamicResourcesTest.class );
        suite.addTestSuite( MavenITmng3641ProfileActivationWarningTest.class );
        suite.addTestSuite( MavenITmng3621UNCInheritedPathsTest.class );
        suite.addTestSuite( MavenITmng3600DeploymentModeDefaultsTest.class );
        suite.addTestSuite( MavenITmng3599useHttpProxyForWebDAVTest.class );
        suite.addTestSuite( MavenITmng3586SystemScopePluginDependencyTest.class );
        suite.addTestSuite( MavenITmng3581PluginUsesWagonDependencyTest.class );
        suite.addTestSuite( MavenITmng3545ProfileDeactivationTest.class );
        suite.addTestSuite( MavenITmng3536AppendedAbsolutePathsTest.class );
        suite.addTestSuite( MavenITmng3535SelfReferentialPropertiesTest.class );
        suite.addTestSuite( MavenITmng3530DynamicPOMInterpolationTest.class );
        suite.addTestSuite( MavenITmng3503Xpp3ShadingTest.class );
        suite.addTestSuite( MavenITmng3498ForkToOtherMojoTest.class );
        suite.addTestSuite( MavenITmng3485OverrideWagonExtensionTest.class );
        suite.addTestSuite( MavenITmng3482DependencyPomInterpolationTest.class );
        suite.addTestSuite( MavenITmng3475BaseAlignedDirTest.class );
        suite.addTestSuite( MavenITmng3461MirrorMatchingTest.class );
        suite.addTestSuite( MavenITmng3441MetadataUpdatedFromDeploymentRepositoryTest.class );
        suite.addTestSuite( MavenITmng3428PluginDescriptorArtifactsIncompleteTest.class );
        suite.addTestSuite( MavenITmng3422ActiveComponentCollectionTest.class );
        suite.addTestSuite( MavenITmng3415JunkRepositoryMetadataTest.class );
        suite.addTestSuite( MavenITmng3396DependencyManagementForOverConstrainedRangesTest.class );
        suite.addTestSuite( MavenITmng3394POMPluginVersionDominanceTest.class );
        suite.addTestSuite( MavenITmng3380ManagedRelocatedTransdepsTest.class );
        suite.addTestSuite( MavenITmng3379ParallelArtifactDownloadsTest.class );
        suite.addTestSuite( MavenITmng3372DirectInvocationOfPluginsTest.class );
        suite.addTestSuite( MavenITmng3355TranslatedPathInterpolationTest.class );
        suite.addTestSuite( MavenITmng3331ModulePathNormalizationTest.class );
        suite.addTestSuite( MavenITmng3314OfflineSnapshotsTest.class );
        suite.addTestSuite( MavenITmng3288SystemScopeDirTest.class );
        suite.addTestSuite( MavenITmng3284UsingCachedPluginsTest.class );
        suite.addTestSuite( MavenITmng3271DefaultReportsSuppressionTest.class );
        suite.addTestSuite( MavenITmng3221InfiniteForkingTest.class );
        suite.addTestSuite( MavenITmng3268MultipleDashPCommandLineTest.class );
        suite.addTestSuite( MavenITmng3259DepsDroppedInMultiModuleBuildTest.class );
        suite.addTestSuite( MavenITmng3220ImportScopeTest.class );
        suite.addTestSuite( MavenITmng3217InterPluginDependencyTest.class );
        suite.addTestSuite( MavenITmng3139UseCachedMetadataOfBlacklistedRepoTest.class );
        suite.addTestSuite( MavenITmng3118TestClassPathOrderTest.class );
        suite.addTestSuite( MavenITmng3106ProfileMultipleActivatorsTest.class );
        suite.addTestSuite( MavenITmng3099SettingsProfilesWithNoPomTest.class );
        suite.addTestSuite( MavenITmng3057VersionExprTransformationsTest.class );
        suite.addTestSuite( MavenITmng3052DepRepoAggregationTest.class );
        suite.addTestSuite( MavenITmng3023ReactorDependencyResolutionTest.class );
        suite.addTestSuite( MavenITmng3012CoreClassImportTest.class );
        suite.addTestSuite( MavenITmng2972OverridePluginDependencyTest.class );
        suite.addTestSuite( MavenITmng2926PluginPrefixOrderTest.class );
        suite.addTestSuite( MavenITmng2921ActiveAttachedArtifactsTest.class );
        suite.addTestSuite( MavenITmng2892HideCorePlexusUtilsTest.class );
        suite.addTestSuite( MavenITmng2883LegacyRepoOfflineTest.class );
        suite.addTestSuite( MavenITmng2878DefaultReportXmlImportTest.class );
        suite.addTestSuite( MavenITmng2871PrePackageSubartifactResolutionTest.class );
        suite.addTestSuite( MavenITmng2865MirrorWildcardTest.class );
        suite.addTestSuite( MavenITmng2861RelocationsAndRangesTest.class );
        suite.addTestSuite( MavenITmng2843PluginConfigPropertiesInjectionTest.class );
        suite.addTestSuite( MavenITmng2831CustomArtifactHandlerAndCustomLifecycleTest.class );
        suite.addTestSuite( MavenITmng2820PomCommentsTest.class );
        suite.addTestSuite( MavenITmng2790LastUpdatedMetadataTest.class );
        suite.addTestSuite( MavenITmng2749ExtensionAvailableToPluginTest.class );
        suite.addTestSuite( MavenITmng2744checksumVerificationTest.class );
        suite.addTestSuite( MavenITmng2739RequiredRepositoryElementsTest.class );
        suite.addTestSuite( MavenITmng2720SiblingClasspathArtifactsTest.class );
        suite.addTestSuite( MavenITmng2695OfflinePluginSnapshotsTest.class );
        suite.addTestSuite( MavenITmng2690MojoLoadingErrorsTest.class );
        suite.addTestSuite( MavenITmng2668UsePluginDependenciesForSortingTest.class );
        suite.addTestSuite( MavenITmng2605BogusProfileActivationTest.class );
        suite.addTestSuite( MavenITmng2591MergeInheritedPluginConfigTest.class );
        suite.addTestSuite( MavenITmng2562TimestampTest.class );
        suite.addTestSuite( MavenITmng2539PluginDependenciesComeFromPluginReposTest.class );
        suite.addTestSuite( MavenITmng2432PluginPrefixOrderTest.class );
        suite.addTestSuite( MavenITmng2387InactiveProxyTest.class );
        suite.addTestSuite( MavenITmng2362DeployedPomEncodingTest.class );
        suite.addTestSuite( MavenITmng2339BadProjectInterpolationTest.class );
        suite.addTestSuite( MavenITmng2318LocalParentResolutionTest.class );
        suite.addTestSuite( MavenITmng2309ProfileInjectionOrderTest.class );
        suite.addTestSuite( MavenITmng2293CustomPluginParamImplTest.class );
        suite.addTestSuite( MavenITmng2277AggregatorAndResolutionPluginsTest.class );
        suite.addTestSuite( MavenITmng2254PomEncodingTest.class );
        suite.addTestSuite( MavenITmng2234ActiveProfilesFromSettingsTest.class );
        suite.addTestSuite( MavenITmng2228ComponentInjectionTest.class );
        suite.addTestSuite( MavenITmng2201PluginConfigInterpolationTest.class );
        suite.addTestSuite( MavenITmng2196ParentResolutionTest.class );
        suite.addTestSuite( MavenITmng2174PluginDepsManagedByParentProfileTest.class );
        suite.addTestSuite( MavenITmng2136ActiveByDefaultProfileTest.class );
        suite.addTestSuite( MavenITmng2130ParentLookupFromReactorCacheTest.class );
        suite.addTestSuite( MavenITmng2124PomInterpolationWithParentValuesTest.class );
        suite.addTestSuite( MavenITmng2123VersionRangeDependencyTest.class );
        suite.addTestSuite( MavenITmng2068ReactorRelativeParentsTest.class );
        suite.addTestSuite( MavenITmng2054PluginExecutionInheritanceTest.class );
        suite.addTestSuite( MavenITmng2052InterpolateWithSettingsProfilePropertiesTest.class );
        suite.addTestSuite( MavenITmng2045testJarDependenciesBrokenInReactorTest.class );
        suite.addTestSuite( MavenITmng2006ChildPathAwareUrlInheritanceTest.class );
        suite.addTestSuite( MavenITmng1999DefaultReportsInheritanceTest.class );
        suite.addTestSuite( MavenITmng1995InterpolateBooleanModelElementsTest.class );
        suite.addTestSuite( MavenITmng1992SystemPropOverridesPomPropTest.class );
        suite.addTestSuite( MavenITmng1957JdkActivationWithVersionRangeTest.class );
        suite.addTestSuite( MavenITmng1908LegacySnapshotUpdateTest.class );
        suite.addTestSuite( MavenITmng1830ShowVersionTest.class );
        suite.addTestSuite( MavenITmng1703PluginMgmtDepInheritanceTest.class );
        suite.addTestSuite( MavenITmng1493NonStandardModulePomNamesTest.class );
        suite.addTestSuite( MavenITmng1491ReactorArtifactIdCollisionTest.class );
        suite.addTestSuite( MavenITmng1415QuotedSystemPropertiesTest.class );
        suite.addTestSuite( MavenITmng1412DependenciesOrderTest.class );
        suite.addTestSuite( MavenITmng1349ChecksumFormatsTest.class );
        suite.addTestSuite( MavenITmng1323AntrunDependenciesTest.class );
        suite.addTestSuite( MavenITmng1233WarDepWithProvidedScopeTest.class );
        suite.addTestSuite( MavenITmng1088ReactorPluginResolutionTest.class );
        suite.addTestSuite( MavenITmng1052PluginMngtConfigTest.class );
        suite.addTestSuite( MavenITmng1021EqualAttachmentBuildNumberTest.class );
        suite.addTestSuite( MavenITmng0985NonExecutedPluginMngtGoalsTest.class );
        suite.addTestSuite( MavenITmng0956ComponentInjectionViaProjectLevelPluginDepTest.class );
        suite.addTestSuite( MavenITmng0947OptionalDependencyTest.class );
        suite.addTestSuite( MavenITmng0870ReactorAwarePluginDiscoveryTest.class );
        suite.addTestSuite( MavenITmng0866EvaluateDefaultValueTest.class );
        suite.addTestSuite( MavenITmng0848SystemPropOverridesDefaultValueTest.class );
        suite.addTestSuite( MavenITmng0836PluginParentResolutionTest.class );
        suite.addTestSuite( MavenITmng0823MojoContextPassingTest.class );
        suite.addTestSuite( MavenITmng0820ConflictResolutionTest.class );
        suite.addTestSuite( MavenITmng0818WarDepsNotTransitiveTest.class );
        suite.addTestSuite( MavenITmng0814ExplicitProfileActivationTest.class );
        suite.addTestSuite( MavenITmng0786ProfileAwareReactorTest.class );
        suite.addTestSuite( MavenITmng0781PluginConfigVsExecConfigTest.class );
        suite.addTestSuite( MavenITmng0773SettingsProfileReactorPollutionTest.class );
        suite.addTestSuite( MavenITmng0768OfflineModeTest.class );
        suite.addTestSuite( MavenITmng0761MissingSnapshotDistRepoTest.class );
        suite.addTestSuite( MavenITmng0731LegacyRepoLayoutTest.class );
        suite.addTestSuite( MavenITmng0680ParentBasedirTest.class );
        suite.addTestSuite( MavenITmng0674PluginParameterAliasTest.class );
        suite.addTestSuite( MavenITmng0666IgnoreLegacyPomTest.class );
        suite.addTestSuite( MavenITmng0557UserSettingsCliOptionTest.class );
        suite.addTestSuite( MavenITmng0553SettingsAuthzEncryptionTest.class );
        suite.addTestSuite( MavenITmng0522PluginMgmtConfigTest.class );
        suite.addTestSuite( MavenITmng0507ArtifactRelocationTest.class );
        suite.addTestSuite( MavenITmng0505VersionRangeTest.class );
        suite.addTestSuite( MavenITmng0496IgnoreUnknownPluginParametersTest.class );
        suite.addTestSuite( MavenITmng0479OverrideCentralRepoTest.class );
        suite.addTestSuite( MavenITmng0471CustomLifecycleTest.class );
        suite.addTestSuite( MavenITmng0469ReportConfigTest.class );
        suite.addTestSuite( MavenITmng0449PluginVersionResolutionTest.class );
        suite.addTestSuite( MavenITmng0377PluginLookupFromPrefixTest.class );
        suite.addTestSuite( MavenITmng0294MergeGlobalAndUserSettingsTest.class );
        suite.addTestSuite( MavenITmng0282NonReactorExecWhenProjectIndependentTest.class );
        suite.addTestSuite( MavenITmng0249ResolveDepsFromReactorTest.class );
        suite.addTestSuite( MavenITmng0095ReactorFailureBehaviorTest.class );
        suite.addTestSuite( MavenIT0144LifecycleExecutionOrderTest.class );
        suite.addTestSuite( MavenIT0143TransitiveDependencyScopesTest.class );
        suite.addTestSuite( MavenIT0142DirectDependencyScopesTest.class );
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
        suite.addTestSuite( MavenIT0113ServerAuthzAvailableToWagonMgrInPluginTest.class );
        suite.addTestSuite( MavenIT0090Test.class );
        suite.addTestSuite( MavenIT0087Test.class );
        suite.addTestSuite( MavenIT0086Test.class );
        suite.addTestSuite( MavenIT0085Test.class );
        suite.addTestSuite( MavenIT0072Test.class );
        suite.addTestSuite( MavenIT0071Test.class );
        suite.addTestSuite( MavenIT0064Test.class );
        suite.addTestSuite( MavenIT0063Test.class );
        suite.addTestSuite( MavenIT0056Test.class );
        suite.addTestSuite( MavenIT0052Test.class );
        suite.addTestSuite( MavenIT0051Test.class );
        suite.addTestSuite( MavenIT0041Test.class );
        suite.addTestSuite( MavenIT0040Test.class );
        suite.addTestSuite( MavenIT0039Test.class );
        suite.addTestSuite( MavenIT0038Test.class );
        suite.addTestSuite( MavenIT0037Test.class );
        suite.addTestSuite( MavenIT0036Test.class );
        suite.addTestSuite( MavenIT0032Test.class );
        suite.addTestSuite( MavenIT0030Test.class );
        suite.addTestSuite( MavenIT0025Test.class );
        suite.addTestSuite( MavenIT0024Test.class );
        suite.addTestSuite( MavenIT0023Test.class );
        suite.addTestSuite( MavenIT0021Test.class );
        suite.addTestSuite( MavenIT0019Test.class );
        suite.addTestSuite( MavenIT0018Test.class );
        suite.addTestSuite( MavenIT0012Test.class );
        suite.addTestSuite( MavenIT0011Test.class );
        suite.addTestSuite( MavenIT0010Test.class );
        suite.addTestSuite( MavenIT0009Test.class );
        suite.addTestSuite( MavenIT0008Test.class );

        /*
         * Add tests in reverse alpha order above.
         */

        return suite;
    }
}
