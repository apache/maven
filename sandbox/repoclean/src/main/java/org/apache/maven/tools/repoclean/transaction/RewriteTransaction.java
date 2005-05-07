package org.apache.maven.tools.repoclean.transaction;

import org.apache.maven.artifact.Artifact;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

public class RewriteTransaction
{

    private final Artifact artifact;

    private List files = new ArrayList();

    public RewriteTransaction( Artifact artifact )
    {
        this.artifact = artifact;
    }

    public void addFile( File file )
    {
        this.files.add( file );
    }

    public void rollback()
        throws RollbackException
    {
        for ( Iterator it = files.iterator(); it.hasNext(); )
        {
            File file = (File) it.next();
            if ( file.exists() && !file.delete() )
            {
                throw new RollbackException( "[rollback] Cannot delete file: " + file
                    + "\nPart of transaction for artifact: {" + artifact.getId() + "}." );
            }
        }
    }

}
