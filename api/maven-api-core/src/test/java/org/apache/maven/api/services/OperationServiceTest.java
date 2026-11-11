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
package org.apache.maven.api.services;

import java.util.List;

import org.apache.maven.api.Service;
import org.apache.maven.api.services.xml.XmlFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationServiceTest {

    @Test
    void testOperationServiceExtendsService() {
        assertTrue(Service.class.isAssignableFrom(OperationService.class));
    }

    @Test
    void testClassifiedOperationServicesAreAssignableToOperationService() {
        List<Class<? extends Service>> classified = List.of(
                ArtifactResolver.class,
                DependencyResolver.class,
                ModelBuilder.class,
                ProjectBuilder.class,
                VersionRangeResolver.class,
                VersionResolver.class);

        for (Class<? extends Service> serviceClass : classified) {
            assertTrue(
                    OperationService.class.isAssignableFrom(serviceClass),
                    () -> serviceClass.getSimpleName() + " must be assignable to OperationService");
            assertTrue(
                    Service.class.isAssignableFrom(serviceClass),
                    () -> serviceClass.getSimpleName() + " must be assignable to Service");
        }
    }

    @Test
    void testUtilityServicesRemainPlainServiceAndNotOperationService() {
        List<Class<? extends Service>> utilitiesAndExcluded = List.of(
                ArtifactCoordinatesFactory.class,
                ArtifactFactory.class,
                DependencyCoordinatesFactory.class,
                PathMatcherFactory.class,
                RepositoryFactory.class,
                MessageBuilderFactory.class,
                XmlFactory.class,
                Interpolator.class,
                VersionParser.class,
                ChecksumAlgorithmService.class,
                OsService.class,
                Lookup.class,
                Prompter.class,
                SuperPomProvider.class,
                ArtifactManager.class,
                ProjectManager.class,
                LocalRepositoryManager.class,
                ToolchainManager.class,
                ExtensibleEnumRegistry.class,
                SettingsBuilder.class,
                ToolchainsBuilder.class,
                ArtifactInstaller.class,
                ArtifactDeployer.class,
                TransportProvider.class);

        for (Class<? extends Service> serviceClass : utilitiesAndExcluded) {
            assertTrue(
                    Service.class.isAssignableFrom(serviceClass),
                    () -> serviceClass.getSimpleName() + " must remain assignable to Service");
            assertFalse(
                    OperationService.class.isAssignableFrom(serviceClass),
                    () -> serviceClass.getSimpleName() + " must NOT be assignable to OperationService");
        }
    }
}
