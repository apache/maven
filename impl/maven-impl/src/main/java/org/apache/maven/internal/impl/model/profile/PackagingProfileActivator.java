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
package org.apache.maven.internal.impl.model.profile;

import java.util.Objects;
import java.util.Optional;

import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Activation;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.model.ProfileActivationContext;
import org.apache.maven.api.services.model.ProfileActivator;

/**
 * Determines profile activation based on the project's packaging.
 */
@Named("packaging")
@Singleton
public class PackagingProfileActivator implements ProfileActivator {

    @Override
    public boolean isActive(Profile profile, ProfileActivationContext context, ModelProblemCollector problems) {
        return getActivationPackaging(profile).map(p -> isPackaging(context, p)).orElse(false);
    }

    @Override
    public boolean presentInConfig(Profile profile, ProfileActivationContext context, ModelProblemCollector problems) {
        return getActivationPackaging(profile).isPresent();
    }

    private static boolean isPackaging(ProfileActivationContext context, String p) {
        String packaging = context.getUserProperties().get(ProfileActivationContext.PROPERTY_NAME_PACKAGING);
        return Objects.equals(p, packaging);
    }

    private static Optional<String> getActivationPackaging(Profile profile) {
        return Optional.ofNullable(profile).map(Profile::getActivation).map(Activation::getPackaging);
    }
}
