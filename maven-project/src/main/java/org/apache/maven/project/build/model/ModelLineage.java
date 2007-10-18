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
import java.util.Iterator;
import java.util.List;

/**
 * Tracks information from a current POM and its ancestors, including Model instances, associated
 * POM files, and repository lists used to resolve each model.
 *
 * @author jdcasey
 *
 */
public interface ModelLineage
{

    /**
     * Retrieve the Model instance for the deepest ancestor which has been resolved so far in this
     * lineage.
     */
    Model getDeepestAncestorModel();

    /**
     * Retrieve the flag telling whether discovery of a profiles.xml file is appropriate
     * for the deepest model in this lineage.
     */
    boolean isDeepestAncestorUsingProfilesXml();

    /**
     * Retrieve the POM file for the deepest ancestor which has been resolved so far in this
     * lineage.
     */
    File getDeepestAncestorFile();

    /**
     * Retrieve the remote-repository list for the deepest ancestor which has been resolved so far
     * in this lineage.
     */
    List getDeepestAncestorArtifactRepositoryList();

    /**
     * Retrieve the Model instance for the POM from which this lineage was constructed. This is the
     * "leaf" of the inheritance hierarchy, or the current POM, or the child (all means the same
     * thing).
     */
    Model getOriginatingModel();

    /**
     * Retrieve the File for the POM from which this lineage was constructed. This is the
     * "leaf" of the inheritance hierarchy, or the current POM, or the child (all means the same
     * thing).
     */
    File getOriginatingPOMFile();

    /**
     * Retrieve the List of ArtifactRepository instances used to resolve the first parent POM of the
     * POM from which this lineage was constructed. This is the "leaf" of the inheritance hierarchy,
     * or the current POM, or the child (all means the same thing).
     */
    List getOriginatingArtifactRepositoryList();

    /**
     * Setup the originating POM information from which this lineage is constructed. This is the
     * "child" POM that is the starting point of the build.
     *
     * @throws IllegalStateException When the originating POM information has already been set.
     */
    void setOrigin( Model model, File pomFile, List artifactRepositories, boolean validProfilesXmlLocation );

    /**
     * Add a parent model, along with its file and the repositories used to resolve it.
     * NOTE: If setOrigin(..) hasn't been called, this method will result in an IllegalStateException.
     *
     * @throws IllegalStateException When the originating POM information has not yet been set.
     */
    void addParent( Model model, File pomFile, List artifactRepositories, boolean validProfilesXmlLocation );

    /**
     * Retrieve the models in this lineage, with the deepest parent at the zero index, and the current
     * POM at the last index.
     */
    List getModelsInDescendingOrder();

    /**
     * Retrieve the files used to construct this lineage, with that of the deepest parent at the
     * zero index, and that of the current POM at the last index.
     */
    List getFilesInDescendingOrder();

    /**
     * Retrieve the remote-artifact repository lists used to construct this lineage, with
     * that of the deepest parent at the zero index, and that of the current POM at the last index.
     */
    List getArtifactRepositoryListsInDescendingOrder();

    /**
     * Retrieve an Iterator derivative that functions in the simplest sense just like the return
     * value of the modelIterator() method. However, the ModelLineageIterator also gives access to
     * the current POM file and current remote ArtifactRepository instances used to resolve the
     * current Model...along with a method to give explicit access to the current Model instance.
     */
    ModelLineageIterator lineageIterator();

    /**
     * Retrieve an Iterator derivative that functions in the simplest sense just like the return
     * value of the modelIterator() method. However, the ModelLineageIterator also gives access to
     * the current POM file and current remote ArtifactRepository instances used to resolve the
     * current Model...along with a method to give explicit access to the current Model instance.
     */
    ModelLineageIterator reversedLineageIterator();

    /**
     * Iterate over the lineage of Model instances, starting with the child (current) Model,
     * and ending with the deepest ancestor.
     */
    Iterator modelIterator();

    /**
     * Iterate over the lineage of POM Files, starting with the child (current) POM and ending with
     * the deepest ancestor.
     */
    Iterator fileIterator();

    /**
     * Iterate over the remote-repository Lists used to resolve the lineage, starting with the
     * child (current) remote-repository List and ending with the deepest ancestor.
     */
    Iterator artifactRepositoryListIterator();

    /**
     * Retrieve the File from which the given Model instance was read. If the model itself doesn't
     * belong to this lineage, match it in the lineage by Model.getId().
     */
    File getFile( Model model );

    /**
     * Retrieve the List of remote repositories from which the given Model instance was resolved.
     * If the model itself doesn't belong to this lineage, match it in the lineage by Model.getId().
     */
    List getArtifactRepositories( Model model );

    /**
     * Retrieve the number of entries (POMs) in this lineage.
     */
    int size();

}