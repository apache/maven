package org.apache.maven.tools.repoclean.index;

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

/**
 * @author jdcasey
 */
public class ArtifactIndexer
    extends AbstractLogEnabled
{

    public static final String ROLE = ArtifactIndexer.class.getName();

    public void writeAritfactIndex( List artifacts, File targetRepositoryBase )
    {
        List sortedArtifacts = new ArrayList( artifacts );
        Collections.sort( sortedArtifacts, new ArtifactIdComparator() );

        File indexFile = new File( targetRepositoryBase, ".index.txt" );
        FileWriter indexWriter = null;
        try
        {
            indexWriter = new FileWriter( indexFile );

            for ( Iterator it = sortedArtifacts.iterator(); it.hasNext(); )
            {
                Artifact artifact = (Artifact) it.next();
                indexWriter.write( artifact.getId() + "\n" );
            }
        }
        catch ( IOException e )
        {
            getLogger().error( "Error writing artifact index file.", e );
        }
        finally
        {
            IOUtil.close( indexWriter );
        }
    }

    private static final class ArtifactIdComparator
        implements Comparator
    {

        public int compare( Object first, Object second )
        {
            Artifact firstArtifact = (Artifact) first;
            Artifact secondArtifact = (Artifact) second;

            return firstArtifact.getConflictId().compareTo( secondArtifact.getConflictId() );
        }

    }

}