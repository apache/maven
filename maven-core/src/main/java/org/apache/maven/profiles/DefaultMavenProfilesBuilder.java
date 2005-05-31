package org.apache.maven.profiles;

import org.apache.maven.profiles.ProfilesRoot;
import org.apache.maven.profiles.io.xpp3.ProfilesXpp3Reader;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class DefaultMavenProfilesBuilder
    implements MavenProfilesBuilder
{
    
    private static final String PROFILES_XML_FILE = "profiles.xml";

    public ProfilesRoot buildProfiles( File basedir )
        throws IOException, XmlPullParserException
    {
        File profilesXml = new File( basedir, PROFILES_XML_FILE );
        
        ProfilesRoot profilesRoot = null;
        
        if( profilesXml.exists() )
        {
            ProfilesXpp3Reader reader = new ProfilesXpp3Reader();
            FileReader fileReader = null;
            try
            {
                fileReader = new FileReader( profilesXml );
                
                profilesRoot = reader.read( fileReader );
            }
            finally
            {
                IOUtil.close( fileReader );
            }
        }
        
        return profilesRoot;
    }

}
