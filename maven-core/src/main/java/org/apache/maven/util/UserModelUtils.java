package org.apache.maven.util;

import org.apache.maven.model.user.DefaultProfiles;
import org.apache.maven.model.user.MavenProfile;
import org.apache.maven.model.user.ProxyProfile;
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

    private static final String USER_MODEL_LOCATION = "/.m2/user.xml";

    private static final String ACTIVE_MAVEN_PROFILE_ID_ENVAR = "maven.profile";

    private UserModelUtils()
    {
    }

    public static MavenProfile getActiveMavenProfile( UserModel userModel )
    {
        String activeProfileId = System.getProperty( ACTIVE_MAVEN_PROFILE_ID_ENVAR );
        if ( activeProfileId == null || activeProfileId.trim().length() < 1 )
        {
            DefaultProfiles defaults = userModel.getDefaultProfiles();
            if ( defaults != null )
            {
                activeProfileId = defaults.getMavenProfileId();
            }
        }

        MavenProfile activeProfile = null;

        if ( activeProfileId != null && activeProfileId.trim().length() > 0 )
        {
            activeProfile = UserModelUtils.getMavenProfile( userModel, activeProfileId );
        }

        return activeProfile;
    }

    public static ProxyProfile getActiveProxyProfile( UserModel userModel )
    {
        String activeProfileId = System.getProperty( ACTIVE_MAVEN_PROFILE_ID_ENVAR );
        if ( activeProfileId == null || activeProfileId.trim().length() < 1 )
        {
            DefaultProfiles defaults = userModel.getDefaultProfiles();
            if ( defaults != null )
            {
                activeProfileId = defaults.getProxyProfileId();
            }
        }

        ProxyProfile activeProfile = null;

        if ( activeProfileId != null && activeProfileId.trim().length() > 0 )
        {
            activeProfile = UserModelUtils.getProxyProfile( userModel, activeProfileId );
        }

        return activeProfile;
    }

    public static MavenProfile getMavenProfile( UserModel userModel, String mavenProfileId )
    {
        MavenProfile result = null;

        List mavenProfiles = userModel.getMavenProfiles();
        if ( mavenProfiles != null )
        {
            for ( Iterator it = mavenProfiles.iterator(); it.hasNext(); )
            {
                MavenProfile profile = (MavenProfile) it.next();
                if ( mavenProfileId.equals( profile.getId() ) )
                {
                    result = profile;
                    break;
                }
            }
        }

        return result;
    }

    public static ProxyProfile getProxyProfile( UserModel userModel, String proxyProfileId )
    {
        ProxyProfile result = null;

        List proxyProfile = userModel.getProxyProfiles();
        if ( proxyProfile != null )
        {
            for ( Iterator it = proxyProfile.iterator(); it.hasNext(); )
            {
                ProxyProfile profile = (ProxyProfile) it.next();
                if ( proxyProfileId.equals( profile.getId() ) )
                {
                    result = profile;
                    break;
                }
            }
        }

        return result;
    }

    public static ServerProfile getServerProfile( UserModel userModel, String serverProfileId )
    {
        ServerProfile result = null;

        List serverProfiles = userModel.getServerProfiles();
        if ( serverProfiles != null )
        {
            for ( Iterator it = serverProfiles.iterator(); it.hasNext(); )
            {
                ServerProfile profile = (ServerProfile) it.next();
                if ( serverProfileId.equals( profile.getId() ) )
                {
                    result = profile;
                    break;
                }
            }
        }

        return result;
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

        if ( model == null )
        {
            model = new UserModel();
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