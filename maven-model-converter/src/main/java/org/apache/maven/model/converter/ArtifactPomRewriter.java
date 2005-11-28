package org.apache.maven.model.converter;

/*
 * Copyright 2005-2005 The Apache Software Foundation.
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

import java.io.Reader;
import java.io.Writer;
import java.util.List;

/**
 * @author jdcasey
 */
public interface ArtifactPomRewriter
{
    public static final String ROLE = ArtifactPomRewriter.class.getName();

    public static final String V3_POM = "v3";

    public static final String V4_POM = "v4";

    public void rewrite( Reader from, Writer to, boolean reportOnly, String groupId, String artifactId, String version,
                         String packaging )
        throws Exception;

    List getWarnings();
}
