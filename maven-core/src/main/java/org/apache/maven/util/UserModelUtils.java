package org.apache.maven.util;

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

    private UserModelUtils()
    {
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