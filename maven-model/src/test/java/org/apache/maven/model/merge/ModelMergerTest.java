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
package org.apache.maven.model.merge;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.junit.Test;

public class ModelMergerTest {

    @Test
    public void testMergedModelSerialization() throws Exception {
        Model target = new Model();
        Model source = new Model();
        target.setLicenses(new ArrayList<License>());
        License lic1 = new License();
        License lic2 = new License();
        target.getLicenses().add(lic1);
        source.setLicenses(new ArrayList<License>());
        source.getLicenses().add(lic2);

        new ModelMerger().mergeModel(target, source, false, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(target);
    }
}
