package org.apache.maven.integrationtests;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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
         * Add tests in reverse alpha order by number below. This makes testing new
         * ITs quicker and since it counts down to zero, it's easier to judge how close
         * the tests are to finishing. Newer tests are also more likely to fail, so this is
         * a fail fast technique as well.
         */

        suite.addTestSuite( MavenITmng3545ProfileDeactivation.class );
        suite.addTestSuite( MavenITmng3498ForkToOtherMojoTest.class );
        suite.addTestSuite( MavenITmng3485OverrideWagonExtensionTest.class );
        suite.addTestSuite( MavenITmng3482DependencyPomInterpolationTest.class );
        suite.addTestSuite( MavenITmng3473PluginReportCrash.class );
        suite.addTestSuite( MavenITmng3428PluginDescriptorArtifactsIncompleteTest.class );
        suite.addTestSuite( MavenITmng3426PluginsClasspathOverrideTest.class );
        suite.addTestSuite( MavenITmng3396DependencyManagementForOverConstrainedRanges.class );
        suite.addTestSuite( MavenITmng3394POMPluginVersionDominanceTest.class );

        // Pending resolution of MNG-3391
//        suite.addTestSuite( MavenITmng3391ImportScopeErrorScenariosTest.class );

        suite.addTestSuite( MavenITmng3372DirectInvocationOfPlugins.class );
        suite.addTestSuite( MavenITmng3355TranslatedPathInterpolationTest.class );
        suite.addTestSuite( MavenITmng3341MetadataUpdatedFromDeploymentRepositoryTest.class );
        suite.addTestSuite( MavenITmng3331ModulePathNormalization.class );
        suite.addTestSuite( MavenITmng3221InfiniteForking.class );
        suite.addTestSuite( MavenITmng3268MultipleDashPCommandLine.class );
        suite.addTestSuite( MavenITmng3220ImportScopeTest.class );
        suite.addTestSuite( MavenITmng3099SettingsProfilesWithNoPOM.class );
        suite.addTestSuite( MavenITmng2972OverridePluginDependency.class );
        suite.addTestSuite( MavenITmng2861RelocationsAndRanges.class );
        suite.addTestSuite( MavenITmng2744checksumVerificationTest.class );
        suite.addTestSuite( MavenITmng2339BadProjectInterpolationTest.class );
        suite.addTestSuite( MavenITmng2277AggregatorAndResolutionPluginsTest.class );
        suite.addTestSuite( MavenITmng2254PomEncodingTest.class );
        suite.addTestSuite( MavenITmng2234ActiveProfilesFromSettingsTest.class );
        suite.addTestSuite( MavenITmng2123VersionRangeDependencyTest.class );
        suite.addTestSuite( MavenITmng2045testJarDependenciesBrokenInReactorTest.class );
        suite.addTestSuite( MavenITmng1493NonStandardModulePomNames.class );
        suite.addTestSuite( MavenITmng1491ReactorArtifactIdCollision.class );
        suite.addTestSuite( MavenITmng1412DependenciesOrderTest.class );
        suite.addTestSuite( MavenIT0129ResourceProvidedToAPluginAsAPluginDependency.class );
        suite.addTestSuite( MavenIT0119PluginPrefixOrder.class );
        suite.addTestSuite( MavenIT0118AttachedArtifactsInReactor.class );
        suite.addTestSuite( MavenIT0115CustomArtifactHandlerAndCustomLifecycleTest.class );
        suite.addTestSuite( MavenIT0114ExtensionThatProvidesResources.class );
        suite.addTestSuite( MavenIT0113ServerAuthzAvailableToWagonMgrInPlugin.class );
        suite.addTestSuite( MavenIT0112ExtensionsThatDragDependencies.class );
        suite.addTestSuite( MavenIT0111PluginsThatRequireAResourceFromAnExtensionTest.class );
        suite.addTestSuite( MavenIT0110PluginDependenciesComeFromPluginReposTest.class );
        suite.addTestSuite( MavenIT0107Test.class );
        suite.addTestSuite( MavenIT0105Test.class );
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
        suite.addTestSuite( MavenIT0094Test.class );
        suite.addTestSuite( MavenIT0092Test.class );
        suite.addTestSuite( MavenIT0090Test.class );
        suite.addTestSuite( MavenIT0089Test.class );
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
        suite.addTestSuite( MavenIT0070Test.class );
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
        suite.addTestSuite( MavenIT0055Test.class );
        suite.addTestSuite( MavenIT0054Test.class );
        suite.addTestSuite( MavenIT0053Test.class );
        suite.addTestSuite( MavenIT0052Test.class );
        suite.addTestSuite( MavenIT0051Test.class );
        suite.addTestSuite( MavenIT0050Test.class );
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
        suite.addTestSuite( MavenIT0033Test.class );
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

        suite.addTestSuite( MavenIT0017Test.class );
        suite.addTestSuite( MavenIT0016Test.class );
        suite.addTestSuite( MavenIT0014Test.class );
        suite.addTestSuite( MavenIT0013Test.class );
        suite.addTestSuite( MavenIT0012Test.class );
        suite.addTestSuite( MavenIT0011Test.class );
        suite.addTestSuite( MavenIT0010Test.class );
        suite.addTestSuite( MavenIT0009Test.class );
        suite.addTestSuite( MavenIT0008Test.class );
        suite.addTestSuite( MavenIT0007Test.class );
        suite.addTestSuite( MavenIT0006Test.class );
        suite.addTestSuite( MavenIT0005Test.class );
        suite.addTestSuite( MavenIT0004Test.class );
        suite.addTestSuite( MavenIT0003Test.class );
        suite.addTestSuite( MavenIT0002Test.class );
        suite.addTestSuite( MavenIT0001Test.class );
        suite.addTestSuite( MavenIT0000Test.class );

        /*
         * Add tests in reverse alpha order above.
         */

        // not fixed in the code yet. Test is correct.
        // suite.addTestSuite( MavenITmng3284UsingCachedPluginsTest.class );

        // ----------------------------------------------------------------------------------------------------
        // Tests that need to be fixed.
        // ----------------------------------------------------------------------------------------------------
        /*
         *Test 18 always fails because it is trying to delete a
         *commonly used artifact (commons-logging-1.0.3) that is in use
         *in the repo. It should be redone using fake artifacts.
         */
        //suite.addTestSuite( MavenIT0018Test.class );

        //this test is flakey on windows and isn't a test of the core.
        //   suite.addTestSuite( MavenIT0020Test.class );

        // suite.addTestSuite(MavenIT0091Test.class);
        // suite.addTestSuite(MavenIT0106Test.class);
        // suite.addTestSuite( MavenIT0108SnapshotUpdateTest.class ); -- MNG-3158
        // suite.addTestSuite( MavenIT0121TransitiveDepManVersion.class ); -- MNG-3038
        // suite.addTestSuite( MavenIT0122ReactorDependencyResolutionTest.class ); -- MNG-3023
        // suite.addTestSuite( MavenIT0123SnapshotRangeRepositoryTest.class ); -- MNG-2994
        // suite.addTestSuite( MavenIT0124PomExtensionComponentOverrideTest.class ); -- MNG-2771
        // suite.addTestSuite( MavenIT0126TestJarDependency.class ); // MJAR-75 / MNG-3160
        // suite.addTestSuite( MavenIT0120EjbClientDependency.class ); // -- not passing for 2.0.7 either, looks to be
        // 2.1+ ?

        return suite;
    }
}
