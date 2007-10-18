package org.apache.maven.project.build.model;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.model.Model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @see org.apache.maven.project.build.model.ModelLineage
 */
public class DefaultModelLineage
    implements ModelLineage
{

    private List tuples = new ArrayList();

    /**
     * @see org.apache.maven.project.build.model.ModelLineage#addParent(org.apache.maven.model.Model, java.io.File, java.util.List)
     */
    public void addParent( Model model, File pomFile, List artifactRepositories, boolean validProfilesXmlLocation )
    {
        if ( tuples.isEmpty() )
        {
            throw new IllegalStateException( "You must call setOrigin(..) before adding a parent to the lineage." );
        }

        tuples.add( new ModelLineageTuple( model, pomFile, artifactRepositories, validProfilesXmlLocation ) );
    }

    /**
     * @see org.apache.maven.project.build.model.ModelLineage#artifactRepositoryListIterator()
     */
    public Iterator artifactRepositoryListIterator()
    {
        return new Iterator()
        {

            private int idx = 0;

            public boolean hasNext()
            {
                return tuples.size() > idx;
            }

            public Object next()
            {
                return ( (ModelLineageTuple) tuples.get( idx++ ) ).remoteRepositories;
            }

            public void remove()
            {
                tuples.remove( idx );
            }

        };
    }

    /**
     * @see org.apache.maven.project.build.model.ModelLineage#fileIterator()
     */
    public Iterator fileIterator()
    {
        return new Iterator()
        {

            private int idx = 0;

            public boolean hasNext()
            {
                return tuples.size() > idx;
            }

            public Object next()
            {
                return ( (ModelLineageTuple) tuples.get( idx++ ) ).file;
            }

            public void remove()
            {
                tuples.remove( idx );
            }

        };
    }

    /**
     * @see org.apache.maven.project.build.model.ModelLineage#getArtifactRepositories(org.apache.maven.model.Model)
     */
    public List getArtifactRepositories( Model model )
    {
        int index = tuples.indexOf( new ModelLineageTuple( model ) );

        ModelLineageTuple tuple = (ModelLineageTuple) tuples.get( index );

        return tuple != null ? tuple.remoteRepositories : null;
    }

    public List getArtifactRepositoryListsInDescendingOrder()
    {
        if ( tuples.isEmpty() )
        {
            return Collections.EMPTY_LIST;
        }

        List tuplesInReverse = new ArrayList( tuples );
        Collections.reverse( tuplesInReverse );

        List results = new ArrayList( tuplesInReverse.size() );
        for ( Iterator it = tuplesInReverse.iterator(); it.hasNext(); )
        {
            ModelLineageTuple tuple = (ModelLineageTuple) it.next();

            results.add( tuple.remoteRepositories );
        }

        return results;
    }

    /**
     * @see org.apache.maven.project.build.model.ModelLineage#getFile(org.apache.maven.model.Model)
     */
    public File getFile( Model model )
    {
        int index = tuples.indexOf( new ModelLineageTuple( model ) );

        ModelLineageTuple tuple = (ModelLineageTuple) tuples.get( index );

        return tuple != null ? tuple.file : null;
    }

    public List getFilesInDescendingOrder()
    {
        if ( tuples.isEmpty() )
        {
            return Collections.EMPTY_LIST;
        }

        List tuplesInReverse = new ArrayList( tuples );
        Collections.reverse( tuplesInReverse );

        List results = new ArrayList( tuplesInReverse.size() );
        for ( Iterator it = tuplesInReverse.iterator(); it.hasNext(); )
        {
            ModelLineageTuple tuple = (ModelLineageTuple) it.next();

            results.add( tuple.file );
        }

        return results;
    }

    public List getModelsInDescendingOrder()
    {
        if ( tuples.isEmpty() )
        {
            return Collections.EMPTY_LIST;
        }

        List tuplesInReverse = new ArrayList( tuples );
        Collections.reverse( tuplesInReverse );

        List results = new ArrayList( tuplesInReverse.size() );
        for ( Iterator it = tuplesInReverse.iterator(); it.hasNext(); )
        {
            ModelLineageTuple tuple = (ModelLineageTuple) it.next();

            results.add( tuple.model );
        }

        return results;
    }

    public List getOriginatingArtifactRepositoryList()
    {
        if ( tuples.isEmpty() )
        {
            return null;
        }

        ModelLineageTuple tuple = (ModelLineageTuple) tuples.get( 0 );

        return tuple.remoteRepositories;
    }

    public Model getOriginatingModel()
    {
        if ( tuples.isEmpty() )
        {
            return null;
        }

        ModelLineageTuple tuple = (ModelLineageTuple) tuples.get( 0 );

        return tuple.model;
    }

    public File getOriginatingPOMFile()
    {
        if ( tuples.isEmpty() )
        {
            return null;
        }

        ModelLineageTuple tuple = (ModelLineageTuple) tuples.get( 0 );

        return tuple.file;
    }

    public List getDeepestAncestorArtifactRepositoryList()
    {
        if ( tuples.isEmpty() )
        {
            return null;
        }

        ModelLineageTuple tuple = (ModelLineageTuple) tuples.get( tuples.size() - 1 );

        return tuple.remoteRepositories;
    }

    public File getDeepestAncestorFile()
    {
        if ( tuples.isEmpty() )
        {
            return null;
        }

        ModelLineageTuple tuple = (ModelLineageTuple) tuples.get( tuples.size() - 1 );

        return tuple.file;
    }

    public Model getDeepestAncestorModel()
    {
        if ( tuples.isEmpty() )
        {
            return null;
        }

        ModelLineageTuple tuple = (ModelLineageTuple) tuples.get( tuples.size() - 1 );

        return tuple.model;
    }

    public boolean isDeepestAncestorUsingProfilesXml()
    {
        if ( tuples.isEmpty() )
        {
            return false;
        }

        ModelLineageTuple tuple = (ModelLineageTuple) tuples.get( tuples.size() - 1 );

        return tuple.validProfilesXmlLocation;
    }

    /**
     * @see org.apache.maven.project.build.model.ModelLineage#modelIterator()
     */
    public Iterator modelIterator()
    {
        return new Iterator()
        {

            private int idx = 0;

            public boolean hasNext()
            {
                return tuples.size() > idx;
            }

            public Object next()
            {
                return ( (ModelLineageTuple) tuples.get( idx++ ) ).model;
            }

            public void remove()
            {
                tuples.remove( idx );
            }

        };
    }

    public void setOrigin( Model model, File pomFile, List artifactRepositories, boolean validProfilesXmlLocation )
    {
        if ( !tuples.isEmpty() )
        {
            throw new IllegalStateException( "Origin already set; you must use addParent(..) for successive additions to the lineage." );
        }

        tuples.add( new ModelLineageTuple( model, pomFile, artifactRepositories, validProfilesXmlLocation ) );
    }

    /**
     * @see org.apache.maven.project.build.model.ModelLineage#size()
     */
    public int size()
    {
        return tuples.size();
    }

    private static final class ModelLineageTuple
    {
        private Model model;

        private File file;

        private List remoteRepositories;

        private final boolean validProfilesXmlLocation;

        private ModelLineageTuple( Model model )
        {
            this.model = model;
            validProfilesXmlLocation = false;
        }

        private ModelLineageTuple( Model model, File file, List remoteRepositories, boolean validProfilesXmlLocation )
        {
            this.model = model;
            this.file = file;
            this.remoteRepositories = remoteRepositories;
            this.validProfilesXmlLocation = validProfilesXmlLocation;
        }

        public boolean equals( Object other )
        {
            if ( this == other )
            {
                return true;
            }
            else if ( other instanceof ModelLineageTuple )
            {
                ModelLineageTuple otherTuple = (ModelLineageTuple) other;

                return model.getId().equals( otherTuple.model.getId() );
            }

            return false;
        }
    }

    public ModelLineageIterator lineageIterator()
    {
        return new ModelLineageIterator()
        {

            private int idx = -1;

            public boolean hasNext()
            {
                return tuples.size() > idx + 1;
            }

            public Object next()
            {
                return ( (ModelLineageTuple) tuples.get( ( ++idx ) ) ).model;
            }

            public void remove()
            {
                tuples.remove( idx );
            }

            public List getArtifactRepositories()
            {
                return ( (ModelLineageTuple) tuples.get( idx ) ).remoteRepositories;
            }

            public Model getModel()
            {
                return ( (ModelLineageTuple) tuples.get( idx ) ).model;
            }

            public File getPOMFile()
            {
                return ( (ModelLineageTuple) tuples.get( idx ) ).file;
            }

        };
    }

    public ModelLineageIterator reversedLineageIterator()
    {
        return new ModelLineageIterator()
        {

            private int idx = tuples.size();

            public boolean hasNext()
            {
                return idx > 0;
            }

            public Object next()
            {
                return ( (ModelLineageTuple) tuples.get( ( --idx ) ) ).model;
            }

            public void remove()
            {
                tuples.remove( idx );
            }

            public List getArtifactRepositories()
            {
                return ( (ModelLineageTuple) tuples.get( idx ) ).remoteRepositories;
            }

            public Model getModel()
            {
                return ( (ModelLineageTuple) tuples.get( idx ) ).model;
            }

            public File getPOMFile()
            {
                return ( (ModelLineageTuple) tuples.get( idx ) ).file;
            }

        };
    }

}
