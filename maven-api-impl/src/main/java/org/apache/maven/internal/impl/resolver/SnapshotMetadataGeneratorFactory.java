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
package org.apache.maven.internal.impl.resolver;

import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.impl.MetadataGenerator;
import org.eclipse.aether.impl.MetadataGeneratorFactory;
import org.eclipse.aether.installation.InstallRequest;

/**
 * Maven GAV level metadata generator factory.
 */
@Named(SnapshotMetadataGeneratorFactory.NAME)
@Singleton
public class SnapshotMetadataGeneratorFactory implements MetadataGeneratorFactory {
    public static final String NAME = "snapshot";

    @Override
    public MetadataGenerator newInstance(RepositorySystemSession session, InstallRequest request) {
        return new LocalSnapshotMetadataGenerator(session, request);
    }

    @Override
    public MetadataGenerator newInstance(RepositorySystemSession session, DeployRequest request) {
        return new RemoteSnapshotMetadataGenerator(session, request);
    }

    @Override
    public float getPriority() {
        return 30; // GAV level metadata should be deployed 1st MD
    }
}
