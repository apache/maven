package org.apache.maven.plugin.ear.module;

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

import org.apache.maven.artifact.Artifact;

/**
 * A base implementation of an {@link EarModule}.
 *
 * @author Stephane Nicoll <stephane.nicoll@gmail.com>
 * @author $Author: sni $ (last edit)
 * @version $Revision: 1.2 $
 */
public abstract class AbstractEarModule
    implements EarModule
{

    protected static final String MODULE_ELEMENT = "module";

    private final String uri;

    private final Artifact artifact;

    AbstractEarModule( String uri, Artifact a )
    {
        this.uri = uri;
        this.artifact = a;
    }

    public Artifact getArtifact()
    {
        return artifact;
    }

    public String getUri()
    {
        return uri;
    }
}
