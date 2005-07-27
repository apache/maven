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

import java.io.File;

/**
 * Capture common archive configuration.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @todo is this general enough to be in Plexus Archiver?
 */
public class MavenArchiveConfiguration
{
    private boolean compress = true;

    private boolean index;

    private File manifestFile;

    private ManifestConfiguration manifest;

    public boolean isCompress()
    {
        return compress;
    }

    public boolean isIndex()
    {
        return index;
    }

    public File getManifestFile()
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

    public void setCompress( boolean compress )
    {
        this.compress = compress;
    }

    public void setIndex( boolean index )
    {
        this.index = index;
    }

    public void setManifestFile( File manifestFile )
    {
        this.manifestFile = manifestFile;
    }

    public void setManifest( ManifestConfiguration manifest )
    {
        this.manifest = manifest;
    }
}
