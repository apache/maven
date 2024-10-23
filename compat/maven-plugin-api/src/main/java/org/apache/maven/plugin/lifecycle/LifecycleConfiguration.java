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

/**
 * Root element of the <code>lifecycle.xml</code> file.
 *
 * @version $Revision$ $Date$
 */
@SuppressWarnings("all")
public class LifecycleConfiguration implements java.io.Serializable {

    // --------------------------/
    // - Class/Member Variables -/
    // --------------------------/

    /**
     * Field lifecycles.
     */
    private java.util.List<Lifecycle> lifecycles;

    /**
     * Field modelEncoding.
     */
    private String modelEncoding = "UTF-8";

    // -----------/
    // - Methods -/
    // -----------/

    /**
     * Method addLifecycle.
     *
     * @param lifecycle a lifecycle object.
     */
    public void addLifecycle(Lifecycle lifecycle) {
        getLifecycles().add(lifecycle);
    } // -- void addLifecycle( Lifecycle )

    /**
     * Method getLifecycles.
     *
     * @return List
     */
    public java.util.List<Lifecycle> getLifecycles() {
        if (this.lifecycles == null) {
            this.lifecycles = new java.util.ArrayList<Lifecycle>();
        }

        return this.lifecycles;
    } // -- java.util.List<Lifecycle> getLifecycles()

    /**
     * Get the modelEncoding field.
     *
     * @return String
     */
    public String getModelEncoding() {
        return this.modelEncoding;
    } // -- String getModelEncoding()

    /**
     * Method removeLifecycle.
     *
     * @param lifecycle a lifecycle object.
     */
    public void removeLifecycle(Lifecycle lifecycle) {
        getLifecycles().remove(lifecycle);
    } // -- void removeLifecycle( Lifecycle )

    /**
     * Set the lifecycles field.
     *
     * @param lifecycles a lifecycles object.
     */
    public void setLifecycles(java.util.List<Lifecycle> lifecycles) {
        this.lifecycles = lifecycles;
    } // -- void setLifecycles( java.util.List )

    /**
     * Set the modelEncoding field.
     *
     * @param modelEncoding a modelEncoding object.
     */
    public void setModelEncoding(String modelEncoding) {
        this.modelEncoding = modelEncoding;
    } // -- void setModelEncoding( String )
}
