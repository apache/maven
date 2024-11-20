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
package org.apache.maven.plugin.coreit;

import org.codehaus.plexus.component.annotations.Component;

/**
 * A dummy component whose role hint is not "default", yet should be used to satisfy an ordinary component requirement.
 *
 * @author Benjamin Bentmann
 */
@Component(role = TestComponent.class, hint = "concrete")
public class ConcreteTestComponent implements TestComponent {

    public String getId() {
        return "concrete";
    }
}
