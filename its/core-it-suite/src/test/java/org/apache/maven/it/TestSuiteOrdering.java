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

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.shared.verifier.Verifier;
import org.junit.jupiter.api.ClassDescriptor;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.ClassOrdererContext;
import org.junit.jupiter.api.Tag;

/**
 * The Core IT suite.
 */
public class TestSuiteOrdering implements ClassOrderer
{

    private static PrintStream out = System.out;

    // store missed test to show info only once
    private final static List<Class<?>> MISSED_TESTS = new ArrayList<>();

    final Map<Class<?>, Integer> tests = new HashMap<>();

    private static void infoProperty( PrintStream info, String property )
    {
        info.println( property + ": " + System.getProperty( property ) );
    }

    static
    {
        try
        {
            // TODO: workaround for https://github.com/apache/maven-integration-testing/pull/232
            // The verifier currently uses system properties to configure itself, such as
            // maven.home (see https://github.com/apache/maven-integration-testing/blob/ba72268198fb4c68890f11bfa0aac3f4889c79b9/core-it-suite/pom.xml#L509-L511)
            // or other properties to configure the maven that will be launched.  Using system properties
            // make impossible the detection whether a system property has been set by the maven being run
            // or by the code that wants to use the verifier to create a new embedded maven, which means
            // those properties can not be cleared by the verifier.  So clear those properties here, as
            // we do want to isolate the tests from the outside environment.
            System.clearProperty( "maven.bootclasspath" );
            System.clearProperty( "maven.conf" );
            System.clearProperty( "classworlds.conf" );

            Verifier verifier = new Verifier( "" );
            String mavenVersion = verifier.getMavenVersion();
            String executable = verifier.getExecutable();

            out.println( "Running integration tests for Maven " + mavenVersion + System.lineSeparator()
                             + "\tusing Maven executable: " + executable + System.lineSeparator()
                             + "\twith verifier.forkMode: " + System.getProperty( "verifier.forkMode",
                                                                                  "not defined == fork" ) );

            System.setProperty( "maven.version", mavenVersion );

            String basedir = System.getProperty( "basedir", "." );

            try ( PrintStream info = new PrintStream(
                Files.newOutputStream( Paths.get( basedir, "target/info.txt" ) ) ) )
            {
                infoProperty( info, "maven.version" );
                infoProperty( info, "java.version" );
                infoProperty( info, "os.name" );
                infoProperty( info, "os.version" );
            }
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }

    public TestSuiteOrdering()
    {
        TestSuiteOrdering suite = this;

        /*
         * This must be the first one to ensure the local repository is properly setup.
         */
        suite.addTestSuite( MavenITBootstrapTest.class, -10 );

        /*
         * Add tests in reverse order of implementation. This makes testing new
         * ITs quicker and since it counts down to zero, it's easier to judge how close
         * the tests are to finishing. Newer tests are also more likely to fail, so this is
         * a fail fast technique as well.
         */
        suite.addTestSuite( MavenITmng7038RootdirTest.class );
        suite.addTestSuite( MavenITmng7697PomWithEmojiTest.class );
        suite.addTestSuite( MavenITmng7737ProfileActivationTest.class );
        suite.addTestSuite( MavenITmng7716BuildDeadlock.class );
        suite.addTestSuite( MavenITmng7679SingleMojoNoPomTest.class );
        suite.addTestSuite( MavenITmng7629SubtreeBuildTest.class );
        suite.addTestSuite( MavenITmng7606DependencyImportScopeTest.class );
        suite.addTestSuite( MavenITmng6609ProfileActivationForPackagingTest.class );
        suite.addTestSuite( MavenITmng7566JavaPrerequisiteTest.class );
        suite.addTestSuite( MavenITmng5889FindBasedir.class );
        suite.addTestSuite( MavenITmng7360BuildConsumer.class );
        suite.addTestSuite( MavenITmng5452MavenBuildTimestampUTCTest.class );
        suite.addTestSuite( MavenITmng3890TransitiveDependencyScopeUpdateTest.class );
        suite.addTestSuite( MavenITmng3092SnapshotsExcludedFromVersionRangeTest.class );
        suite.addTestSuite( MavenITmng3038TransitiveDepManVersionTest.class );
        suite.addTestSuite( MavenITmng2771PomExtensionComponentOverrideTest.class );
        suite.addTestSuite( MavenITmng0612NewestConflictResolverTest.class );
        suite.addTestSuite( MavenIT0108SnapshotUpdateTest.class );
        suite.addTestSuite( MavenITmng7310LifecycleActivatedInSpecifiedModuleTest.class );
        suite.addTestSuite( MavenITmng7474SessionScopeTest.class );
        suite.addTestSuite( MavenITmng7529VersionRangeRepositorySelection.class );
        suite.addTestSuite( MavenITmng7443ConsistencyOfOptionalProjectsAndProfilesTest.class );
        suite.addTestSuite( MavenITmng7353CliGoalInvocationTest.class );
        suite.addTestSuite( MavenITmng7504NotWarnUnsupportedReportPluginsTest.class );
        suite.addTestSuite( MavenITmng7160ExtensionClassloader.class );
        suite.addTestSuite( MavenITmng7468UnsupportedPluginsParametersTest.class );
        suite.addTestSuite( MavenITmng7487DeadlockTest.class );
        suite.addTestSuite( MavenITmng7470ResolverTransportTest.class );
        suite.addTestSuite( MavenITmng7464ReadOnlyMojoParametersWarningTest.class );
        suite.addTestSuite( MavenITmng7404IgnorePrefixlessExpressionsTest.class );
        suite.addTestSuite( MavenITmng5222MojoDeprecatedTest.class );
        suite.addTestSuite( MavenITmng7390SelectModuleOutsideCwdTest.class );
        suite.addTestSuite( MavenITmng7244IgnorePomPrefixInExpressions.class );
        suite.addTestSuite( MavenITmng7349RelocationWarningTest.class );
        suite.addTestSuite( MavenITmng6326CoreExtensionsNotFoundTest.class );
        suite.addTestSuite( MavenITmng5561PluginRelocationLosesConfigurationTest.class );
        suite.addTestSuite( MavenITmng7335MissingJarInParallelBuild.class );
        suite.addTestSuite( MavenITmng4463DependencyManagementImportVersionRanges.class );
        suite.addTestSuite( MavenITmng7112ProjectsWithNonRecursiveTest.class );
        suite.addTestSuite( MavenITmng7128BlockExternalHttpReactorTest.class );
        suite.addTestSuite( MavenITmng6511OptionalProjectSelectionTest.class );
        suite.addTestSuite( MavenITmng7110ExtensionClassloader.class );
        suite.addTestSuite( MavenITmng7051OptionalProfileActivationTest.class );
        suite.addTestSuite( MavenITmng6957BuildConsumer.class );
        suite.addTestSuite( MavenITmng7045DropUselessAndOutdatedCdiApiTest.class );
        suite.addTestSuite( MavenITmng6566ExecuteAnnotationShouldNotReExecuteGoalsTest.class );
        suite.addTestSuite( MavenITmng6754TimestampInMultimoduleProject.class );
        suite.addTestSuite( MavenITmng6981ProjectListShouldIncludeChildrenTest.class );
        suite.addTestSuite( MavenITmng6972AllowAccessToGraphPackageTest.class );
        suite.addTestSuite( MavenITmng6772NestedImportScopeRepositoryOverride.class );
        suite.addTestSuite( MavenITmng6759TransitiveDependencyRepositoriesTest.class );
        suite.addTestSuite( MavenITmng6720FailFastTest.class );
        suite.addTestSuite( MavenITmng6656BuildConsumer.class );
        suite.addTestSuite( MavenITmng6562WarnDefaultBindings.class );
        suite.addTestSuite( MavenITmng6558ToolchainsBuildingEventTest.class );
        suite.addTestSuite( MavenITmng6506PackageAnnotationTest.class );
        suite.addTestSuite( MavenITmng6391PrintVersionTest.class );
        suite.addTestSuite( MavenITmng6386BaseUriPropertyTest.class );
        suite.addTestSuite( MavenITmng6330RelativePath.class );
        suite.addTestSuite( MavenITmng6256SpecialCharsAlternatePOMLocation.class );
        suite.addTestSuite( MavenITmng6255FixConcatLines.class );
        suite.addTestSuite( MavenITmng6240PluginExtensionAetherProvider.class );
        suite.addTestSuite( MavenITmng6223FindBasedir.class );
        suite.addTestSuite( MavenITmng6210CoreExtensionsCustomScopesTest.class );
        suite.addTestSuite( MavenITmng6189SiteReportPluginsWarningTest.class );
        suite.addTestSuite( MavenITmng6173GetProjectsAndDependencyGraphTest.class );
        suite.addTestSuite( MavenITmng6173GetAllProjectsInReactorTest.class );
        suite.addTestSuite( MavenITmng6127PluginExecutionConfigurationInterferenceTest.class );
        suite.addTestSuite( MavenITmng6118SubmoduleInvocation.class );
        suite.addTestSuite( MavenITmng6090CIFriendlyTest.class );
        suite.addTestSuite( MavenITmng6084Jsr250PluginTest.class );
        suite.addTestSuite( MavenITmng6071GetResourceWithCustomPom.class );
        suite.addTestSuite( MavenITmng6065FailOnSeverityTest.class );
        suite.addTestSuite( MavenITmng6057CheckReactorOrderTest.class );
        suite.addTestSuite( MavenITmng5965ParallelBuildMultipliesWorkTest.class );
        suite.addTestSuite( MavenITmng5958LifecyclePhaseBinaryCompat.class );
        suite.addTestSuite( MavenITmng5935OptionalLostInTranstiveManagedDependenciesTest.class );
        suite.addTestSuite( MavenITmng5898BuildMultimoduleWithEARFailsToResolveWARTest.class );
        suite.addTestSuite( MavenITmng5895CIFriendlyUsageWithPropertyTest.class );
        suite.addTestSuite( MavenITmng5868NoDuplicateAttachedArtifacts.class );
        suite.addTestSuite( MavenITmng5840RelativePathReactorMatching.class );
        suite.addTestSuite( MavenITmng5840ParentVersionRanges.class );
        suite.addTestSuite( MavenITmng5805PkgTypeMojoConfiguration2.class );
        suite.addTestSuite( MavenITmng5783PluginDependencyFiltering.class );
        suite.addTestSuite( MavenITmng5774ConfigurationProcessorsTest.class );
        suite.addTestSuite( MavenITmng5771CoreExtensionsTest.class );
        suite.addTestSuite( MavenITmng5768CliExecutionIdTest.class );
        suite.addTestSuite( MavenITmng5760ResumeFeatureTest.class );
        suite.addTestSuite( MavenITmng5753CustomMojoExecutionConfiguratorTest.class );
        suite.addTestSuite( MavenITmng5742BuildExtensionClassloaderTest.class );
        suite.addTestSuite( MavenITmng5716ToolchainsTypeTest.class );
        suite.addTestSuite( MavenITmng5669ReadPomsOnce.class );
        suite.addTestSuite( MavenITmng5663NestedImportScopePomResolutionTest.class );
        suite.addTestSuite( MavenITmng5640LifecycleParticipantAfterSessionEnd.class );
        suite.addTestSuite( MavenITmng5639ImportScopePomResolutionTest.class );
        suite.addTestSuite( MavenITmng5608ProfileActivationWarningTest.class );
        suite.addTestSuite( MavenITmng5591WorkspaceReader.class );
        suite.addTestSuite( MavenITmng5581LifecycleMappingDelegate.class );
        suite.addTestSuite( MavenITmng5578SessionScopeTest.class );
        suite.addTestSuite( MavenITmng5576CdFriendlyVersions.class );
        suite.addTestSuite( MavenITmng5572ReactorPluginExtensionsTest.class );
        suite.addTestSuite( MavenITmng5530MojoExecutionScopeTest.class );
        suite.addTestSuite( MavenITmng5482AetherNotFoundTest.class );
        suite.addTestSuite( MavenITmng5445LegacyStringSearchModelInterpolatorTest.class );
        suite.addTestSuite( MavenITmng5389LifecycleParticipantAfterSessionEnd.class );
        suite.addTestSuite( MavenITmng5387ArtifactReplacementPlugin.class );
        suite.addTestSuite( MavenITmng5382Jsr330Plugin.class );
        suite.addTestSuite( MavenITmng5338FileOptionToDirectory.class );
        suite.addTestSuite( MavenITmng5280SettingsProfilesRepositoriesOrderTest.class );
        suite.addTestSuite( MavenITmng5230MakeReactorWithExcludesTest.class );
        suite.addTestSuite( MavenITmng5224InjectedSettings.class );
        suite.addTestSuite( MavenITmng5214DontMapWsdlToJar.class );
        suite.addTestSuite( MavenITmng5208EventSpyParallelTest.class );
        suite.addTestSuite( MavenITmng5175WagonHttpTest.class );
        suite.addTestSuite( MavenITmng5137ReactorResolutionInForkedBuildTest.class );
        suite.addTestSuite( MavenITmng5135AggregatorDepResolutionModuleExtensionTest.class );
        suite.addTestSuite( MavenITmng5096ExclusionAtDependencyWithImpliedClassifierTest.class );
        suite.addTestSuite( MavenITmng5064SuppressSnapshotUpdatesTest.class );
        suite.addTestSuite( MavenITmng5019StringBasedCompLookupFromChildPluginRealmTest.class );
        suite.addTestSuite( MavenITmng5013ConfigureParamBeanFromScalarValueTest.class );
        suite.addTestSuite( MavenITmng5012CollectionVsArrayParamCoercionTest.class );
        suite.addTestSuite( MavenITmng5011ConfigureCollectionArrayFromUserPropertiesTest.class );
        suite.addTestSuite( MavenITmng5009AggregationCycleTest.class );
        suite.addTestSuite( MavenITmng5006VersionRangeDependencyParentResolutionTest.class );
        suite.addTestSuite( MavenITmng5000ChildPathAwareUrlInheritanceTest.class );
        suite.addTestSuite( MavenITmng4992MapStylePropertiesParamConfigTest.class );
        suite.addTestSuite( MavenITmng4991NonProxyHostsTest.class );
        suite.addTestSuite( MavenITmng4987TimestampBasedSnapshotSelectionTest.class );
        suite.addTestSuite( MavenITmng4975ProfileInjectedPluginExecutionOrderTest.class );
        suite.addTestSuite( MavenITmng4973ExtensionVisibleToPluginInReactorTest.class );
        suite.addTestSuite( MavenITmng4966AbnormalUrlPreservationTest.class );
        suite.addTestSuite( MavenITmng4963ParentResolutionFromMirrorTest.class );
        suite.addTestSuite( MavenITmng4960MakeLikeReactorResumeTest.class );
        suite.addTestSuite( MavenITmng4955LocalVsRemoteSnapshotResolutionTest.class );
        suite.addTestSuite( MavenITmng4952MetadataReleaseInfoUpdateTest.class );
        suite.addTestSuite( MavenITmng4936EventSpyTest.class );
        suite.addTestSuite( MavenITmng4925ContainerLookupRealmDuringMojoExecTest.class );
        suite.addTestSuite( MavenITmng4919LifecycleMappingWithSameGoalTwiceTest.class );
        suite.addTestSuite( MavenITmng4913UserPropertyVsDependencyPomPropertyTest.class );
        suite.addTestSuite( MavenITmng4895PluginDepWithNonRelocatedMavenApiTest.class );
        suite.addTestSuite( MavenITmng4891RobustSnapshotResolutionTest.class );
        suite.addTestSuite( MavenITmng4890MakeLikeReactorConsidersVersionsTest.class );
        suite.addTestSuite( MavenITmng4883FailUponOverconstrainedVersionRangesTest.class );
        suite.addTestSuite( MavenITmng4877DeployUsingPrivateKeyTest.class );
        suite.addTestSuite( MavenITmng4874UpdateLatestPluginVersionTest.class );
        suite.addTestSuite( MavenITmng4872ReactorResolutionAttachedWithExclusionsTest.class );
        suite.addTestSuite( MavenITmng4842ParentResolutionOfDependencyPomTest.class );
        suite.addTestSuite( MavenITmng4840MavenPrerequisiteTest.class );
        suite.addTestSuite( MavenITmng4834ParentProjectResolvedFromRemoteReposTest.class );
        suite.addTestSuite( MavenITmng4829ChecksumFailureWarningTest.class );
        suite.addTestSuite( MavenITmng4814ReResolutionOfDependenciesDuringReactorTest.class );
        suite.addTestSuite( MavenITmng4811CustomComponentConfiguratorTest.class );
        suite.addTestSuite( MavenITmng4800NearestWinsVsScopeWideningTest.class );
        suite.addTestSuite( MavenITmng4795DepResolutionInReactorProjectForkedByLifecycleTest.class );
        suite.addTestSuite( MavenITmng4791ProjectBuilderResolvesRemotePomArtifactTest.class );
        suite.addTestSuite( MavenITmng4789ScopeInheritanceMeetsConflictTest.class );
        suite.addTestSuite( MavenITmng4788InstallationToCustomLocalRepoTest.class );
        suite.addTestSuite( MavenITmng4786AntBased21xMojoSupportTest.class );
        suite.addTestSuite( MavenITmng4785TransitiveResolutionInForkedThreadTest.class );
        suite.addTestSuite( MavenITmng4781DeploymentToNexusStagingRepoTest.class );
        suite.addTestSuite( MavenITmng4779MultipleDepsWithVersionRangeFromLocalRepoTest.class );
        suite.addTestSuite( MavenITmng4776ForkedReactorPluginVersionResolutionTest.class );
        suite.addTestSuite( MavenITmng4772PluginVersionResolutionDoesntTouchDisabledRepoTest.class );
        suite.addTestSuite( MavenITmng4771PluginPrefixResolutionDoesntTouchDisabledRepoTest.class );
        suite.addTestSuite( MavenITmng4768NearestMatchConflictResolutionTest.class );
        suite.addTestSuite( MavenITmng4765LocalPomProjectBuilderTest.class );
        suite.addTestSuite( MavenITmng4755FetchRemoteMetadataForVersionRangeTest.class );
        suite.addTestSuite( MavenITmng4750ResolvedMavenProjectDependencyArtifactsTest.class );
        suite.addTestSuite( MavenITmng4747JavaAgentUsedByPluginTest.class );
        suite.addTestSuite( MavenITmng4745PluginVersionUpdateTest.class );
        suite.addTestSuite( MavenITmng4729MirrorProxyAuthUsedByProjectBuilderTest.class );
        suite.addTestSuite( MavenITmng4721OptionalPluginDependencyTest.class );
        suite.addTestSuite( MavenITmng4720DependencyManagementExclusionMergeTest.class );
        suite.addTestSuite( MavenITmng4696MavenProjectDependencyArtifactsTest.class );
        suite.addTestSuite( MavenITmng4690InterdependentConflictResolutionTest.class );
        suite.addTestSuite( MavenITmng4684DistMgmtOverriddenByProfileTest.class );
        suite.addTestSuite( MavenITmng4679SnapshotUpdateInPluginTest.class );
        suite.addTestSuite( MavenITmng4677DisabledPluginConfigInheritanceTest.class );
        suite.addTestSuite( MavenITmng4666CoreRealmImportTest.class );
        suite.addTestSuite( MavenITmng4660ResumeFromTest.class );
        suite.addTestSuite( MavenITmng4660OutdatedPackagedArtifact.class );
        suite.addTestSuite( MavenITmng4654ArtifactHandlerForMainArtifactTest.class );
        suite.addTestSuite( MavenITmng4644StrictPomParsingRejectsMisplacedTextTest.class );
        suite.addTestSuite( MavenITmng4633DualCompilerExecutionsWeaveModeTest.class );
        suite.addTestSuite( MavenITmng4629NoPomValidationErrorUponMissingSystemDepTest.class );
        suite.addTestSuite( MavenITmng4625SettingsXmlInterpolationWithXmlMarkupTest.class );
        suite.addTestSuite( MavenITmng4618AggregatorBuiltAfterModulesTest.class );
        suite.addTestSuite( MavenITmng4615ValidateRequiredPluginParameterTest.class );
        suite.addTestSuite( MavenITmng4600DependencyOptionalFlagManagementTest.class );
        suite.addTestSuite( MavenITmng4590ImportedPomUsesSystemAndUserPropertiesTest.class );
        suite.addTestSuite( MavenITmng4586PluginPrefixResolutionFromVersionlessPluginMgmtTest.class );
        suite.addTestSuite( MavenITmng4580ProjectLevelPluginDepUsedForCliInvocInReactorTest.class );
        suite.addTestSuite( MavenITmng4572ModelVersionSurroundedByWhitespaceTest.class );
        suite.addTestSuite( MavenITmng4561MirroringOfPluginRepoTest.class );
        suite.addTestSuite( MavenITmng4555MetaversionResolutionOfflineTest.class );
        suite.addTestSuite( MavenITmng4554PluginPrefixMappingUpdateTest.class );
        suite.addTestSuite( MavenITmng4553CoreArtifactFilterConsidersGroupIdTest.class );
        suite.addTestSuite( MavenITmng4544ActiveComponentCollectionThreadSafeTest.class );
        suite.addTestSuite( MavenITmng4536RequiresNoProjectForkingMojoTest.class );
        suite.addTestSuite( MavenITmng4528ExcludeWagonsFromMavenCoreArtifactsTest.class );
        suite.addTestSuite( MavenITmng4526MavenProjectArtifactsScopeTest.class );
        suite.addTestSuite( MavenITmng4522FailUponMissingDependencyParentPomTest.class );
        suite.addTestSuite( MavenITmng4500NoUpdateOfTimestampedSnapshotsTest.class );
        suite.addTestSuite( MavenITmng4498IgnoreBrokenMetadataTest.class );
        suite.addTestSuite( MavenITmng4489MirroringOfExtensionRepoTest.class );
        suite.addTestSuite( MavenITmng4488ValidateExternalParenPomLenientTest.class );
        suite.addTestSuite( MavenITmng4482ForcePluginSnapshotUpdateTest.class );
        suite.addTestSuite( MavenITmng4474PerLookupWagonInstantiationTest.class );
        suite.addTestSuite( MavenITmng4470AuthenticatedDeploymentToProxyTest.class );
        suite.addTestSuite( MavenITmng4469AuthenticatedDeploymentToCustomRepoTest.class );
        suite.addTestSuite( MavenITmng4465PluginPrefixFromLocalCacheOfDownRepoTest.class );
        suite.addTestSuite( MavenITmng4464PlatformIndependentFileSeparatorTest.class );
        suite.addTestSuite( MavenITmng4461ArtifactUploadMonitorTest.class );
        suite.addTestSuite( MavenITmng4459InMemorySettingsKeptEncryptedTest.class );
        suite.addTestSuite( MavenITmng4453PluginVersionFromLifecycleMappingTest.class );
        suite.addTestSuite( MavenITmng4452ResolutionOfSnapshotWithClassifierTest.class );
        suite.addTestSuite( MavenITmng4450StubModelForMissingDependencyPomTest.class );
        suite.addTestSuite( MavenITmng4436SingletonComponentLookupTest.class );
        suite.addTestSuite( MavenITmng4433ForceParentSnapshotUpdateTest.class );
        suite.addTestSuite( MavenITmng4430DistributionManagementStatusTest.class );
        suite.addTestSuite( MavenITmng4429CompRequirementOnNonDefaultImplTest.class );
        suite.addTestSuite( MavenITmng4428FollowHttpRedirectTest.class );
        suite.addTestSuite( MavenITmng4423SessionDataFromPluginParameterExpressionTest.class );
        suite.addTestSuite( MavenITmng4422PluginExecutionPhaseInterpolationTest.class );
        suite.addTestSuite( MavenITmng4421DeprecatedPomInterpolationExpressionsTest.class );
        suite.addTestSuite( MavenITmng4416PluginOrderAfterProfileInjectionTest.class );
        suite.addTestSuite( MavenITmng4415InheritedPluginOrderTest.class );
        suite.addTestSuite( MavenITmng4413MirroringOfDependencyRepoTest.class );
        suite.addTestSuite( MavenITmng4412OfflineModeInPluginTest.class );
        suite.addTestSuite( MavenITmng4411VersionInfoTest.class );
        suite.addTestSuite( MavenITmng4410UsageHelpTest.class );
        suite.addTestSuite( MavenITmng4408NonExistentSettingsFileTest.class );
        suite.addTestSuite( MavenITmng4405ValidPluginVersionTest.class );
        suite.addTestSuite( MavenITmng4404UniqueProfileIdTest.class );
        suite.addTestSuite( MavenITmng4403LenientDependencyPomParsingTest.class );
        suite.addTestSuite( MavenITmng4402DuplicateChildModuleTest.class );
        suite.addTestSuite( MavenITmng4401RepositoryOrderForParentPomTest.class );
        suite.addTestSuite( MavenITmng4400RepositoryOrderTest.class );
        suite.addTestSuite( MavenITmng4396AntBased20xMojoSupportTest.class );
        suite.addTestSuite( MavenITmng4393ParseExternalParenPomLenientTest.class );
        suite.addTestSuite( MavenITmng4387QuietLoggingTest.class );
        suite.addTestSuite( MavenITmng4386DebugLoggingTest.class );
        suite.addTestSuite( MavenITmng4385LifecycleMappingFromExtensionInReactorTest.class );
        suite.addTestSuite( MavenITmng4383ValidDependencyVersionTest.class );
        suite.addTestSuite( MavenITmng4381ExtensionSingletonComponentTest.class );
        suite.addTestSuite( MavenITmng4379TransitiveSystemPathInterpolatedWithEnvVarTest.class );
        suite.addTestSuite( MavenITmng4368TimestampAwareArtifactInstallerTest.class );
        suite.addTestSuite( MavenITmng4367LayoutAwareMirrorSelectionTest.class );
        suite.addTestSuite( MavenITmng4365XmlMarkupInAttributeValueTest.class );
        suite.addTestSuite( MavenITmng4363DynamicAdditionOfDependencyArtifactTest.class );
        suite.addTestSuite( MavenITmng4361ForceDependencySnapshotUpdateTest.class );
        suite.addTestSuite( MavenITmng4360WebDavSupportTest.class );
        suite.addTestSuite( MavenITmng4359LocallyReachableParentOutsideOfReactorTest.class );
        suite.addTestSuite( MavenITmng4357LifecycleMappingDiscoveryInReactorTest.class );
        suite.addTestSuite( MavenITmng4355ExtensionAutomaticVersionResolutionTest.class );
        suite.addTestSuite( MavenITmng4353PluginDependencyResolutionFromPomRepoTest.class );
        suite.addTestSuite( MavenITmng4350LifecycleMappingExecutionOrderTest.class );
        suite.addTestSuite( MavenITmng4349RelocatedArtifactWithInvalidPomTest.class );
        suite.addTestSuite( MavenITmng4348NoUnnecessaryRepositoryAccessTest.class );
        suite.addTestSuite( MavenITmng4347ImportScopeWithSettingsProfilesTest.class );
        suite.addTestSuite( MavenITmng4345DefaultPluginExecutionOrderTest.class );
        suite.addTestSuite( MavenITmng4344ManagedPluginExecutionOrderTest.class );
        suite.addTestSuite( MavenITmng4343MissingReleaseUpdatePolicyTest.class );
        suite.addTestSuite( MavenITmng4342IndependentMojoParameterDefaultValuesTest.class );
        suite.addTestSuite( MavenITmng4341PluginExecutionOrderTest.class );
        suite.addTestSuite( MavenITmng4338OptionalMojosTest.class );
        suite.addTestSuite( MavenITmng4335SettingsOfflineModeTest.class );
        suite.addTestSuite( MavenITmng4332DefaultPluginExecutionOrderTest.class );
        suite.addTestSuite( MavenITmng4331DependencyCollectionTest.class );
        suite.addTestSuite( MavenITmng4328PrimitiveMojoParameterConfigurationTest.class );
        suite.addTestSuite( MavenITmng4327ExcludeForkingMojoFromForkedLifecycleTest.class );
        suite.addTestSuite( MavenITmng4326LocalSnapshotSuppressesRemoteCheckTest.class );
        suite.addTestSuite( MavenITmng4321CliUsesPluginMgmtConfigTest.class );
        suite.addTestSuite( MavenITmng4320AggregatorAndDependenciesTest.class );
        suite.addTestSuite( MavenITmng4319PluginExecutionGoalInterpolationTest.class );
        suite.addTestSuite( MavenITmng4318ProjectExecutionRootTest.class );
        suite.addTestSuite( MavenITmng4317PluginVersionResolutionFromMultiReposTest.class );
        suite.addTestSuite( MavenITmng4314DirectInvocationOfAggregatorTest.class );
        suite.addTestSuite( MavenITmng4312TypeAwarePluginParameterExpressionInjectionTest.class );
        suite.addTestSuite( MavenITmng4309StrictChecksumValidationForMetadataTest.class );
        suite.addTestSuite( MavenITmng4305LocalRepoBasedirTest.class );
        suite.addTestSuite( MavenITmng4304ProjectDependencyArtifactsTest.class );
        suite.addTestSuite( MavenITmng4293RequiresCompilePlusRuntimeScopeTest.class );
        suite.addTestSuite( MavenITmng4292EnumTypeMojoParametersTest.class );
        suite.addTestSuite( MavenITmng4291MojoRequiresOnlineModeTest.class );
        suite.addTestSuite( MavenITmng4283ParentPomPackagingTest.class );
        suite.addTestSuite( MavenITmng4281PreferLocalSnapshotTest.class );
        suite.addTestSuite( MavenITmng4276WrongTransitivePlexusUtilsTest.class );
        suite.addTestSuite( MavenITmng4275RelocationWarningTest.class );
        suite.addTestSuite( MavenITmng4274PluginRealmArtifactsTest.class );
        suite.addTestSuite( MavenITmng4273RestrictedCoreRealmAccessForPluginTest.class );
        suite.addTestSuite( MavenITmng4270ArtifactHandlersFromPluginDepsTest.class );
        suite.addTestSuite( MavenITmng4269BadReactorResolutionFromOutDirTest.class );
        suite.addTestSuite( MavenITmng4262MakeLikeReactorDottedPathTest.class );
        suite.addTestSuite( MavenITmng4262MakeLikeReactorDottedPath370Test.class );
        suite.addTestSuite( MavenITmng4238ArtifactHandlerExtensionUsageTest.class );
        suite.addTestSuite( MavenITmng4235HttpAuthDeploymentChecksumsTest.class );
        suite.addTestSuite( MavenITmng4233ReactorResolutionForManuallyCreatedArtifactTest.class );
        suite.addTestSuite( MavenITmng4231SnapshotUpdatePolicyTest.class );
        suite.addTestSuite( MavenITmng4214MirroredParentSearchReposTest.class );
        suite.addTestSuite( MavenITmng4208InterpolationPrefersCliOverProjectPropsTest.class );
        suite.addTestSuite( MavenITmng4207PluginWithLog4JTest.class );
        suite.addTestSuite( MavenITmng4203TransitiveDependencyExclusionTest.class );
        suite.addTestSuite( MavenITmng4199CompileMeetsRuntimeScopeTest.class );
        suite.addTestSuite( MavenITmng4196ExclusionOnPluginDepTest.class );
        suite.addTestSuite( MavenITmng4193UniqueRepoIdTest.class );
        suite.addTestSuite( MavenITmng4190MirrorRepoMergingTest.class );
        suite.addTestSuite( MavenITmng4189UniqueVersionSnapshotTest.class );
        suite.addTestSuite( MavenITmng4180PerDependencyExclusionsTest.class );
        suite.addTestSuite( MavenITmng4172EmptyDependencySetTest.class );
        suite.addTestSuite( MavenITmng4166HideCoreCommonsCliTest.class );
        suite.addTestSuite( MavenITmng4162ReportingMigrationTest.class );
        suite.addTestSuite( MavenITmng4150VersionRangeTest.class );
        suite.addTestSuite( MavenITmng4129PluginExecutionInheritanceTest.class );
        suite.addTestSuite( MavenITmng4116UndecodedUrlsTest.class );
        suite.addTestSuite( MavenITmng4112MavenVersionPropertyTest.class );
        suite.addTestSuite( MavenITmng4107InterpolationUsesDominantProfileSourceTest.class );
        suite.addTestSuite( MavenITmng4106InterpolationUsesDominantProfileTest.class );
        suite.addTestSuite( MavenITmng4102InheritedPropertyInterpolationTest.class );
        suite.addTestSuite( MavenITmng4091BadPluginDescriptorTest.class );
        suite.addTestSuite( MavenITmng4087PercentEncodedFileUrlTest.class );
        suite.addTestSuite( MavenITmng4072InactiveProfileReposTest.class );
        suite.addTestSuite( MavenITmng4070WhitespaceTrimmingTest.class );
        suite.addTestSuite( MavenITmng4068AuthenticatedMirrorTest.class );
        suite.addTestSuite( MavenITmng4056ClassifierBasedDepResolutionFromReactorTest.class );
        suite.addTestSuite( MavenITmng4053PluginConfigAttributesTest.class );
        suite.addTestSuite( MavenITmng4052ReactorAwareImportScopeTest.class );
        suite.addTestSuite( MavenITmng4048VersionRangeReactorResolutionTest.class );
        suite.addTestSuite( MavenITmng4040ProfileInjectedModulesTest.class );
        suite.addTestSuite( MavenITmng4036ParentResolutionFromSettingsRepoTest.class );
        suite.addTestSuite( MavenITmng4034ManagedProfileDependencyTest.class );
        suite.addTestSuite( MavenITmng4026ReactorDependenciesOrderTest.class );
        suite.addTestSuite( MavenITmng4023ParentProfileOneTimeInjectionTest.class );
        suite.addTestSuite( MavenITmng4022IdempotentPluginConfigMergingTest.class );
        suite.addTestSuite( MavenITmng4016PrefixedPropertyInterpolationTest.class );
        suite.addTestSuite( MavenITmng4009InheritProfileEffectsTest.class );
        suite.addTestSuite( MavenITmng4008MergedFilterOrderTest.class );
        suite.addTestSuite( MavenITmng4007PlatformFileSeparatorTest.class );
        suite.addTestSuite( MavenITmng4005UniqueDependencyKeyTest.class );
        suite.addTestSuite( MavenITmng4000MultiPluginExecutionsTest.class );
        suite.addTestSuite( MavenITmng3998PluginExecutionConfigTest.class );
        suite.addTestSuite( MavenITmng3991ValidDependencyScopeTest.class );
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
        suite.addTestSuite( MavenITmng3941ExecutionProjectRestrictedToForkingMojoTest.class );
        suite.addTestSuite( MavenITmng3940EnvVarInterpolationTest.class );
        suite.addTestSuite( MavenITmng3938MergePluginExecutionsTest.class );
        suite.addTestSuite( MavenITmng3937MergedPluginExecutionGoalsTest.class );
        suite.addTestSuite( MavenITmng3927PluginDefaultExecutionConfigTest.class );
        suite.addTestSuite( MavenITmng3925MergedPluginExecutionOrderTest.class );
        suite.addTestSuite( MavenITmng3924XmlMarkupInterpolationTest.class );
        suite.addTestSuite( MavenITmng3916PluginExecutionInheritanceTest.class );
        suite.addTestSuite( MavenITmng3906MergedPluginClassPathOrderingTest.class );
        suite.addTestSuite( MavenITmng3904NestedBuildDirInterpolationTest.class );
        suite.addTestSuite( MavenITmng3900ProfilePropertiesInterpolationTest.class );
        suite.addTestSuite( MavenITmng3899ExtensionInheritanceTest.class );
        suite.addTestSuite( MavenITmng3892ReleaseDeploymentTest.class );
        suite.addTestSuite( MavenITmng3887PluginExecutionOrderTest.class );
        suite.addTestSuite( MavenITmng3886ExecutionGoalsOrderTest.class );
        suite.addTestSuite( MavenITmng3877BasedirAlignedModelTest.class );
        suite.addTestSuite( MavenITmng3873MultipleExecutionGoalsTest.class );
        suite.addTestSuite( MavenITmng3872ProfileActivationInRelocatedPomTest.class );
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
        suite.addTestSuite( MavenITmng3814BogusProjectCycleTest.class );
        suite.addTestSuite( MavenITmng3813PluginClassPathOrderingTest.class );
        suite.addTestSuite( MavenITmng3811ReportingPluginConfigurationInheritanceTest.class );
        suite.addTestSuite( MavenITmng3810BadProfileActivationTest.class );
        suite.addTestSuite( MavenITmng3808ReportInheritanceOrderingTest.class );
        suite.addTestSuite( MavenITmng3807PluginConfigExpressionEvaluationTest.class );
        suite.addTestSuite( MavenITmng3805ExtensionClassPathOrderingTest.class );
        suite.addTestSuite( MavenITmng3796ClassImportInconsistencyTest.class );
        suite.addTestSuite( MavenITmng3775ConflictResolutionBacktrackingTest.class );
        suite.addTestSuite( MavenITmng3769ExclusionRelocatedTransdepsTest.class );
        suite.addTestSuite( MavenITmng3766ToolchainsFromExtensionTest.class );
        suite.addTestSuite( MavenITmng3748BadSettingsXmlTest.class );
        suite.addTestSuite( MavenITmng3747PrefixedPathExpressionTest.class );
        suite.addTestSuite( MavenITmng3746POMPropertyOverrideTest.class );
        suite.addTestSuite( MavenITmng3740SelfReferentialReactorProjectsTest.class );
        suite.addTestSuite( MavenITmng3732ActiveProfilesTest.class );
        suite.addTestSuite( MavenITmng3729MultiForkAggregatorsTest.class );
        suite.addTestSuite( MavenITmng3724ExecutionProjectSyncTest.class );
        suite.addTestSuite( MavenITmng3723ConcreteParentProjectTest.class );
        suite.addTestSuite( MavenITmng3719PomExecutionOrderingTest.class );
        suite.addTestSuite( MavenITmng3716AggregatorForkingTest.class );
        suite.addTestSuite( MavenITmng3714ToolchainsCliOptionTest.class );
        suite.addTestSuite( MavenITmng3710PollutedClonedPluginsTest.class );
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
        suite.addTestSuite( MavenITmng3645POMSyntaxErrorTest.class );
        suite.addTestSuite( MavenITmng3642DynamicResourcesTest.class );
        suite.addTestSuite( MavenITmng3641ProfileActivationWarningTest.class );
        suite.addTestSuite( MavenITmng3621UNCInheritedPathsTest.class );
        suite.addTestSuite( MavenITmng3607ClassLoadersUseValidUrlsTest.class );
        suite.addTestSuite( MavenITmng3600DeploymentModeDefaultsTest.class );
        suite.addTestSuite( MavenITmng3599useHttpProxyForWebDAVMk2Test.class );
        suite.addTestSuite( MavenITmng3586SystemScopePluginDependencyTest.class );
        suite.addTestSuite( MavenITmng3581PluginUsesWagonDependencyTest.class );
        suite.addTestSuite( MavenITmng3575HexadecimalOctalPluginParameterConfigTest.class );
        suite.addTestSuite( MavenITmng3545ProfileDeactivationTest.class );
        suite.addTestSuite( MavenITmng3536AppendedAbsolutePathsTest.class );
        suite.addTestSuite( MavenITmng3535SelfReferentialPropertiesTest.class );
        suite.addTestSuite( MavenITmng3529QuotedCliArgTest.class );
        suite.addTestSuite( MavenITmng3506ArtifactHandlersFromPluginsTest.class );
        suite.addTestSuite( MavenITmng3503Xpp3ShadingTest.class );
        suite.addTestSuite( MavenITmng3498ForkToOtherMojoTest.class );
        suite.addTestSuite( MavenITmng3485OverrideWagonExtensionTest.class );
        suite.addTestSuite( MavenITmng3482DependencyPomInterpolationTest.class );
        suite.addTestSuite( MavenITmng3477DependencyResolutionErrorMessageTest.class );
        suite.addTestSuite( MavenITmng3475BaseAlignedDirTest.class );
        suite.addTestSuite( MavenITmng3470StrictChecksumVerificationOfDependencyPomTest.class );
        suite.addTestSuite( MavenITmng3461MirrorMatchingTest.class );
        suite.addTestSuite( MavenITmng3441MetadataUpdatedFromDeploymentRepositoryTest.class );
        suite.addTestSuite( MavenITmng3422ActiveComponentCollectionTest.class );
        suite.addTestSuite( MavenITmng3415JunkRepositoryMetadataTest.class );
        suite.addTestSuite( MavenITmng3401CLIDefaultExecIdTest.class );
        suite.addTestSuite( MavenITmng3396DependencyManagementForOverConstrainedRangesTest.class );
        suite.addTestSuite( MavenITmng3394POMPluginVersionDominanceTest.class );
        suite.addTestSuite( MavenITmng3380ManagedRelocatedTransdepsTest.class );
        suite.addTestSuite( MavenITmng3379ParallelArtifactDownloadsTest.class );
        suite.addTestSuite( MavenITmng3372DirectInvocationOfPluginsTest.class );
        suite.addTestSuite( MavenITmng3355TranslatedPathInterpolationTest.class );
        suite.addTestSuite( MavenITmng3331ModulePathNormalizationTest.class );
        suite.addTestSuite( MavenITmng3314OfflineSnapshotsTest.class );
        suite.addTestSuite( MavenITmng3297DependenciesNotLeakedToMojoTest.class );
        suite.addTestSuite( MavenITmng3288SystemScopeDirTest.class );
        suite.addTestSuite( MavenITmng3284UsingCachedPluginsTest.class );
        suite.addTestSuite( MavenITmng3268MultipleHyphenPCommandLineTest.class );
        suite.addTestSuite( MavenITmng3259DepsDroppedInMultiModuleBuildTest.class );
        suite.addTestSuite( MavenITmng3220ImportScopeTest.class );
        suite.addTestSuite( MavenITmng3217InterPluginDependencyTest.class );
        suite.addTestSuite( MavenITmng3208ProfileAwareReactorSortingTest.class );
        suite.addTestSuite( MavenITmng3203DefaultLifecycleExecIdTest.class );
        suite.addTestSuite( MavenITmng3183LoggingToFileTest.class );
        suite.addTestSuite( MavenITmng3139UseCachedMetadataOfBlacklistedRepoTest.class );
        suite.addTestSuite( MavenITmng3133UrlNormalizationNotBeforeInterpolationTest.class );
        suite.addTestSuite( MavenITmng3122ActiveProfilesNoDuplicatesTest.class );
        suite.addTestSuite( MavenITmng3118TestClassPathOrderTest.class );
        suite.addTestSuite( MavenITmng3099SettingsProfilesWithNoPomTest.class );
        suite.addTestSuite( MavenITmng3052DepRepoAggregationTest.class );
        suite.addTestSuite( MavenITmng3043BestEffortReactorResolutionTest.class );
        suite.addTestSuite( MavenITmng3023ReactorDependencyResolutionTest.class );
        suite.addTestSuite( MavenITmng3012CoreClassImportTest.class );
        suite.addTestSuite( MavenITmng3004ReactorFailureBehaviorMultithreadedTest.class );
        suite.addTestSuite( MavenITmng2994SnapshotRangeRepositoryTest.class );
        suite.addTestSuite( MavenITmng2972OverridePluginDependencyTest.class );
        suite.addTestSuite( MavenITmng2926PluginPrefixOrderTest.class );
        suite.addTestSuite( MavenITmng2921ActiveAttachedArtifactsTest.class );
        suite.addTestSuite( MavenITmng2892HideCorePlexusUtilsTest.class );
        suite.addTestSuite( MavenITmng2871PrePackageSubartifactResolutionTest.class );
        suite.addTestSuite( MavenITmng2865MirrorWildcardTest.class );
        suite.addTestSuite( MavenITmng2861RelocationsAndRangesTest.class );
        suite.addTestSuite( MavenITmng2848ProfileActivationByEnvironmentVariableTest.class );
        suite.addTestSuite( MavenITmng2843PluginConfigPropertiesInjectionTest.class );
        suite.addTestSuite( MavenITmng2831CustomArtifactHandlerAndCustomLifecycleTest.class );
        suite.addTestSuite( MavenITmng2820PomCommentsTest.class );
        suite.addTestSuite( MavenITmng2790LastUpdatedMetadataTest.class );
        suite.addTestSuite( MavenITmng2749ExtensionAvailableToPluginTest.class );
        suite.addTestSuite( MavenITmng2744checksumVerificationTest.class );
        suite.addTestSuite( MavenITmng2741PluginMetadataResolutionErrorMessageTest.class );
        suite.addTestSuite( MavenITmng2739RequiredRepositoryElementsTest.class );
        suite.addTestSuite( MavenITmng2738ProfileIdCollidesWithCliOptionTest.class );
        suite.addTestSuite( MavenITmng2720SiblingClasspathArtifactsTest.class );
        suite.addTestSuite( MavenITmng2695OfflinePluginSnapshotsTest.class );
        suite.addTestSuite( MavenITmng2693SitePluginRealmTest.class );
        suite.addTestSuite( MavenITmng2690MojoLoadingErrorsTest.class );
        suite.addTestSuite( MavenITmng2668UsePluginDependenciesForSortingTest.class );
        suite.addTestSuite( MavenITmng2605BogusProfileActivationTest.class );
        suite.addTestSuite( MavenITmng2591MergeInheritedPluginConfigTest.class );
        suite.addTestSuite( MavenITmng2577SettingsXmlInterpolationTest.class );
        suite.addTestSuite( MavenITmng2576MakeLikeReactorTest.class );
        suite.addTestSuite( MavenITmng2562Timestamp322Test.class );
        suite.addTestSuite( MavenITmng2486TimestampedDependencyVersionInterpolationTest.class );
        suite.addTestSuite( MavenITmng2432PluginPrefixOrderTest.class );
        suite.addTestSuite( MavenITmng2387InactiveProxyTest.class );
        suite.addTestSuite( MavenITmng2363BasedirAwareFileActivatorTest.class );
        suite.addTestSuite( MavenITmng2362DeployedPomEncodingTest.class );
        suite.addTestSuite( MavenITmng2339BadProjectInterpolationTest.class );
        suite.addTestSuite( MavenITmng2318LocalParentResolutionTest.class );
        suite.addTestSuite( MavenITmng2309ProfileInjectionOrderTest.class );
        suite.addTestSuite( MavenITmng2305MultipleProxiesTest.class );
        suite.addTestSuite( MavenITmng2277AggregatorAndResolutionPluginsTest.class );
        suite.addTestSuite( MavenITmng2276ProfileActivationBySettingsPropertyTest.class );
        suite.addTestSuite( MavenITmng2254PomEncodingTest.class );
        suite.addTestSuite( MavenITmng2234ActiveProfilesFromSettingsTest.class );
        suite.addTestSuite( MavenITmng2228ComponentInjectionTest.class );
        suite.addTestSuite( MavenITmng2222OutputDirectoryReactorResolutionTest.class );
        suite.addTestSuite( MavenITmng2201PluginConfigInterpolationTest.class );
        suite.addTestSuite( MavenITmng2199ParentVersionRangeTest.class );
        suite.addTestSuite( MavenITmng2196ParentResolutionTest.class );
        suite.addTestSuite( MavenITmng2174PluginDepsManagedByParentProfileTest.class );
        suite.addTestSuite( MavenITmng2140ReactorAwareDepResolutionWhenForkTest.class );
        suite.addTestSuite( MavenITmng2136ActiveByDefaultProfileTest.class );
        suite.addTestSuite( MavenITmng2135PluginBuildInReactorTest.class );
        suite.addTestSuite( MavenITmng2130ParentLookupFromReactorCacheTest.class );
        suite.addTestSuite( MavenITmng2124PomInterpolationWithParentValuesTest.class );
        suite.addTestSuite( MavenITmng2123VersionRangeDependencyTest.class );
        suite.addTestSuite( MavenITmng2103PluginExecutionInheritanceTest.class );
        suite.addTestSuite( MavenITmng2098VersionRangeSatisfiedFromWrongRepoTest.class );
        suite.addTestSuite( MavenITmng2068ReactorRelativeParentsTest.class );
        suite.addTestSuite( MavenITmng2054PluginExecutionInheritanceTest.class );
        suite.addTestSuite( MavenITmng2052InterpolateWithSettingsProfilePropertiesTest.class );
        suite.addTestSuite( MavenITmng2045testJarDependenciesBrokenInReactorTest.class );
        suite.addTestSuite( MavenITmng2006ChildPathAwareUrlInheritanceTest.class );
        suite.addTestSuite( MavenITmng1995InterpolateBooleanModelElementsTest.class );
        suite.addTestSuite( MavenITmng1992SystemPropOverridesPomPropTest.class );
        suite.addTestSuite( MavenITmng1957JdkActivationWithVersionRangeTest.class );
        suite.addTestSuite( MavenITmng1895ScopeConflictResolutionTest.class );
        suite.addTestSuite( MavenITmng1803PomValidationErrorIncludesLineNumberTest.class );
        suite.addTestSuite( MavenITmng1751ForcedMetadataUpdateDuringDeploymentTest.class );
        suite.addTestSuite( MavenITmng1703PluginMgmtDepInheritanceTest.class );
        suite.addTestSuite( MavenITmng1701DuplicatePluginTest.class );
        suite.addTestSuite( MavenITmng1493NonStandardModulePomNamesTest.class );
        suite.addTestSuite( MavenITmng1491ReactorArtifactIdCollisionTest.class );
        suite.addTestSuite( MavenITmng1415QuotedSystemPropertiesTest.class );
        suite.addTestSuite( MavenITmng1412DependenciesOrderTest.class );
        suite.addTestSuite( MavenITmng1349ChecksumFormatsTest.class );
        suite.addTestSuite( MavenITmng1323AntrunDependenciesTest.class );
        suite.addTestSuite( MavenITmng1233WarDepWithProvidedScopeTest.class );
        suite.addTestSuite( MavenITmng1144MultipleDefaultGoalsTest.class );
        suite.addTestSuite( MavenITmng1142VersionRangeIntersectionTest.class );
        suite.addTestSuite( MavenITmng1088ReactorPluginResolutionTest.class );
        suite.addTestSuite( MavenITmng1073AggregatorForksReactorTest.class );
        suite.addTestSuite( MavenITmng1052PluginMgmtConfigTest.class );
        suite.addTestSuite( MavenITmng1021EqualAttachmentBuildNumberTest.class );
        suite.addTestSuite( MavenITmng0985NonExecutedPluginMgmtGoalsTest.class );
        suite.addTestSuite( MavenITmng0956ComponentInjectionViaProjectLevelPluginDepTest.class );
        suite.addTestSuite( MavenITmng0947OptionalDependencyTest.class );
        suite.addTestSuite( MavenITmng0870ReactorAwarePluginDiscoveryTest.class );
        suite.addTestSuite( MavenITmng0866EvaluateDefaultValueTest.class );
        suite.addTestSuite( MavenITmng0848UserPropertyOverridesDefaultValueTest.class );
        suite.addTestSuite( MavenITmng0836PluginParentResolutionTest.class );
        suite.addTestSuite( MavenITmng0828PluginConfigValuesInDebugTest.class );
        suite.addTestSuite( MavenITmng0823MojoContextPassingTest.class );
        suite.addTestSuite( MavenITmng0820ConflictResolutionTest.class );
        suite.addTestSuite( MavenITmng0818WarDepsNotTransitiveTest.class );
        suite.addTestSuite( MavenITmng0814ExplicitProfileActivationTest.class );
        suite.addTestSuite( MavenITmng0786ProfileAwareReactorTest.class );
        suite.addTestSuite( MavenITmng0781PluginConfigVsExecConfigTest.class );
        suite.addTestSuite( MavenITmng0773SettingsProfileReactorPollutionTest.class );
        suite.addTestSuite( MavenITmng0768OfflineModeTest.class );
        suite.addTestSuite( MavenITmng0761MissingSnapshotDistRepoTest.class );
        suite.addTestSuite( MavenITmng0680ParentBasedirTest.class );
        suite.addTestSuite( MavenITmng0674PluginParameterAliasTest.class );
        suite.addTestSuite( MavenITmng0666IgnoreLegacyPomTest.class );
        suite.addTestSuite( MavenITmng0557UserSettingsCliOptionTest.class );
        suite.addTestSuite( MavenITmng0553SettingsAuthzEncryptionTest.class );
        suite.addTestSuite( MavenITmng0522InheritedPluginMgmtConfigTest.class );
        suite.addTestSuite( MavenITmng0507ArtifactRelocationTest.class );
        suite.addTestSuite( MavenITmng0505VersionRangeTest.class );
        suite.addTestSuite( MavenITmng0496IgnoreUnknownPluginParametersTest.class );
        suite.addTestSuite( MavenITmng0479OverrideCentralRepoTest.class );
        suite.addTestSuite( MavenITmng0471CustomLifecycleTest.class );
        suite.addTestSuite( MavenITmng0469ReportConfigTest.class );
        suite.addTestSuite( MavenITmng0461TolerateMissingDependencyPomTest.class );
        suite.addTestSuite( MavenITmng0449PluginVersionResolutionTest.class );
        suite.addTestSuite( MavenITmng0377PluginLookupFromPrefixTest.class );
        suite.addTestSuite( MavenITmng0294MergeGlobalAndUserSettingsTest.class );
        suite.addTestSuite( MavenITmng0282NonReactorExecWhenProjectIndependentTest.class );
        suite.addTestSuite( MavenITmng0249ResolveDepsFromReactorTest.class );
        suite.addTestSuite( MavenITmng0187CollectedProjectsTest.class );
        suite.addTestSuite( MavenITmng0095ReactorFailureBehaviorTest.class );
        suite.addTestSuite( MavenIT0199CyclicImportScopeTest.class );
        suite.addTestSuite( MavenIT0146InstallerSnapshotNaming.class );
        suite.addTestSuite( MavenIT0144LifecycleExecutionOrderTest.class );
        suite.addTestSuite( MavenIT0143TransitiveDependencyScopesTest.class );
        suite.addTestSuite( MavenIT0142DirectDependencyScopesTest.class );
        suite.addTestSuite( MavenIT0140InterpolationWithPomPrefixTest.class );
        suite.addTestSuite( MavenIT0139InterpolationWithProjectPrefixTest.class );
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
        suite.addTestSuite( MavenIT0090EnvVarInterpolationTest.class );
        suite.addTestSuite( MavenIT0087PluginRealmWithProjectLevelDepsTest.class );
        suite.addTestSuite( MavenIT0086PluginRealmTest.class );
        suite.addTestSuite( MavenIT0085TransitiveSystemScopeTest.class );
        suite.addTestSuite( MavenIT0072InterpolationWithDottedPropertyTest.class );
        suite.addTestSuite( MavenIT0071PluginConfigWithDottedPropertyTest.class );
        suite.addTestSuite( MavenIT0064MojoConfigViaSettersTest.class );
        suite.addTestSuite( MavenIT0063SystemScopeDependencyTest.class );
        suite.addTestSuite( MavenIT0056MultipleGoalExecutionsTest.class );
        suite.addTestSuite( MavenIT0052ReleaseProfileTest.class );
        suite.addTestSuite( MavenIT0051ReleaseProfileTest.class );
        suite.addTestSuite( MavenIT0041ArtifactTypeFromPluginExtensionTest.class );
        suite.addTestSuite( MavenIT0040PackagingFromPluginExtensionTest.class );
        suite.addTestSuite( MavenIT0038AlternatePomFileDifferentDirTest.class );
        suite.addTestSuite( MavenIT0037AlternatePomFileSameDirTest.class );
        suite.addTestSuite( MavenIT0032MavenPrerequisiteTest.class );
        suite.addTestSuite( MavenIT0030DepPomDepMgmtInheritanceTest.class );
        suite.addTestSuite( MavenIT0025MultipleExecutionLevelConfigsTest.class );
        suite.addTestSuite( MavenIT0024MultipleGoalExecutionsTest.class );
        suite.addTestSuite( MavenIT0023SettingsProfileTest.class );
        suite.addTestSuite( MavenIT0021PomProfileTest.class );
        suite.addTestSuite( MavenIT0019PluginVersionMgmtBySuperPomTest.class );
        suite.addTestSuite( MavenIT0018DependencyManagementTest.class );
        suite.addTestSuite( MavenIT0012PomInterpolationTest.class );
        suite.addTestSuite( MavenIT0011DefaultVersionByDependencyManagementTest.class );
        suite.addTestSuite( MavenIT0010DependencyClosureResolutionTest.class );
        suite.addTestSuite( MavenIT0009GoalConfigurationTest.class );
        suite.addTestSuite( MavenIT0008SimplePluginTest.class );
        /*
         * Add tests in reverse alpha order above.
         */
    }

    void addTestSuite( Class<?> clazz )
    {
        addTestSuite( clazz, tests.size() );
    }

    void addTestSuite( Class<?> clazz, int order )
    {
        tests.put( clazz, order );
    }

    int getIndex( ClassDescriptor cd )
    {
        Integer i = tests.get( cd.getTestClass() );
        return i != null ? i : -1;
    }

    public void orderClasses( ClassOrdererContext context )
    {
        context.getClassDescriptors().stream()
            .filter( cd -> !MISSED_TESTS.contains( cd.getTestClass() ) )
            .filter( cd -> getIndex( cd ) == -1 )
            .filter( cd -> cd.findRepeatableAnnotations( Tag.class ).stream()
                .noneMatch( t -> "disabled".equals( t.value() ) ) )
            .forEach( cd -> {
                out.println( "Test " + cd.getTestClass()
                                 + " is not present in TestSuiteOrdering " + System.lineSeparator()
                                 + "\t- please add it or annotate with @Tag(\"disabled\")" + System.lineSeparator() );
                MISSED_TESTS.add( cd.getTestClass() );
            } );

        context.getClassDescriptors().sort( Comparator.comparing( this::getIndex ) );
    }

}
