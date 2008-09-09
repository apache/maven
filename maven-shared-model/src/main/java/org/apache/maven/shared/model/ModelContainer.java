package org.apache.maven.shared.model;

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

import java.util.List;

/**
 * Provides services for determining actions to take: noop, delete, join. For example, say containers with the same ids
 * are joined, otherwise one must be deleted.
 * <pre>
 * ModelContainerA.id = "foo" and
 * ModelContainerB.id = "foobar"
 * </pre>
 * then ModelContainerA.containerAction(ModelContainerB) would return delete action for ModelContainerB.
 */
public interface ModelContainer
{

    /**
     * Returns the model properties contained within the model container. This list must be unmodifiable.
     *
     * @return the model properties contained within the model container
     */
    List<ModelProperty> getProperties();

    /**
     * Returns model container action (noop, delete, join) for the specified model container.
     *
     * @param modelContainer the model container to determine the action of
     * @return model container action (noop, delete, join) for the specified model container
     */
    ModelContainerAction containerAction( ModelContainer modelContainer );

    /**
     * Creates new instance of model container.
     *
     * @param modelProperties
     * @return new instance of model container
     */
    ModelContainer createNewInstance( List<ModelProperty> modelProperties );

}
