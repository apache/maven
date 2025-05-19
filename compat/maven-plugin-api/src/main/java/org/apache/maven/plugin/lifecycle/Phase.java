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
package org.apache.maven.plugin.lifecycle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A phase mapping definition.
 *
 * @version $Revision$ $Date$
 */
@SuppressWarnings("all")
public class Phase implements Serializable {

    // --------------------------/
    // - Class/Member Variables -/
    // --------------------------/

    /**
     * The ID of this phase, e.g., <code>generate-sources</code>.
     */
    private String id;

    /**
     * Field executions.
     */
    private List<Execution> executions;

    /**
     * Configuration to pass to all goals run in this phase.
     */
    private Object configuration;

    // -----------/
    // - Methods -/
    // -----------/

    /**
     * Method addExecution.
     *
     * @param execution a execution object.
     */
    public void addExecution(Execution execution) {
        getExecutions().add(execution);
    } // -- void addExecution( Execution )

    /**
     * Get configuration to pass to all goals run in this phase.
     *
     * @return Object
     */
    public Object getConfiguration() {
        return this.configuration;
    } // -- Object getConfiguration()

    /**
     * Method getExecutions.
     *
     * @return List
     */
    public List<Execution> getExecutions() {
        if (this.executions == null) {
            this.executions = new ArrayList<Execution>();
        }

        return this.executions;
    } // -- java.util.List<Execution> getExecutions()

    /**
     * Get the ID of this phase, e.g.,
     * <code>generate-sources</code>.
     *
     * @return String
     */
    public String getId() {
        return this.id;
    } // -- String getId()

    /**
     * Method removeExecution.
     *
     * @param execution a execution object.
     */
    public void removeExecution(Execution execution) {
        getExecutions().remove(execution);
    } // -- void removeExecution( Execution )

    /**
     * Set configuration to pass to all goals run in this phase.
     *
     * @param configuration a configuration object.
     */
    public void setConfiguration(Object configuration) {
        this.configuration = configuration;
    } // -- void setConfiguration( Object )

    /**
     * Set the goals to execute within the phase.
     *
     * @param executions a executions object.
     */
    public void setExecutions(List<Execution> executions) {
        this.executions = executions;
    } // -- void setExecutions( java.util.List )

    /**
     * Set the ID of this phase, e.g.,
     * <code>generate-sources</code>.
     *
     * @param id a id object.
     */
    public void setId(String id) {
        this.id = id;
    } // -- void setId( String )
}
