package org.apache.maven.tools.repoclean.discover;

import org.apache.maven.tools.repoclean.report.Reporter;

import java.io.File;
import java.util.List;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * @author jdcasey
 */
public interface ArtifactDiscoverer
{
    public static final String ROLE = ArtifactDiscoverer.class.getName();
    
    public static final String[] STANDARD_DISCOVERY_EXCLUDES = {
        "bin/**",
        "reports/**",
        ".maven/**",
        "**/poms/*.pom",
        "**/*.md5",
        "**/*snapshot-version",
        "*/website/**",
        "*/licenses/**",
        "*/licences/**",
        "**/.htaccess",
        "**/REPOSITORY-V*.txt"
    };

    List discoverArtifacts( File repositoryBase, Reporter reporter ) throws Exception;
    
}