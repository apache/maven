package org.apache.maven.util;

import org.apache.maven.model.user.JdkProfile;
import org.apache.maven.model.user.MavenProfile;
import org.apache.maven.model.user.ServerProfile;
import org.apache.maven.model.user.UserModel;
import org.apache.maven.model.user.io.xpp3.MavenUserModelXpp3Reader;
import org.apache.maven.model.user.io.xpp3.MavenUserModelXpp3Writer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * @author jdcasey
 */
public final class UserModelUtils
{

    public static final String JDK_PROFILE_ENVAR = "maven.jdkVersion";

    public static final String MAVEN_PROFILE_ENVAR = "maven.mavenProfileId";

    public static final String SERVER_PROFILE_ENVAR = "maven.serverProfileId";

    private static final String USER_MODEL_LOCATION = "/.m2/user.xml";

    private UserModelUtils()
    {
    }

    public static ServerProfile getActiveServer( UserModel userModel )
    {
        List servers = userModel.getServerProfiles();

        String serverId = System.getProperty( SERVER_PROFILE_ENVAR );
        if ( serverId == null || serverId.trim().length() < 1 )
        {
            serverId = userModel.getDefaultProfiles().getServerProfileId();
        }

        ServerProfile active = null;
        for ( Iterator it = servers.iterator(); it.hasNext(); )
        {
            ServerProfile server = (ServerProfile) it.next();
            if ( serverId.equals( server.getId() ) )
            {
                active = server;
                break;
            }
        }

        return active;
    }

    public static JdkProfile getActiveJdk( UserModel userModel )
    {
        List jdks = userModel.getJdkProfiles();

        String jdkId = System.getProperty( JDK_PROFILE_ENVAR );
        if ( jdkId == null || jdkId.trim().length() < 1 )
        {
            jdkId = userModel.getDefaultProfiles().getJdkVersion();
        }

        JdkProfile active = null;
        for ( Iterator it = jdks.iterator(); it.hasNext(); )
        {
            JdkProfile jdk = (JdkProfile) it.next();
            if ( jdkId.equals( jdk.getVersion() ) )
            {
                active = jdk;
                break;
            }
        }

        return active;
    }

    public static MavenProfile getActiveRuntimeProfile( UserModel userModel )
    {
        List mavenProfiles = userModel.getMavenProfiles();

        String mavenProfileId = System.getProperty( MAVEN_PROFILE_ENVAR );
        if ( mavenProfileId == null || mavenProfileId.trim().length() < 1 )
        {
            mavenProfileId = userModel.getDefaultProfiles().getMavenProfileId();
        }

        MavenProfile active = null;
        for ( Iterator it = mavenProfiles.iterator(); it.hasNext(); )
        {
            MavenProfile mavenProfile = (MavenProfile) it.next();
            if ( mavenProfileId.equals( mavenProfile.getId() ) )
            {
                active = mavenProfile;
                break;
            }
        }

        return active;
    }

    // TODO: don't throw Exception.
    public static UserModel getUserModel() throws Exception
    {
        UserModel model = null;

        File modelFile = getUserModelFile();
        if ( modelFile.exists() && modelFile.isFile() )
        {
            MavenUserModelXpp3Reader modelReader = new MavenUserModelXpp3Reader();
            FileReader reader = null;
            try
            {
                reader = new FileReader( modelFile );

                model = modelReader.read( reader );
            }
            finally
            {
                if ( reader != null )
                {
                    try
                    {
                        reader.close();
                    }
                    catch ( IOException e )
                    {
                    }
                }
            }
        }

        return model;
    }

    // TODO: don't throw Exception.
    public static void setUserModel( UserModel userModel ) throws Exception
    {
        File modelFile = getUserModelFile();

        File modelDir = modelFile.getParentFile();
        if ( !modelDir.exists() )
        {
            modelDir.mkdirs();
        }

        MavenUserModelXpp3Writer modelWriter = new MavenUserModelXpp3Writer();

        FileWriter writer = null;
        try
        {
            writer = new FileWriter( modelFile );

            modelWriter.write( writer, userModel );

            writer.flush();
        }
        finally
        {
            if ( writer != null )
            {
                try
                {
                    writer.close();
                }
                catch ( IOException e )
                {
                }
            }
        }

    }

    private static File getUserModelFile()
    {
        String userDir = System.getProperty( "user.home" );

        String modelPath = userDir + USER_MODEL_LOCATION;

        modelPath = modelPath.replaceAll( "\\\\", "/" );
        modelPath = modelPath.replaceAll( "//", "/" );

        File userModelFile = new File( modelPath );

        return userModelFile;
    }

}