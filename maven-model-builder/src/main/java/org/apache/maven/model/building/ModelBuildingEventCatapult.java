package org.apache.maven.model.building;

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

/**
 * Assists in firing events from a generic method by abstracting from the actual callback method to be called on the
 * listener.
 *
 * @author Benjamin Bentmann
 */
interface ModelBuildingEventCatapult
{

    /**
     * Notifies the specified listener of the given event.
     *
     * @param listener The listener to notify, must not be {@code null}.
     * @param event The event to fire, must not be {@code null}.
     */
    void fire( ModelBuildingListener listener, ModelBuildingEvent event );

    ModelBuildingEventCatapult BUILD_EXTENSIONS_ASSEMBLED = ModelBuildingListener::buildExtensionsAssembled;

}
