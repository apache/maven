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
package org.apache.maven.model.profile.activation;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblem.Version;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.codehaus.plexus.util.StringUtils;

/**
 * Determines profile activation based on the existence or value of some execution property.
 *
 * @author Benjamin Bentmann
 * @see ActivationProperty
 */
@Named("property")
@Singleton
public class PropertyProfileActivator implements ProfileActivator {

    @Override
    public boolean isActive(Profile profile, ProfileActivationContext context, ModelProblemCollector problems) {
        Activation activation = profile.getActivation();

        if (activation == null) {
            return false;
        }

        ActivationProperty property = activation.getProperty();

        if (property == null) {
            return false;
        }

        String name = property.getName();
        boolean reverseName = false;

        if (name != null && name.startsWith("!")) {
            reverseName = true;
            name = name.substring(1);
        }

        if (name == null || name.length() <= 0) {
            problems.add(new ModelProblemCollectorRequest(Severity.ERROR, Version.BASE)
                    .setMessage("The property name is required to activate the profile " + profile.getId())
                    .setLocation(property.getLocation("")));
            return false;
        }

        String sysValue = context.getUserProperties().get(name);
        if (sysValue == null) {
            sysValue = context.getSystemProperties().get(name);
        }

        String propValue = property.getValue();
        if (StringUtils.isNotEmpty(propValue)) {
            boolean reverseValue = false;
            if (propValue.startsWith("!")) {
                reverseValue = true;
                propValue = propValue.substring(1);
            }

            // we have a value, so it has to match the system value...
            boolean result = propValue.equals(sysValue);

            return reverseValue ? !result : result;
        } else {
            boolean result = StringUtils.isNotEmpty(sysValue);

            return reverseName ? !result : result;
        }
    }

    @Override
    public boolean presentInConfig(Profile profile, ProfileActivationContext context, ModelProblemCollector problems) {
        Activation activation = profile.getActivation();

        if (activation == null) {
            return false;
        }

        ActivationProperty property = activation.getProperty();

        if (property == null) {
            return false;
        }
        return true;
    }
}
