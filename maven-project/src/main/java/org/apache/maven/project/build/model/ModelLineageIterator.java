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
 * Iterator that gives access to all information associated with each model in a ModelLineage.
 * The result of the next() method is the Model instance itself, but you can also retrieve this
 * Model instance using getModel() below.
 * 
 * @author jdcasey
 */
public interface ModelLineageIterator
    extends Iterator
{
    
    /**
     * Retrieve the Model instance associated with the current position in the ModelLineage.
     * This is the same return value as the next() method.
     */
    Model getModel();
    
    /**
     * Retrieve the POM File associated with the current position in the ModelLineage
     */
    File getPOMFile();
    
    /**
     * Retrieve the remote ArtifactRepository instances associated with the current position 
     * in the ModelLineage.
     */
    List getArtifactRepositories();

}
