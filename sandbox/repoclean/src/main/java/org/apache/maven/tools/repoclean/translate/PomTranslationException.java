package org.apache.maven.tools.repoclean.translate;

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

/**
 * @author jdcasey
 */
public class PomTranslationException
    extends Exception
{

    private final String groupId;

    private final String artifactId;

    private final String version;

    public PomTranslationException( String groupId, String artifactId, String version, String message )
    {
        this( groupId, artifactId, version, message, null );
    }

    public PomTranslationException( String groupId, String artifactId, String version, Throwable cause )
    {
        this( groupId, artifactId, version, "[No message provided.]", cause );
    }

    public PomTranslationException( String groupId, String artifactId, String version, String message, Throwable cause )
    {
        super( "In POM{" + groupId + ":" + artifactId + ":" + version + "}: " + message, cause );
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }
}