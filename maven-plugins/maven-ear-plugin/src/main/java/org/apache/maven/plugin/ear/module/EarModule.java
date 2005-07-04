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
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * The ear module interface.
 *
 * @author Stephane Nicoll <stephane.nicoll@gmail.com>
 * @author $Author: sni $ (last edit)
 * @version $Revision: 1.2 $
 */
public interface EarModule
{

    /**
     * Returns the <tt>Artifact</tt> representing this module.
     *
     * @return the <tt>Artifact</tt>
     */
    public Artifact getArtifact();

    /**
     * Returns the <tt>URI</tt> fo the Ear module.
     *
     * @return the <tt>URI</tt>
     */
    public String getUri();

    /**
     * Appends the <tt>XML</tt> representation of this module.
     *
     * @param writer  the writer to use
     * @param version the version of the <tt>application.xml</tt> file
     */
    public void appendModule( XMLWriter writer, String version );

}
