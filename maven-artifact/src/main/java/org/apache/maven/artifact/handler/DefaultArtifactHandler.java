package org.apache.maven.artifact.handler;

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

/**
 * @author <a href="mailto:brett@apach.org">Brett Porter</a>
 * @version $Id: AbstractArtifactHandler.java 189871 2005-06-10 00:57:19Z brett $
 */
public class DefaultArtifactHandler
    implements ArtifactHandler
{
    private String extension;

    private String type;

    private String classifier;

    private String directory;

    private String packaging;

    public DefaultArtifactHandler()
    {
    }

    public DefaultArtifactHandler( String type )
    {
        this.type = type;
    }

    public String getExtension()
    {
        if ( extension == null )
        {
            extension = type;
        }
        return extension;
    }

    public String getType()
    {
        return type;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public String getDirectory()
    {
        if ( directory == null )
        {
            directory = type + "s";
        }
        return directory;
    }

    public String getPackaging()
    {
        if ( packaging == null )
        {
            packaging = type;
        }
        return packaging;
    }
}
