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
 * A set of goals to execute.
 *
 * @version $Revision$ $Date$
 */
@SuppressWarnings("all")
public class Execution implements Serializable {

    // --------------------------/
    // - Class/Member Variables -/
    // --------------------------/

    /**
     * Configuration to pass to the goals.
     */
    private Object configuration;

    /**
     * Field goals.
     */
    private List<String> goals;

    // -----------/
    // - Methods -/
    // -----------/

    /**
     * Method addGoal.
     *
     * @param string a string object.
     */
    public void addGoal(String string) {
        getGoals().add(string);
    } // -- void addGoal( String )

    /**
     * Get configuration to pass to the goals.
     *
     * @return Object
     */
    public Object getConfiguration() {
        return this.configuration;
    } // -- Object getConfiguration()

    /**
     * Method getGoals.
     *
     * @return List
     */
    public List<String> getGoals() {
        if (this.goals == null) {
            this.goals = new ArrayList<String>();
        }

        return this.goals;
    } // -- java.util.List<String> getGoals()

    /**
     * Method removeGoal.
     *
     * @param string a string object.
     */
    public void removeGoal(String string) {
        getGoals().remove(string);
    } // -- void removeGoal( String )

    /**
     * Set configuration to pass to the goals.
     *
     * @param configuration a configuration object.
     */
    public void setConfiguration(Object configuration) {
        this.configuration = configuration;
    } // -- void setConfiguration( Object )

    /**
     * Set the goals to execute.
     *
     * @param goals a goals object.
     */
    public void setGoals(List<String> goals) {
        this.goals = goals;
    } // -- void setGoals( java.util.List )
}
