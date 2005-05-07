package org.apache.maven.tools.repoclean.rewrite;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.tools.repoclean.report.Reporter;

import java.io.Reader;
import java.io.Writer;

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
public interface ArtifactPomRewriter
{

    public static final String ROLE = ArtifactPomRewriter.class.getName();

    public static final String V3_POM = "v3";

    public static final String V4_POM = "v4";

    void rewrite( Artifact artifact, Reader from, Writer to, Reporter reporter, boolean reportOnly )
        throws Exception;

}
