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
package org.apache.maven.internal.impl.model;

import java.util.Arrays;

import org.apache.maven.api.Session;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.SessionScoped;
import org.apache.maven.api.di.Typed;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Parent;
import org.apache.maven.api.services.ModelTransformerException;
import org.apache.maven.api.spi.ModelTransformer;

@SessionScoped
@Named
@Typed
public class CIFriendlyVersionModelTransformer implements ModelTransformer {

    private static final String SHA1_PROPERTY = "sha1";

    private static final String CHANGELIST_PROPERTY = "changelist";

    private static final String REVISION_PROPERTY = "revision";

    private final Session session;

    @Inject
    public CIFriendlyVersionModelTransformer(Session session) {
        this.session = session;
    }

    @Override
    public Model transformFileModel(Model model) throws ModelTransformerException {
        return model.with()
                .version(replaceCiFriendlyVersion(model.getVersion()))
                .parent(replaceParent(model.getParent()))
                .build();
    }

    private Parent replaceParent(Parent parent) {
        return parent != null ? parent.withVersion(replaceCiFriendlyVersion(parent.getVersion())) : null;
    }

    protected String replaceCiFriendlyVersion(String version) {
        if (version != null) {
            for (String key : Arrays.asList(SHA1_PROPERTY, CHANGELIST_PROPERTY, REVISION_PROPERTY)) {
                String val = session.getUserProperties().get(key);
                if (val != null) {
                    version = version.replace("${" + key + "}", val);
                }
            }
        }
        return version;
    }
}
