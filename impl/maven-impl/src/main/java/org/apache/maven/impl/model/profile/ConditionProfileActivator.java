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
package org.apache.maven.impl.model.profile;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.apache.maven.api.di.Inject;
import org.apache.maven.api.di.Named;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.Activation;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.services.BuilderProblem.Severity;
import org.apache.maven.api.services.Interpolator;
import org.apache.maven.api.services.ModelProblem.Version;
import org.apache.maven.api.services.ModelProblemCollector;
import org.apache.maven.api.services.VersionParser;
import org.apache.maven.api.services.model.ProfileActivationContext;
import org.apache.maven.api.services.model.ProfileActivator;

import static org.apache.maven.impl.model.profile.ConditionParser.toBoolean;

/**
 * This class is responsible for activating profiles based on conditions specified in the profile's activation section.
 * It evaluates the condition expression and determines whether the profile should be active.
 */
@Named("condition")
@Singleton
public class ConditionProfileActivator implements ProfileActivator {

    private final VersionParser versionParser;

    private final Interpolator interpolator;

    /**
     * Constructs a new ConditionProfileActivator with the necessary dependencies.
     *
     * @param versionParser The parser for handling version comparisons
     * @param interpolator  The interpolator for interpolating the values in the given map using the provided callback function
     */
    @Inject
    public ConditionProfileActivator(VersionParser versionParser, Interpolator interpolator) {
        this.versionParser = versionParser;
        this.interpolator = interpolator;
    }

    /**
     * Determines whether a profile should be active based on its condition.
     *
     * @param profile The profile to evaluate
     * @param context The context in which the profile is being evaluated
     * @param problems A collector for any problems encountered during evaluation
     * @return true if the profile should be active, false otherwise
     */
    @Override
    public boolean isActive(Profile profile, ProfileActivationContext context, ModelProblemCollector problems) {
        if (profile.getActivation() == null || profile.getActivation().getCondition() == null) {
            return false;
        }
        String condition = profile.getActivation().getCondition();
        try {
            Map<String, ConditionParser.ExpressionFunction> functions = registerFunctions(context, versionParser);
            UnaryOperator<String> propertyResolver = s -> property(context, s);
            return toBoolean(new ConditionParser(functions, propertyResolver).parse(condition));
        } catch (Exception e) {
            problems.add(
                    Severity.ERROR, Version.V41, "Error parsing profile activation condition: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Checks if the condition is present in the profile's configuration.
     *
     * @param profile The profile to check
     * @param context The context in which the profile is being evaluated
     * @param problems A collector for any problems encountered during evaluation
     * @return true if the condition is present and not blank, false otherwise
     */
    @Override
    public boolean presentInConfig(Profile profile, ProfileActivationContext context, ModelProblemCollector problems) {
        Activation activation = profile.getActivation();
        if (activation == null) {
            return false;
        }
        return activation.getCondition() != null && !activation.getCondition().isBlank();
    }

    /**
     * Registers the condition functions that can be used in profile activation expressions.
     *
     * @param context The profile activation context
     * @param versionParser The parser for handling version comparisons
     * @return A map of function names to their implementations
     */
    public Map<String, ConditionParser.ExpressionFunction> registerFunctions(
            ProfileActivationContext context, VersionParser versionParser) {
        Map<String, ConditionParser.ExpressionFunction> functions = new HashMap<>();

        ConditionFunctions conditionFunctions = new ConditionFunctions(context, versionParser);

        for (java.lang.reflect.Method method : ConditionFunctions.class.getDeclaredMethods()) {
            String methodName = method.getName();
            if (methodName.endsWith("_")) {
                methodName = methodName.substring(0, methodName.length() - 1);
            }
            final String finalMethodName = methodName;

            functions.put(finalMethodName, args -> {
                try {
                    return method.invoke(conditionFunctions, args);
                } catch (Exception e) {
                    StringBuilder causeChain = new StringBuilder();
                    Throwable cause = e;
                    while (cause != null) {
                        if (!causeChain.isEmpty()) {
                            causeChain.append(" Caused by: ");
                        }
                        causeChain.append(cause);
                        cause = cause.getCause();
                    }
                    throw new RuntimeException(
                            "Error invoking function '" + finalMethodName + "': " + e + ". Cause chain: " + causeChain,
                            e);
                }
            });
        }

        return functions;
    }

    /**
     * Retrieves the value of a property from the project context.
     * Special function used to support the <code>${property}</code> syntax.
     * <p>
     * The profile activation is done twice: once on the file model (so the model
     * which has just been read from the file) and once while computing the effective
     * model (so the model which will be used to build the project). We do need
     * those two activations to be consistent, so we need to restrict access to
     * properties that cannot change between file and effective model.
     *
     * @param name The property name
     * @return The value of the property, or null if not found
     * @throws IllegalArgumentException if the number of arguments is not exactly one
     */
    String property(ProfileActivationContext context, String name) {
        String value = doGetProperty(context, name);
        return interpolator.interpolate(value, s -> doGetProperty(context, s));
    }

    static String doGetProperty(ProfileActivationContext context, String name) {
        // Handle special project-related properties
        if ("project.basedir".equals(name)) {
            return context.getModelBaseDirectory();
        }
        if ("project.rootDirectory".equals(name)) {
            return context.getModelRootDirectory();
        }
        if ("project.artifactId".equals(name)) {
            return context.getModelArtifactId();
        }
        if ("project.packaging".equals(name)) {
            return context.getModelPackaging();
        }

        // Check user properties
        String v = context.getUserProperty(name);
        if (v == null) {
            // Check project properties
            // TODO: this may leads to instability between file model activation and effective model activation
            //       as the effective model properties may be different from the file model
            v = context.getModelProperty(name);
        }
        if (v == null) {
            // Check system properties
            v = context.getSystemProperty(name);
        }
        return v;
    }
}
