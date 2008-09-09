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

/**
 * Extensions or implementations of this interface can be used to provide wrappers around existing models or can be
 * used to expose model elements directly. Each respective ModelTransformer implementation should know how to cast to
 * the appropriate domain model type(s).
 */
public interface DomainModel
{

    /**
     * Returns event history of joins and deletes used in constructing this domain model.
     *
     * @return event history of joins and deletes used in constructing this domain model
     */
    String getEventHistory();

    /**
     * Sets event history of joins and deletes used in constructing this domain model
     *
     * @param history history of joins and deletes used in constructing this domain model
     */
    void setEventHistory( String history );
}
