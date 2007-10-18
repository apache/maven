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

import junit.framework.TestCase;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.model.Model;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractModelLineageTest
    extends TestCase
{

    protected abstract ModelLineage newModelLineage();

    public void testAddParent_ShouldThrowIllegalStateExceptionWhenSetOriginNotCalled()
    {
        ModelLineage ml = newModelLineage();

        assertEquals( 0, ml.size() );

        try
        {
            ml.addParent( new Model(), null, null, false );

            fail( "Should throw IllegalStateException when setOrigin(..) is not called first." );
        }
        catch ( IllegalStateException e )
        {
            // expected
        }
    }

    public void testSetOrigin_ShouldThrowIllegalStateExceptionWhenSetOriginCalledRepeatedly()
    {
        ModelLineage ml = newModelLineage();

        assertEquals( 0, ml.size() );

        ml.setOrigin( new Model(), null, null, false );

        assertEquals( 1, ml.size() );

        try
        {
            ml.setOrigin( new Model(), null, null, false );

            fail( "Should throw IllegalStateException when setOrigin(..) called multiple times." );
        }
        catch ( IllegalStateException e )
        {
            // expected
        }
    }

    public void testSetOriginAndSize_SizeShouldIncrementByOneWhenOriginSet()
    {
        ModelLineage ml = newModelLineage();

        assertEquals( 0, ml.size() );

        ml.setOrigin( new Model(), null, null, false );

        assertEquals( 1, ml.size() );
    }

    public void testLineageIterator_ShouldAddTwoEntriesAndIterateInFIFOOrder()
        throws IOException
    {
        ModelLineage ml = newModelLineage();

        String gOne = "group1";
        String aOne = "artifact1";
        String vOne = "1";

        Model mOne = new Model();

        mOne.setGroupId( gOne );
        mOne.setArtifactId( aOne );
        mOne.setVersion( vOne );

        File fOne = File.createTempFile( "ModelLineageTest.modelLineageIterator-test.", "" );
        fOne.deleteOnExit();

        ml.setOrigin( mOne, fOne, null, false );

        String gTwo = "group2";
        String aTwo = "artifact2";
        String vTwo = "2";

        Model mTwo = new Model();

        mOne.setGroupId( gTwo );
        mOne.setArtifactId( aTwo );
        mOne.setVersion( vTwo );

        File fTwo = File.createTempFile( "ModelLineageTest.fileIterator-test.", "" );
        fTwo.deleteOnExit();

        ml.addParent( mTwo, fTwo, null, false );

        ModelLineageIterator it = ml.lineageIterator();

        assertTrue( it.hasNext() );
        assertEquals( mOne.getId(), ( (Model) it.next() ).getId() );
        assertEquals( mOne.getId(), it.getModel().getId() );
        assertEquals( fOne, it.getPOMFile() );

        assertTrue( it.hasNext() );
        assertEquals( mTwo.getId(), ( (Model) it.next() ).getId() );
        assertEquals( mTwo.getId(), it.getModel().getId() );
        assertEquals( fTwo, it.getPOMFile() );
    }

    public void testModelIterator_ShouldAddTwoModelsAndIterateInFIFOOrder()
    {
        ModelLineage ml = newModelLineage();

        String gOne = "group1";
        String aOne = "artifact1";
        String vOne = "1";

        Model mOne = new Model();

        mOne.setGroupId( gOne );
        mOne.setArtifactId( aOne );
        mOne.setVersion( vOne );

        ml.setOrigin( mOne, null, null, false );

        String gTwo = "group2";
        String aTwo = "artifact2";
        String vTwo = "2";

        Model mTwo = new Model();

        mOne.setGroupId( gTwo );
        mOne.setArtifactId( aTwo );
        mOne.setVersion( vTwo );

        ml.addParent( mTwo, null, null, false );

        Iterator it = ml.modelIterator();

        assertTrue( it.hasNext() );
        assertEquals( mOne.getId(), ( (Model) it.next() ).getId() );

        assertTrue( it.hasNext() );
        assertEquals( mTwo.getId(), ( (Model) it.next() ).getId() );
    }

    public void testFileIterator_ShouldAddTwoParentsAndIterateInFIFOOrder()
        throws IOException
    {
        ModelLineage ml = newModelLineage();

        File fOne = File.createTempFile( "ModelLineageTest.fileIterator-test.", "" );
        fOne.deleteOnExit();

        ml.setOrigin( new Model(), fOne, null, false );

        File fTwo = File.createTempFile( "ModelLineageTest.fileIterator-test.", "" );
        fTwo.deleteOnExit();

        ml.addParent( new Model(), fTwo, null, false );

        Iterator it = ml.fileIterator();

        assertTrue( it.hasNext() );
        assertEquals( fOne, it.next() );

        assertTrue( it.hasNext() );
        assertEquals( fTwo, it.next() );
    }

    public void testArtifactRepositoryListIterator_ShouldAddTwoParentsAndIterateInFIFOOrder()
    {
        DefaultRepositoryLayout layout = new DefaultRepositoryLayout();

        ModelLineage ml = newModelLineage();

        ArtifactRepository arOne = new DefaultArtifactRepository( "", "", layout );

        ml.setOrigin( new Model(), null, Collections.singletonList( arOne ), false );

        ArtifactRepository arTwo = new DefaultArtifactRepository( "", "", layout );

        ml.addParent( new Model(), null, Collections.singletonList( arTwo ), false );

        Iterator it = ml.artifactRepositoryListIterator();

        assertTrue( it.hasNext() );
        assertSame( arOne, ( (List) it.next() ).get( 0 ) );

        assertTrue( it.hasNext() );
        assertSame( arTwo, ( (List) it.next() ).get( 0 ) );
    }

    public void testAddParentAndGetFile_ShouldRetrieveCorrectFileForModel()
        throws IOException
    {
        ModelLineage ml = newModelLineage();

        File fOne = File.createTempFile( "ModelLineageTest.fileIterator-test.", "" );
        fOne.deleteOnExit();

        Model mOne = new Model();

        String gOne = "group";
        String aOne = "artifact";
        String vOne = "1";

        mOne.setGroupId( gOne );
        mOne.setArtifactId( aOne );
        mOne.setVersion( vOne );

        ml.setOrigin( mOne, fOne, null, false );

        File fTwo = File.createTempFile( "ModelLineageTest.fileIterator-test.", "" );
        fTwo.deleteOnExit();

        ml.addParent( new Model(), fTwo, null, false );

        Model retriever = new Model();

        retriever.setGroupId( gOne );
        retriever.setArtifactId( aOne );
        retriever.setVersion( vOne );

        assertEquals( fOne, ml.getFile( retriever ) );
    }

    public void testAddParentAndGetArtifactRepositories_ShouldRetrieveCorrectRepoListForModel()
        throws IOException
    {
        DefaultRepositoryLayout layout = new DefaultRepositoryLayout();

        ModelLineage ml = newModelLineage();

        Model mOne = new Model();

        String gOne = "group";
        String aOne = "artifact";
        String vOne = "1";

        mOne.setGroupId( gOne );
        mOne.setArtifactId( aOne );
        mOne.setVersion( vOne );

        ArtifactRepository arOne = new DefaultArtifactRepository( "", "", layout );

        ml.setOrigin( mOne, null, Collections.singletonList( arOne ), false );

        ArtifactRepository arTwo = new DefaultArtifactRepository( "", "", layout );

        ml.addParent( new Model(), null, Collections.singletonList( arTwo ), false );

        Model retriever = new Model();

        retriever.setGroupId( gOne );
        retriever.setArtifactId( aOne );
        retriever.setVersion( vOne );

        assertSame( arOne, ( ml.getArtifactRepositories( retriever ) ).get( 0 ) );
    }

    public void testSetOriginAndGetOriginatingModel()
    {
        Model model = new Model();
        model.setGroupId( "group" );
        model.setArtifactId( "artifact" );
        model.setVersion( "1" );

        ModelLineage ml = newModelLineage();

        ml.setOrigin( model, null, null, false );

        assertEquals( model.getId(), ml.getOriginatingModel().getId() );
    }

    public void testSetOriginAndGetOriginatingFile()
        throws IOException
    {
        ModelLineage ml = newModelLineage();

        File pomFile = File.createTempFile( "ModelLineage.test.", ".pom" );
        pomFile.deleteOnExit();

        ml.setOrigin( null, pomFile, null, false );

        assertEquals( pomFile, ml.getOriginatingPOMFile() );
    }

    public void testSetOriginAndGetOriginatingArtifactRepositoryList()
    {
        DefaultRepositoryLayout layout = new DefaultRepositoryLayout();

        ModelLineage ml = newModelLineage();

        Model mOne = new Model();

        String gOne = "group";
        String aOne = "artifact";
        String vOne = "1";

        mOne.setGroupId( gOne );
        mOne.setArtifactId( aOne );
        mOne.setVersion( vOne );

        ArtifactRepository arOne = new DefaultArtifactRepository( "", "", layout );

        ml.setOrigin( mOne, null, Collections.singletonList( arOne ), false );

        assertSame( arOne, ml.getOriginatingArtifactRepositoryList().get( 0 ) );
    }

    public void testGetModelsInDescendingOrder_ShouldAddTwoAndRetrieveInLIFOOrder()
    {
        ModelLineage ml = newModelLineage();

        String gOne = "group1";
        String aOne = "artifact1";
        String vOne = "1";

        Model mOne = new Model();

        mOne.setGroupId( gOne );
        mOne.setArtifactId( aOne );
        mOne.setVersion( vOne );

        ml.setOrigin( mOne, null, null, false );

        String gTwo = "group2";
        String aTwo = "artifact2";
        String vTwo = "2";

        Model mTwo = new Model();

        mOne.setGroupId( gTwo );
        mOne.setArtifactId( aTwo );
        mOne.setVersion( vTwo );

        ml.addParent( mTwo, null, null, false );

        Iterator it = ml.getModelsInDescendingOrder().iterator();

        assertTrue( it.hasNext() );
        assertEquals( mTwo.getId(), ( (Model) it.next() ).getId() );

        assertTrue( it.hasNext() );
        assertEquals( mOne.getId(), ( (Model) it.next() ).getId() );
    }

    public void testGetFilesInDescendingOrder_ShouldAddTwoAndRetrieveInLIFOOrder()
        throws IOException
    {
        ModelLineage ml = newModelLineage();

        File fOne = File.createTempFile( "ModelLineageTest.fileIterator-test.", "" );
        fOne.deleteOnExit();

        ml.setOrigin( new Model(), fOne, null, false );

        File fTwo = File.createTempFile( "ModelLineageTest.fileIterator-test.", "" );
        fTwo.deleteOnExit();

        ml.addParent( new Model(), fTwo, null, false );

        Iterator it = ml.getFilesInDescendingOrder().iterator();

        assertTrue( it.hasNext() );
        assertEquals( fTwo, it.next() );

        assertTrue( it.hasNext() );
        assertEquals( fOne, it.next() );
    }

    public void testGetArtifactRepositoryListsInDescendingOrder_ShouldAddTwoAndRetrieveInLIFOOrder()
    {
        DefaultRepositoryLayout layout = new DefaultRepositoryLayout();

        ModelLineage ml = newModelLineage();

        ArtifactRepository arOne = new DefaultArtifactRepository( "", "", layout );

        ml.setOrigin( new Model(), null, Collections.singletonList( arOne ), false );

        ArtifactRepository arTwo = new DefaultArtifactRepository( "", "", layout );

        ml.addParent( new Model(), null, Collections.singletonList( arTwo ), false );

        Iterator it = ml.getArtifactRepositoryListsInDescendingOrder().iterator();

        assertTrue( it.hasNext() );
        assertSame( arTwo, ( (List) it.next() ).get( 0 ) );

        assertTrue( it.hasNext() );
        assertSame( arOne, ( (List) it.next() ).get( 0 ) );
    }

}
