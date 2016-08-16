package org.apache.maven.model.profile.activation;

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

import org.apache.maven.model.condition.SingleValueEvaluator;
import org.apache.maven.model.path.PathTranslator;

/**
 * @author Loïc B.
 */
public class FileExistenceEvaluator
    implements SingleValueEvaluator
{

    private final boolean missing;

    private final PathTranslator pathTranslator;

    private final File basedir;

    /**
     * Creates the FileExistenceEvaluator for missing or existing files.
     * 
     * @param missing true if the file should be missing, false if it should be existing
     * @param pathTranslator path translator implementation to be used
     */
    public FileExistenceEvaluator( boolean missing, PathTranslator pathTranslator, File basedir )
    {
        this.missing = missing;
        this.pathTranslator = pathTranslator;
        this.basedir = basedir;
    }

    /* (non-Javadoc)
     * @see org.apache.maven.model.profile.activation.SingleValueEvaluator#evaluate(java.lang.String)
     */
    @Override
    public boolean evaluate( String path )
    {
        boolean reversed = false;
        if ( path.startsWith( "!" ) )
        {
            reversed = true;
            path = path.substring( 1 );
        }
        path = pathTranslator.alignToBaseDirectory( path, basedir );
        File f = new File( path );

        if ( !f.isAbsolute() )
        {
            return false;
        }

        boolean fileExists = f.exists();

        return missing ^ reversed ? !fileExists : fileExists;
    }

}
