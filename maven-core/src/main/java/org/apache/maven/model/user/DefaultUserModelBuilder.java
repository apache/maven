package org.apache.maven.model.user;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.model.user.io.xpp3.MavenUserModelXpp3Reader;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author jdcasey
 */
public class DefaultUserModelBuilder
    extends AbstractLogEnabled
    implements UserModelBuilder
{

    private static final String DEFAULT_USER_MODEL_PATH = "${user.home}/.m2/user.xml";
    
    private String userModelPath = DEFAULT_USER_MODEL_PATH;

    // TODO: don't throw Exception.
    public UserModel buildUserModel() throws Exception
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
            getLogger().debug( "UserModel not found. Creating empty instance." );
            model = new UserModel();
        }

        return model;
    }

    // TODO: don't throw Exception.
//    public static void setUserModel( UserModel userModel ) throws Exception
//    {
//        File modelFile = getUserModelFile();
//
//        File modelDir = modelFile.getParentFile();
//        if ( !modelDir.exists() )
//        {
//            modelDir.mkdirs();
//        }
//
//        MavenUserModelXpp3Writer modelWriter = new MavenUserModelXpp3Writer();
//
//        FileWriter writer = null;
//        try
//        {
//            writer = new FileWriter( modelFile );
//
//            modelWriter.write( writer, userModel );
//
//            writer.flush();
//        }
//        finally
//        {
//            if ( writer != null )
//            {
//                try
//                {
//                    writer.close();
//                }
//                catch ( IOException e )
//                {
//                }
//            }
//        }
//
//    }

    private File getUserModelFile()
    {
        String userDir = System.getProperty( "user.home" );
        
        String path = userModelPath;
        
        path = path.replaceAll( "\\$\\{user.home\\}", userDir );
        path = path.replaceAll( "\\\\", "/" );
        path = path.replaceAll( "//", "/" );

        File userModelFile = new File( path );
        
        getLogger().debug( "Using userModel configured from: " + userModelFile );
        
        return userModelFile;
    }

}
