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
package org.apache.maven.cling.invoker.mvnup.goals;

/**
 * Constants used throughout the Maven upgrade tools.
 * Organized into logical groups for better maintainability.
 */
public final class UpgradeConstants {

    private UpgradeConstants() {
        // Utility class
    }

    /**
     * Maven model version constants.
     */
    public static final class ModelVersions {
        /** Maven 4.0.0 model version */
        public static final String MODEL_VERSION_4_0_0 = "4.0.0";

        /** Maven 4.1.0 model version */
        public static final String MODEL_VERSION_4_1_0 = "4.1.0";

        private ModelVersions() {
            // Utility class
        }
    }

    /**
     * Common XML element names used in Maven POMs.
     */
    public static final class XmlElements {
        // Core POM elements
        public static final String MODEL_VERSION = "modelVersion";
        public static final String GROUP_ID = "groupId";
        public static final String ARTIFACT_ID = "artifactId";
        public static final String VERSION = "version";
        public static final String PARENT = "parent";
        public static final String RELATIVE_PATH = "relativePath";
        public static final String PACKAGING = "packaging";
        public static final String NAME = "name";
        public static final String DESCRIPTION = "description";
        public static final String URL = "url";

        // Build elements
        public static final String BUILD = "build";
        public static final String PLUGINS = "plugins";
        public static final String PLUGIN = "plugin";
        public static final String PLUGIN_MANAGEMENT = "pluginManagement";
        public static final String DEFAULT_GOAL = "defaultGoal";
        public static final String DIRECTORY = "directory";
        public static final String FINAL_NAME = "finalName";
        public static final String SOURCE_DIRECTORY = "sourceDirectory";
        public static final String SCRIPT_SOURCE_DIRECTORY = "scriptSourceDirectory";
        public static final String TEST_SOURCE_DIRECTORY = "testSourceDirectory";
        public static final String OUTPUT_DIRECTORY = "outputDirectory";
        public static final String TEST_OUTPUT_DIRECTORY = "testOutputDirectory";
        public static final String EXTENSIONS = "extensions";
        public static final String EXECUTIONS = "executions";
        public static final String GOALS = "goals";
        public static final String INHERITED = "inherited";
        public static final String CONFIGURATION = "configuration";

        // Module elements
        public static final String MODULES = "modules";
        public static final String MODULE = "module";
        public static final String SUBPROJECTS = "subprojects";
        public static final String SUBPROJECT = "subproject";

        // Dependency elements
        public static final String DEPENDENCIES = "dependencies";
        public static final String DEPENDENCY = "dependency";
        public static final String DEPENDENCY_MANAGEMENT = "dependencyManagement";
        public static final String CLASSIFIER = "classifier";
        public static final String TYPE = "type";
        public static final String SCOPE = "scope";
        public static final String SYSTEM_PATH = "systemPath";
        public static final String OPTIONAL = "optional";
        public static final String EXCLUSIONS = "exclusions";

        // Profile elements
        public static final String PROFILES = "profiles";
        public static final String PROFILE = "profile";

        // Project information elements
        public static final String PROPERTIES = "properties";
        public static final String INCEPTION_YEAR = "inceptionYear";
        public static final String ORGANIZATION = "organization";
        public static final String LICENSES = "licenses";
        public static final String DEVELOPERS = "developers";
        public static final String CONTRIBUTORS = "contributors";
        public static final String MAILING_LISTS = "mailingLists";
        public static final String PREREQUISITES = "prerequisites";
        public static final String SCM = "scm";
        public static final String ISSUE_MANAGEMENT = "issueManagement";
        public static final String CI_MANAGEMENT = "ciManagement";
        public static final String DISTRIBUTION_MANAGEMENT = "distributionManagement";
        public static final String REPOSITORIES = "repositories";
        public static final String PLUGIN_REPOSITORIES = "pluginRepositories";
        public static final String REPOSITORY = "repository";
        public static final String PLUGIN_REPOSITORY = "pluginRepository";
        public static final String REPORTING = "reporting";

        private XmlElements() {
            // Utility class
        }
    }

    /**
     * Common indentation patterns for XML formatting.
     */
    public static final class Indentation {
        public static final String TWO_SPACES = "  ";
        public static final String FOUR_SPACES = "    ";
        public static final String TAB = "\t";
        public static final String DEFAULT = TWO_SPACES;

        private Indentation() {
            // Utility class
        }
    }

    /**
     * Common Maven plugin constants.
     */
    public static final class Plugins {
        /** Default Maven plugin groupId */
        public static final String DEFAULT_MAVEN_PLUGIN_GROUP_ID = "org.apache.maven.plugins";

        /** Maven plugin artifact prefix */
        public static final String MAVEN_PLUGIN_PREFIX = "maven-";

        /** Standard reason for Maven 4 compatibility upgrades */
        public static final String MAVEN_4_COMPATIBILITY_REASON = "Maven 4 compatibility";

        private Plugins() {
            // Utility class
        }
    }

    /**
     * Common file and directory names.
     */
    public static final class Files {
        /** Standard Maven POM file name */
        public static final String POM_XML = "pom.xml";

        /** Maven configuration directory (alternative name) */
        public static final String MVN_DIRECTORY = ".mvn";

        /** Default parent POM relative path */
        public static final String DEFAULT_PARENT_RELATIVE_PATH = "../pom.xml";

        private Files() {
            // Utility class
        }
    }

    /**
     * Maven namespace constants.
     */
    public static final class Namespaces {
        /** Maven 4.0.0 namespace URI */
        public static final String MAVEN_4_0_0_NAMESPACE = "http://maven.apache.org/POM/4.0.0";

        /** Maven 4.1.0 namespace URI */
        public static final String MAVEN_4_1_0_NAMESPACE = "http://maven.apache.org/POM/4.1.0";

        private Namespaces() {
            // Utility class
        }
    }

    /**
     * Schema location constants.
     */
    public static final class SchemaLocations {
        /** Schema location for 4.0.0 models */
        public static final String MAVEN_4_0_0_SCHEMA_LOCATION =
                Namespaces.MAVEN_4_0_0_NAMESPACE + " https://maven.apache.org/xsd/maven-4.0.0.xsd";

        /** Schema location for 4.1.0 models */
        public static final String MAVEN_4_1_0_SCHEMA_LOCATION =
                Namespaces.MAVEN_4_1_0_NAMESPACE + " https://maven.apache.org/xsd/maven-4.1.0.xsd";

        private SchemaLocations() {
            // Utility class
        }
    }

    /**
     * XML attribute constants.
     */
    public static final class XmlAttributes {
        /** Schema location attribute name */
        public static final String SCHEMA_LOCATION = "schemaLocation";

        /** XSI namespace prefix */
        public static final String XSI_NAMESPACE_PREFIX = "xsi";

        /** XSI namespace URI */
        public static final String XSI_NAMESPACE_URI = "http://www.w3.org/2001/XMLSchema-instance";

        // Combine attributes
        public static final String COMBINE_CHILDREN = "combine.children";
        public static final String COMBINE_SELF = "combine.self";

        // Combine attribute values
        public static final String COMBINE_OVERRIDE = "override";
        public static final String COMBINE_MERGE = "merge";
        public static final String COMBINE_APPEND = "append";

        private XmlAttributes() {
            // Utility class
        }
    }
}
