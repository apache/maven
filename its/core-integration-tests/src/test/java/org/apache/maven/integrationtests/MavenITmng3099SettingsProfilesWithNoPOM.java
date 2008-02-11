package org.apache.maven.integrationtests;

import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is a sample integration test. The IT tests typically
 * operate by having a sample project in the
 * /src/test/resources folder along with a junit test like
 * this one. The junit test uses the verifier (which uses
 * the invoker) to invoke a new instance of Maven on the
 * project in the resources folder. It then checks the
 * results. This is a non-trivial example that shows two
 * phases. See more information inline in the code.
 *
 * @author <a href="mailto:brianf@apache.org">Brian Fox</a>
 *
 */
public class MavenITmng3099SettingsProfilesWithNoPOM
    extends AbstractMavenIntegrationTestCase
{
    public MavenITmng3099SettingsProfilesWithNoPOM()
        throws InvalidVersionSpecificationException
    {
        super( "(2.0.8,)" ); // 2.0.9+
    }

    public void testitMNG3099 ()
        throws Exception
    {
        // The testdir is computed from the location of this
        // file.
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/mng-3099-settingsProfilesWithNoPOM" );

        File plugin = new File( testDir, "plugin" );

        Verifier verifier;

        verifier = new Verifier( plugin.getAbsolutePath() );

        verifier.executeGoal( "install" );

        /*
         * Reset the streams before executing the verifier
         * again.
         */
        verifier.resetStreams();

        verifier = new Verifier( testDir.getAbsolutePath() );

        /*
         * Use the settings for this test, which contains the profile we're looking for.
         */
        List cliOptions = new ArrayList();
        cliOptions.add( "-s" );
        cliOptions.add( new File( testDir, "settings.xml" ).getAbsolutePath() );

        verifier.setCliOptions( cliOptions );

        verifier.setAutoclean( false );
        verifier.executeGoal( "org.apache.maven.its.mng3099:maven-mng3099-plugin:1:profile-props" );


        List lines = verifier.loadFile( new File( testDir, "log.txt" ), false );
        boolean found = false;
        for ( Iterator it = lines.iterator(); it.hasNext(); )
        {
            String line = (String) it.next();
            if ( line.indexOf( "local-profile-prop=local-profile-prop-value" ) > -1 )
            {
                found = true;
                break;
            }
        }

        if ( !found )
        {
            fail( "Profile-injected property value: local-profile-prop-value was not found in log output." );
        }
    }
}
