package org.apache.maven.archiver;

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
 * Capture common archive configuration.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @todo is this general enough to be in Plexus Archiver?
 */
public class MavenArchiveConfiguration
{
    /**
     * @todo boolean instead
     */
    private String compress;

    /**
     * @todo boolean instead
     */
    private String index;

    private String manifestFile;

    private ManifestConfiguration manifest;

    public boolean isCompress()
    {
        return compress != null ? Boolean.valueOf( compress ).booleanValue() : true;
    }

    public boolean isIndex()
    {
        return index != null ? Boolean.valueOf( index ).booleanValue() : false;
    }

    public String getManifestFile()
    {
        return manifestFile;
    }

    public ManifestConfiguration getManifest()
    {
        if ( manifest == null )
        {
            manifest = new ManifestConfiguration();
        }
        return manifest;
    }
}
