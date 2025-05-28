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
package org.apache.maven.cling.invoker.mvnup.jdom;

/*
 * Copyright 2018 CoreMedia AG, Hamburg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public class JDomPomCfg implements JDomCfg {
    public static final String POM_ELEMENT_ACTIVATION = "activation";
    public static final String POM_ELEMENT_ACTIVE_BY_DEFAULT = "activeByDefault";
    public static final String POM_ELEMENT_ARTIFACT_ID = "artifactId";
    public static final String POM_ELEMENT_BUILD = "build";
    public static final String POM_ELEMENT_CI_MANAGEMENT = "ciManagement";
    public static final String POM_ELEMENT_CLASSIFIER = "classifier";
    public static final String POM_ELEMENT_CONFIGURATION = "configuration";
    public static final String POM_ELEMENT_CONNECTION = "connection";
    public static final String POM_ELEMENT_CONTRIBUTORS = "contributors";
    public static final String POM_ELEMENT_DEFAULT_GOAL = "defaultGoal";
    public static final String POM_ELEMENT_DEPENDENCIES = "dependencies";
    public static final String POM_ELEMENT_DEPENDENCY_MANAGEMENT = "dependencyManagement";
    public static final String POM_ELEMENT_DEPENDENCY = "dependency";
    public static final String POM_ELEMENT_DESCRIPTION = "description";
    public static final String POM_ELEMENT_DEVELOPER_CONNECTION = "developerConnection";
    public static final String POM_ELEMENT_DEVELOPERS = "developers";
    public static final String POM_ELEMENT_DIRECTORY = "directory";
    public static final String POM_ELEMENT_DISTRIBUTION_MANAGEMENT = "distributionManagement";
    public static final String POM_ELEMENT_EXCLUSIONS = "exclusions";
    public static final String POM_ELEMENT_EXCLUSION = "exclusion";
    public static final String POM_ELEMENT_EXECUTIONS = "executions";
    public static final String POM_ELEMENT_EXECUTION = "execution";
    public static final String POM_ELEMENT_EXTENSIONS = "extensions";
    public static final String POM_ELEMENT_EXTENSION = "extension";
    public static final String POM_ELEMENT_FINAL_NAME = "finalName";
    public static final String POM_ELEMENT_GOALS = "goals";
    public static final String POM_ELEMENT_GOAL = "goal";
    public static final String POM_ELEMENT_GROUP_ID = "groupId";
    public static final String POM_ELEMENT_ID = "id";
    public static final String POM_ELEMENT_INCEPTION_YEAR = "inceptionYear";
    public static final String POM_ELEMENT_INHERITED = "inherited";
    public static final String POM_ELEMENT_ISSUE_MANAGEMENT = "issueManagement";
    public static final String POM_ELEMENT_JDK = "jdk";
    public static final String POM_ELEMENT_LICENSES = "licenses";
    public static final String POM_ELEMENT_MAILING_LISTS = "mailingLists";
    public static final String POM_ELEMENT_MODEL_VERSION = "modelVersion";
    public static final String POM_ELEMENT_MODULE = "module";
    public static final String POM_ELEMENT_MODULES = "modules";
    public static final String POM_ELEMENT_NAME = "name";
    public static final String POM_ELEMENT_OPTIONAL = "optional";
    public static final String POM_ELEMENT_ORGANIZATION = "organization";
    public static final String POM_ELEMENT_OUTPUT_DIRECTORY = "outputDirectory";
    public static final String POM_ELEMENT_PACKAGING = "packaging";
    public static final String POM_ELEMENT_PARENT = "parent";
    public static final String POM_ELEMENT_PHASE = "phase";
    public static final String POM_ELEMENT_PLUGIN = "plugin";
    public static final String POM_ELEMENT_PLUGIN_MANAGEMENT = "pluginManagement";
    public static final String POM_ELEMENT_PLUGIN_REPOSITORIES = "pluginRepositories";
    public static final String POM_ELEMENT_PLUGINS = "plugins";
    public static final String POM_ELEMENT_PREREQUISITES = "prerequisites";
    public static final String POM_ELEMENT_PROFILE = "profile";
    public static final String POM_ELEMENT_PROFILES = "profiles";
    public static final String POM_ELEMENT_PROJECT = "project";
    public static final String POM_ELEMENT_PROPERTIES = "properties";
    public static final String POM_ELEMENT_PROPERTY = "property";
    public static final String POM_ELEMENT_RELATIVE_PATH = "relativePath";
    public static final String POM_ELEMENT_REPORTING = "reporting";
    public static final String POM_ELEMENT_REPOSITORIES = "repositories";
    public static final String POM_ELEMENT_SCM = "scm";
    public static final String POM_ELEMENT_SCOPE = "scope";
    public static final String POM_ELEMENT_SCRIPT_SOURCE_DIRECTORY = "scriptSourceDirectory";
    public static final String POM_ELEMENT_SOURCE_DIRECTORY = "sourceDirectory";
    public static final String POM_ELEMENT_SYSTEM_PATH = "systemPath";
    public static final String POM_ELEMENT_TAG = "tag";
    public static final String POM_ELEMENT_TEST_OUTPUT_DIRECTORY = "testOutputDirectory";
    public static final String POM_ELEMENT_TEST_SOURCE_DIRECTORY = "testSourceDirectory";
    public static final String POM_ELEMENT_TYPE = "type";
    public static final String POM_ELEMENT_URL = "url";
    public static final String POM_ELEMENT_VALUE = "value";
    public static final String POM_ELEMENT_VERSION = "version";

    private final Map<String, List<String>> elementOrder = new HashMap<>();

    public JDomPomCfg() {
        elementOrder.put(
                POM_ELEMENT_PROJECT,
                asList(
                        POM_ELEMENT_MODEL_VERSION,
                        "",
                        POM_ELEMENT_PARENT,
                        "",
                        POM_ELEMENT_GROUP_ID,
                        POM_ELEMENT_ARTIFACT_ID,
                        POM_ELEMENT_VERSION,
                        POM_ELEMENT_PACKAGING,
                        "",
                        POM_ELEMENT_NAME,
                        POM_ELEMENT_DESCRIPTION,
                        POM_ELEMENT_URL,
                        POM_ELEMENT_INCEPTION_YEAR,
                        POM_ELEMENT_ORGANIZATION,
                        POM_ELEMENT_LICENSES,
                        "",
                        POM_ELEMENT_DEVELOPERS,
                        POM_ELEMENT_CONTRIBUTORS,
                        "",
                        POM_ELEMENT_MAILING_LISTS,
                        "",
                        POM_ELEMENT_PREREQUISITES,
                        "",
                        POM_ELEMENT_MODULES,
                        "",
                        POM_ELEMENT_SCM,
                        POM_ELEMENT_ISSUE_MANAGEMENT,
                        POM_ELEMENT_CI_MANAGEMENT,
                        POM_ELEMENT_DISTRIBUTION_MANAGEMENT,
                        "",
                        POM_ELEMENT_PROPERTIES,
                        "",
                        POM_ELEMENT_DEPENDENCY_MANAGEMENT,
                        POM_ELEMENT_DEPENDENCIES,
                        "",
                        POM_ELEMENT_REPOSITORIES,
                        POM_ELEMENT_PLUGIN_REPOSITORIES,
                        "",
                        POM_ELEMENT_BUILD,
                        "",
                        POM_ELEMENT_REPORTING,
                        "",
                        POM_ELEMENT_PROFILES));
        elementOrder.put(
                POM_ELEMENT_PROFILE,
                asList(
                        POM_ELEMENT_ID,
                        POM_ELEMENT_ACTIVATION,
                        POM_ELEMENT_MODULES,
                        POM_ELEMENT_DISTRIBUTION_MANAGEMENT,
                        POM_ELEMENT_PROPERTIES,
                        POM_ELEMENT_DEPENDENCY_MANAGEMENT,
                        POM_ELEMENT_DEPENDENCIES,
                        POM_ELEMENT_REPOSITORIES,
                        POM_ELEMENT_PLUGIN_REPOSITORIES,
                        POM_ELEMENT_BUILD,
                        POM_ELEMENT_REPORTING));
        elementOrder.put(
                POM_ELEMENT_DEPENDENCY,
                asList(
                        POM_ELEMENT_GROUP_ID,
                        POM_ELEMENT_ARTIFACT_ID,
                        POM_ELEMENT_VERSION,
                        POM_ELEMENT_CLASSIFIER,
                        POM_ELEMENT_TYPE,
                        POM_ELEMENT_SCOPE,
                        POM_ELEMENT_SYSTEM_PATH,
                        POM_ELEMENT_OPTIONAL,
                        POM_ELEMENT_EXCLUSIONS));
    }

    @Override
    public List<String> getElementOrder(String type) {
        return elementOrder.get(type);
    }

    @Override
    public void setElementOrder(String type, List<String> elementOrder) {
        this.elementOrder.put(type, elementOrder);
    }
}
