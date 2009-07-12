package org.apache.maven.embedder;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;

/**
 * A configuration to be used for unit testing of the embedder. This basically sets some default values.
 * 
 * @author Benjamin Bentmann
 */
public class SimpleConfiguration
    extends DefaultConfiguration
{

    public SimpleConfiguration()
    {
        String localRepo = System.getProperty( "maven.repo.local", "" );

        if ( localRepo.length() > 0 )
        {
            setLocalRepository( new File( localRepo ).getAbsoluteFile() );
        }

        setClassLoader( Thread.currentThread().getContextClassLoader() );

        setMavenEmbedderLogger( new MavenEmbedderConsoleLogger() );

        setUserSettingsFile( MavenEmbedder.DEFAULT_USER_SETTINGS_FILE );
    }

}
