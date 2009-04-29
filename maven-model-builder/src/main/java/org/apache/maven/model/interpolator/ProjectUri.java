package org.apache.maven.model.interpolator;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Defines all the unique ids for canonical data model.
 */
final class ProjectUri
{

    /*
     * NOTE: The strings defined here are deliberately non-final to prevent the compiler from inlining them when
     * downstream code compiles against them.
     */

    public static final String baseUri = "http://apache.org/maven";

    public static final String xUri = "http://apache.org/maven/project";

    public static final class Parent
    {
        public static final String xUri = "http://apache.org/maven/project/parent";

        public static final String artifactId = "http://apache.org/maven/project/parent/artifactId";

        public static final String groupId = "http://apache.org/maven/project/parent/groupId";

        public static final String version = "http://apache.org/maven/project/parent/version";

        public static final String relativePath = "http://apache.org/maven/project/parent/relativePath";
    }

    public static final String modelVersion = "http://apache.org/maven/project/modelVersion";

    public static final String groupId = "http://apache.org/maven/project/groupId";

    public static final String artifactId = "http://apache.org/maven/project/artifactId";

    public static final String packaging = "http://apache.org/maven/project/packaging";

    public static final String name = "http://apache.org/maven/project/name";

    public static final String version = "http://apache.org/maven/project/version";

    public static final String description = "http://apache.org/maven/project/description";

    public static final String url = "http://apache.org/maven/project/url";

    public static final class Prerequisites
    {
        public static final String xUri = "http://apache.org/maven/project/prerequisites";

        public static final String maven = "http://apache.org/maven/project/prerequisites/maven";
    }

    public static final class IssueManagement
    {
        public static final String xUri = "http://apache.org/maven/project/issueManagement";

        public static final String system = "http://apache.org/maven/project/issueManagement/system";

        public static final String url = "http://apache.org/maven/project/issueManagement/url";
    }

    public static final class CiManagement
    {
        public static final String xUri = "http://apache.org/maven/project/ciManagement";

        public static final String system = "http://apache.org/maven/project/ciManagement/system";

        public static final String url = "http://apache.org/maven/project/ciManagement/url";

        public static final class Notifiers
        {
            public static final String xUri = "http://apache.org/maven/project/ciManagement/notifiers#collection";

            public static final class Notifier
            {
                public static final String xUri =
                    "http://apache.org/maven/project/ciManagement/notifiers#collection/notifier";

                public static final String type =
                    "http://apache.org/maven/project/ciManagement/notifiers#collection/notifier/type";

                public static final String sendOnError =
                    "http://apache.org/maven/project/ciManagement/notifiers#collection/notifier/sendOnError";

                public static final String sendOnFailure =
                    "http://apache.org/maven/project/ciManagement/notifiers#collection/notifier/sendOnFailure";

                public static final String sendOnSuccess =
                    "http://apache.org/maven/project/ciManagement/notifiers#collection/notifier/sendOnSuccess";

                public static final String sendOnWarning =
                    "http://apache.org/maven/project/ciManagement/notifiers#collection/notifier/sendOnWarning";

                public static final String address =
                    "http://apache.org/maven/project/ciManagement/notifiers#collection/notifier/address";

                public static final String configuration =
                    "http://apache.org/maven/project/ciManagement/notifiers#collection/notifier/configuration#set";
            }
        }
    }

    public static final String inceptionYear = "http://apache.org/maven/project/inceptionYear";

    public static final class MailingLists
    {
        public static final String xUri = "http://apache.org/maven/project/mailingLists#collection";

        public static final class MailingList
        {
            public static final String xUri = "http://apache.org/maven/project/mailingLists#collection/mailingList";

            public static final String name = "http://apache.org/maven/project/mailingLists#collection/mailingList/name";

            public static final String subscribe =
                "http://apache.org/maven/project/mailingLists#collection/mailingList/subscribe";

            public static final String unsubscribe =
                "http://apache.org/maven/project/mailingLists#collection/mailingList/unsubscribe";

            public static final String post = "http://apache.org/maven/project/mailingLists#collection/mailingList/post";

            public static final String archive =
                "http://apache.org/maven/project/mailingLists#collection/mailingList/archive";

            public static final String otherArchives =
                "http://apache.org/maven/project/mailingLists#collection/mailingList/otherArchives";
        }
    }

    public static final class Developers
    {
        public static final String xUri = "http://apache.org/maven/project/developers#collection";

        public static final class Developer
        {
            public static final String xUri = "http://apache.org/maven/project/developers#collection/developer";

            public static final String id = "http://apache.org/maven/project/developers#collection/developer/id";

            public static final String name = "http://apache.org/maven/project/developers#collection/developer/name";

            public static final String email = "http://apache.org/maven/project/developers#collection/developer/email";

            public static final String url = "http://apache.org/maven/project/developers#collection/developer/url";

            public static final String organization =
                "http://apache.org/maven/project/developers#collection/developer/organization";

            public static final String organizationUrl =
                "http://apache.org/maven/project/developers#collection/developer/organizationUrl";

            public static final String roles =
                "http://apache.org/maven/project/developers#collection/developer/roles#collection";

            public static final String timezone = "http://apache.org/maven/project/developers#collection/developer/timezone";

            public static final String properties =
                "http://apache.org/maven/project/developers#collection/developer/properties";
        }
    }

    public static final class Contributors
    {
        public static final String xUri = "http://apache.org/maven/project/contributors#collection";

        public static final class Contributor
        {
            public static final String xUri = "http://apache.org/maven/project/contributors#collection/contributor";

            public static final String name = "http://apache.org/maven/project/contributors#collection/contributor/name";

            public static final String email = "http://apache.org/maven/project/contributors#collection/contributor/email";

            public static final String url = "http://apache.org/maven/project/contributors#collection/contributor/url";

            public static final String organization =
                "http://apache.org/maven/project/contributors#collection/contributor/organization";

            public static final String organizationUrl =
                "http://apache.org/maven/project/contributors#collection/contributor/organizationUrl";

            public static final String roles =
                "http://apache.org/maven/project/contributors#collection/contributor/roles#collection";

            public static final String timezone =
                "http://apache.org/maven/project/contributors#collection/contributor/timezone";

            public static final String properties =
                "http://apache.org/maven/project/contributors#collection/contributor/properties";
        }
    }

    public static final class Licenses
    {
        public static final String xUri = "http://apache.org/maven/project/licenses#collection";

        public static final class License
        {
            public static final String xUri = "http://apache.org/maven/project/licenses#collection/license";

            public static final String name = "http://apache.org/maven/project/licenses#collection/license/name";

            public static final String url = "http://apache.org/maven/project/licenses#collection/license/url";

            public static final String distribution =
                "http://apache.org/maven/project/licenses#collection/license/distribution";

            public static final String comments = "http://apache.org/maven/project/licenses#collection/license/comments";
        }
    }

    public static final class Scm
    {
        public static final String xUri = "http://apache.org/maven/project/scm";

        public static final String connection = "http://apache.org/maven/project/scm/connection";

        public static final String developerConnection = "http://apache.org/maven/project/scm/developerConnection";

        public static final String tag = "http://apache.org/maven/project/scm/tag";

        public static final String url = "http://apache.org/maven/project/scm/url";
    }

    public static final class Organization
    {
        public static final String xUri = "http://apache.org/maven/project/organization";

        public static final String name = "http://apache.org/maven/project/organization/name";

        public static final String url = "http://apache.org/maven/project/organization/url";
    }

    public static final class Build
    {
        public static final String xUri = "http://apache.org/maven/project/build";

        public static final String sourceDirectory = "http://apache.org/maven/project/build/sourceDirectory";

        public static final String scriptSourceDirectory = "http://apache.org/maven/project/build/scriptSourceDirectory";

        public static final String testSourceDirectory = "http://apache.org/maven/project/build/testSourceDirectory";

        public static final String outputDirectory = "http://apache.org/maven/project/build/outputDirectory";

        public static final String testOutputDirectory = "http://apache.org/maven/project/build/testOutputDirectory";

        public static final class Extensions
        {
            public static final String xUri = "http://apache.org/maven/project/build/extensions#collection";

            public static final class Extension
            {
                public static final String xUri = "http://apache.org/maven/project/build/extensions#collection/extension";

                public static final String groupId =
                    "http://apache.org/maven/project/build/extensions#collection/extension/groupId";

                public static final String artifactId =
                    "http://apache.org/maven/project/build/extensions#collection/extension/artifactId";

                public static final String version =
                    "http://apache.org/maven/project/build/extensions#collection/extension/version";
            }
        }

        public static final String defaultGoal = "http://apache.org/maven/project/build/defaultGoal";

        public static final class Resources
        {
            public static final String xUri = "http://apache.org/maven/project/build/resources#collection";

            public static final class Resource
            {
                public static final String xUri = "http://apache.org/maven/project/build/resources#collection/resource";

                public static final String targetPath =
                    "http://apache.org/maven/project/build/resources#collection/resource/targetPath";

                public static final String filtering =
                    "http://apache.org/maven/project/build/resources#collection/resource/filtering";

                public static final String directory =
                    "http://apache.org/maven/project/build/resources#collection/resource/directory";

                public static final String includes =
                    "http://apache.org/maven/project/build/resources#collection/resource/includes#collection";

                public static final String excludes =
                    "http://apache.org/maven/project/build/resources#collection/resource/excludes#collection";
            }
        }

        public static final class TestResources
        {
            public static final String xUri = "http://apache.org/maven/project/build/testResources#collection";

            public static final class TestResource
            {
                public static final String xUri =
                    "http://apache.org/maven/project/build/testResources#collection/testResource";

                public static final String targetPath =
                    "http://apache.org/maven/project/build/testResources#collection/testResource/targetPath";

                public static final String filtering =
                    "http://apache.org/maven/project/build/testResources#collection/testResource/filtering";

                public static final String directory =
                    "http://apache.org/maven/project/build/testResources#collection/testResource/directory";

                public static final String excludes =
                    "http://apache.org/maven/project/build/testResources#collection/testResource/excludes";

                public static final class Includes
                {
                    public static final String xUri =
                        "http://apache.org/maven/project/build/testResources#collection/testResource/includes";

                    public static final String include =
                        "http://apache.org/maven/project/build/testResources#collection/testResource/includes/include";
                }
            }
        }

        public static final String directory = "http://apache.org/maven/project/build/directory";

        public static final String finalName = "http://apache.org/maven/project/build/finalName";

        public static final class Filters
        {
            public static final String xUri = "http://apache.org/maven/project/build/filters#collection";

            public static final String filter = xUri + "/filter";
        }

        public static final class PluginManagement
        {
            public static final String xUri = "http://apache.org/maven/project/build/pluginManagement";

            public static final class Plugins
            {
                public static final String xUri = "http://apache.org/maven/project/build/pluginManagement/plugins#collection";

                public static final class Plugin
                {
                    public static final String xUri =
                        "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin";

                    public static final String groupId =
                        "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/groupId";

                    public static final String artifactId =
                        "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/artifactId";

                    public static final String version =
                        "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/version";

                    public static final String extensions =
                        "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/extensions";

                    public static final class Executions
                    {
                        public static final String xUri =
                            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/executions#collection";

                        public static final class Execution
                        {
                            public static final String xUri =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/executions#collection/execution";

                            public static final String id =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/executions#collection/execution/id";

                            public static final String phase =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/executions#collection/execution/phase";

                            public static final class Goals
                            {
                                public static final String xURI =
                                    "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/executions#collection/execution/goals#collection";

                                public static final String goal =
                                    "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/executions#collection/execution/goals#collection/goal";
                            }

                            public static final String inherited =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/executions#collection/execution/inherited";

                            public static final String configuration =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/execution#collection/execution/configuration#set";
                        }
                    }

                    public static final class Dependencies
                    {
                        public static final String xUri =
                            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies#collection";

                        public static final class Dependency
                        {
                            public static final String xUri =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency";

                            public static final String groupId =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/groupId";

                            public static final String artifactId =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/artifactId";

                            public static final String version =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/version";

                            public static final String type =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/type";

                            public static final String classifier =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/classifier";

                            public static final String scope =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/scope";

                            public static final String systemPath =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/systemPath";

                            public static final class Exclusions
                            {
                                public static final String xUri =
                                    "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/exclusions#collection";

                                public static final class Exclusion
                                {
                                    public static final String xUri =
                                        "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/exclusions#collection/exclusion";

                                    public static final String artifactId =
                                        "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/exclusions#collection/exclusion/artifactId";

                                    public static final String groupId =
                                        "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/exclusions#collection/exclusion/groupId";
                                }
                            }

                            public static final String optional =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/optional";
                        }
                    }

                    public static final class Goals
                    {
                        public static final String xURI =
                            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/goals#collection";

                        public static final String goal =
                            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/goals#collection/goal";

                    }

                    public static final String inherited =
                        "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/inherited";

                    public static final String configuration =
                        "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/configuration#set";
                }
            }
        }

        public static final class Plugins
        {
            public static final String xUri = "http://apache.org/maven/project/build/plugins#collection";

            public static final class Plugin
            {
                public static final String xUri = "http://apache.org/maven/project/build/plugins#collection/plugin";

                public static final String groupId =
                    "http://apache.org/maven/project/build/plugins#collection/plugin/groupId";

                public static final String artifactId =
                    "http://apache.org/maven/project/build/plugins#collection/plugin/artifactId";

                public static final String version =
                    "http://apache.org/maven/project/build/plugins#collection/plugin/version";

                public static final String extensions =
                    "http://apache.org/maven/project/build/plugins#collection/plugin/extensions";

                public static final class Executions
                {
                    public static final String xUri =
                        "http://apache.org/maven/project/build/plugins#collection/plugin/executions#collection";

                    public static final class Execution
                    {
                        public static final String xUri =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/executions#collection/execution";

                        public static final String id =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/executions#collection/execution/id";

                        public static final String phase =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/executions#collection/execution/phase";

                        public static final class Goals
                        {
                            public static final String xURI =
                                "http://apache.org/maven/project/build/plugins#collection/plugin/executions#collection/execution/goals#collection";

                            public static final String goal =
                                "http://apache.org/maven/project/build/plugins#collection/plugin/executions#collection/execution/goals#collection/goal";
                        }

                        public static final String inherited =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/executions#collection/execution/inherited";

                        public static final String configuration =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/executions#collection/execution/configuration#set";
                    }
                }

                public static final class Dependencies
                {
                    public static final String xUri =
                        "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection";

                    public static final class Dependency
                    {
                        public static final String xUri =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency";

                        public static final String groupId =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency/groupId";

                        public static final String artifactId =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency/artifactId";

                        public static final String version =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency/version";

                        public static final String type =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency/type";

                        public static final String classifier =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency/classifier";

                        public static final String scope =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency/scope";

                        public static final String systemPath =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency/systemPath";

                        public static final class Exclusions
                        {
                            public static final String xUri =
                                "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency/exclusions#collection";

                            public static final class Exclusion
                            {
                                public static final String xUri =
                                    "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency/exclusions#collection/exclusion";

                                public static final String artifactId =
                                    "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency/exclusions#collection/exclusion/artifactId";

                                public static final String groupId =
                                    "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency/exclusions#collection/exclusion/groupId";
                            }
                        }

                        public static final String optional =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency/optional";
                    }
                }
                public static final class Goals
                {
                    public static final String xURI =
                        "http://apache.org/maven/project/build/plugins#collection/plugin/goals#collection";

                    public static final String goal =
                        "http://apache.org/maven/project/build/plugins#collection/plugin/goals#collection/goal";
                }

                public static final String inherited =
                    "http://apache.org/maven/project/build/plugins#collection/plugin/inherited";

                public static final String configuration =
                    "http://apache.org/maven/project/build/plugins#collection/plugin/configuration#set";
            }
        }
    }

    public static final class Profiles
    {
        public static final String xUri = "http://apache.org/maven/project/profiles#collection";

        public static final class Profile
        {
            public static final String xUri = "http://apache.org/maven/project/profiles#collection/profile";

            public static final String id = "http://apache.org/maven/project/profiles#collection/profile/id";

            public static final class Activation
            {
                public static final String xUri = "http://apache.org/maven/project/profiles#collection/profile/activation";

                public static final String activeByDefault =
                    "http://apache.org/maven/project/profiles#collection/profile/activation/activeByDefault";

                public static final String jdk = "http://apache.org/maven/project/profiles#collection/profile/activation/jdk";

                public static final class Os
                {
                    public static final String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/activation/os";

                    public static final String name =
                        "http://apache.org/maven/project/profiles#collection/profile/activation/os/name";

                    public static final String family =
                        "http://apache.org/maven/project/profiles#collection/profile/activation/os/family";

                    public static final String arch =
                        "http://apache.org/maven/project/profiles#collection/profile/activation/os/arch";

                    public static final String version =
                        "http://apache.org/maven/project/profiles#collection/profile/activation/os/version";
                }

                public static final class Property
                {
                    public static final String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/activation/property";

                    public static final String name =
                        "http://apache.org/maven/project/profiles#collection/profile/activation/property/name";

                    public static final String value =
                        "http://apache.org/maven/project/profiles#collection/profile/activation/property/value";
                }

                public static final class File
                {
                    public static final String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/activation/file";

                    public static final String missing =
                        "http://apache.org/maven/project/profiles#collection/profile/activation/file/missing";

                    public static final String exists =
                        "http://apache.org/maven/project/profiles#collection/profile/activation/file/exists";
                }
            }

            public static final class Build
            {
                public static final String xUri = "http://apache.org/maven/project/profiles#collection/profile/build";

                public static final String defaultGoal =
                    "http://apache.org/maven/project/profiles#collection/profile/build/defaultGoal";

                public static final class Resources
                {
                    public static final String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/build/resources#collection";

                    public static final class Resource
                    {
                        public static final String xUri =
                            "http://apache.org/maven/project/profiles#collection/profile/build/resources#collection/resource";

                        public static final String targetPath =
                            "http://apache.org/maven/project/profiles#collection/profile/build/resources#collection/resource/targetPath";

                        public static final String filtering =
                            "http://apache.org/maven/project/profiles#collection/profile/build/resources#collection/resource/filtering";

                        public static final String directory =
                            "http://apache.org/maven/project/profiles#collection/profile/build/resources#collection/resource/directory";

                        public static final String includes =
                            "http://apache.org/maven/project/profiles#collection/profile/build/resources#collection/resource/includes#collection";

                        public static final String excludes =
                            "http://apache.org/maven/project/profiles#collection/profile/build/resources#collection/resource/excludes#collection";
                    }
                }

                public static final class TestResources
                {
                    public static final String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/build/testResources#collection";

                    public static final class TestResource
                    {
                        public static final String xUri =
                            "http://apache.org/maven/project/profiles#collection/profile/build/testResources#collection/testResource";

                        public static final String targetPath =
                            "http://apache.org/maven/project/profiles#collection/profile/build/testResources#collection/testResource/targetPath";

                        public static final String filtering =
                            "http://apache.org/maven/project/profiles#collection/profile/build/testResources#collection/testResource/filtering";

                        public static final String directory =
                            "http://apache.org/maven/project/profiles#collection/profile/build/testResources#collection/testResource/directory";

                        public static final String includes =
                            "http://apache.org/maven/project/profiles#collection/profile/build/testResources#collection/testResource/includes#collection";

                        public static final String excludes =
                            "http://apache.org/maven/project/profiles#collection/profile/build/testResources#collection/testResource/excludes#collection";

                    }
                }

                public static final String directory =
                    "http://apache.org/maven/project/profiles#collection/profile/build/directory";

                public static final String finalName =
                    "http://apache.org/maven/project/profiles#collection/profile/build/finalName";

                public static final String filters =
                    "http://apache.org/maven/project/profiles#collection/profile/build/filters";

                public static final class PluginManagement
                {
                    public static final String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement";

                    public static final class Plugins
                    {
                        public static final String xUri =
                            "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection";

                        public static final class Plugin
                        {
                            public static final String xUri =
                                "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin";

                            public static final String groupId =
                                "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/groupId";

                            public static final String artifactId =
                                "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/artifactId";

                            public static final String version =
                                "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/version";

                            public static final String extensions =
                                "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/extensions";

                            public static final class Executions
                            {
                                public static final String xUri =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/executions#collection";

                                public static final class Execution
                                {
                                    public static final String xUri =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/executions#collection/execution";

                                    public static final String id =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/executions#collection/execution/id";

                                    public static final String phase =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/executions#collection/execution/phase";

                                     public static final class Goals
                                     {
                                        public static final String xURI =
                                            "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/executions#collection/execution/goals#collection";

                                        public static final String goal =
                                             "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/executions#collection/execution/goals#collection/goal";
                                     }

                                    public static final String inherited =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/executions#collection/execution/inherited";

                                    public static final String configuration =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/executions#collection/execution/configuration#set";
                                }
                            }

                            public static final class Dependencies
                            {
                                public static final String xUri =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/dependencies#collection";

                                public static final class Dependency
                                {
                                    public static final String xUri =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency";

                                    public static final String groupId =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/groupId";

                                    public static final String artifactId =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/artifactId";

                                    public static final String version =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/version";

                                    public static final String type =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/type";

                                    public static final String classifier =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/classifier";

                                    public static final String scope =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/scope";

                                    public static final String systemPath =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/systemPath";

                                    public static final class Exclusions
                                    {
                                        public static final String xUri =
                                            "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/exclusions#collection";

                                        public static final class Exclusion
                                        {
                                            public static final String xUri =
                                                "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/exclusions#collection/exclusion";

                                            public static final String artifactId =
                                                "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/exclusions#collection/exclusion/artifactId";

                                            public static final String groupId =
                                                "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/dependencies#collection/dependency/exclusions#collection/exclusion/groupId";
                                        }
                                    }

                                    public static final String optional =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/dependencies#collection/dependency/optional";
                                }
                            }

                            public static final class Goals
                            {
                                public static final String xURI =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/goals#collection";

                                public static final String goal =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/goals#collection/goal";
                            }

                            public static final String inherited =
                                "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/inherited";

                            public static final String configuration =
                                "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins#collection/plugin/configuration#set";
                        }
                    }
                }

                public static final class Plugins
                {
                    public static final String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection";

                    public static final class Plugin
                    {
                        public static final String xUri =
                            "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin";

                        public static final String groupId =
                            "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/groupId";

                        public static final String artifactId =
                            "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/artifactId";

                        public static final String version =
                            "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/version";

                        public static final String extensions =
                            "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/extensions#collection";

                        public static final class Executions
                        {
                            public static final String xUri =
                                "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/executions#collection";

                            public static final class Execution
                            {
                                public static final String xUri =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/executions#collection/execution";

                                public static final String id =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/executions#collection/execution/id";

                                public static final String phase =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/executions#collection/execution/phase";

                                public static final class Goals
                                {
                                    public static final String xURI =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/executions#collection/execution/goals#collection";

                                    public static final String goal =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/executions#collection/execution/goals#collection/goal";
                                }

                                public static final String inherited =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/executions#collection/execution/inherited";

                                public static final String configuration =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/executions#collection/execution/configuration#set";
                            }
                        }

                        public static final class Dependencies
                        {
                            public static final String xUri =
                                "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/dependencies#collection";

                            public static final class Dependency
                            {
                                public static final String xUri =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/dependencies#collection/dependency";

                                public static final String groupId =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/dependencies#collection/dependency/groupId";

                                public static final String artifactId =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/dependencies#collection/dependency/artifactId";

                                public static final String version =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/dependencies#collection/dependency/version";

                                public static final String type =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/dependencies#collection/dependency/type";

                                public static final String classifier =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/dependencies#collection/dependency/classifier";

                                public static final String scope =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/dependencies#collection/dependency/scope";

                                public static final String systemPath =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/dependencies#collection/dependency/systemPath";

                                public static final class Exclusions
                                {
                                    public static final String xUri =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/dependencies#collection/dependency/exclusions#collection";

                                    public static final class Exclusion
                                    {
                                        public static final String xUri =
                                            "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/dependencies#collection/dependency/exclusions#collection/exclusion";

                                        public static final String artifactId =
                                            "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/dependencies#collection/dependency/exclusions#collection/exclusion/artifactId";

                                        public static final String groupId =
                                            "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/dependencies#collection/dependency/exclusions#collection/exclusion/groupId";
                                    }
                                }

                                public static final String optional =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/dependencies#collection/dependency/optional";
                            }
                        }

                        public static final class Goals
                        {
                            public static final String xURI =
                                "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/goals#collection";

                            public static final String goal =
                                "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/goals#collection/goal";
                        }

                        public static final String inherited =
                            "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/inherited";

                        public static final String configuration =
                            "http://apache.org/maven/project/profiles#collection/profile/build/plugins#collection/plugin/configuration#set";
                    }
                }
            }

            public static final String modules = "http://apache.org/maven/project/profiles#collection/profile/modules#collection";

            public static final class Repositories
            {
                public static final String xUri = "http://apache.org/maven/project/profiles#collection/profile/repositories#collection";

                public static final class Repository
                {
                    public static final String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/repositories#collection/repository";

                    public static final class Releases
                    {
                        public static final String xUri =
                            "http://apache.org/maven/project/profiles#collection/profile/repositories#collection/repository/releases";

                        public static final String enabled =
                            "http://apache.org/maven/project/profiles#collection/profile/repositories#collection/repository/releases/enabled";

                        public static final String updatePolicy =
                            "http://apache.org/maven/project/profiles#collection/profile/repositories#collection/repository/releases/updatePolicy";

                        public static final String checksumPolicy =
                            "http://apache.org/maven/project/profiles#collection/profile/repositories#collection/repository/releases/checksumPolicy";
                    }

                    public static final class Snapshots
                    {
                        public static final String xUri =
                            "http://apache.org/maven/project/profiles#collection/profile/repositories#collection/repository/snapshots";

                        public static final String enabled =
                            "http://apache.org/maven/project/profiles#collection/profile/repositories#collection/repository/snapshots/enabled";

                        public static final String updatePolicy =
                            "http://apache.org/maven/project/profiles#collection/profile/repositories#collection/repository/snapshots/updatePolicy";

                        public static final String checksumPolicy =
                            "http://apache.org/maven/project/profiles#collection/profile/repositories#collection/repository/snapshots/checksumPolicy";
                    }

                    public static final String id =
                        "http://apache.org/maven/project/profiles#collection/profile/repositories#collection/repository/id";

                    public static final String name =
                        "http://apache.org/maven/project/profiles#collection/profile/repositories#collection/repository/name";

                    public static final String url =
                        "http://apache.org/maven/project/profiles#collection/profile/repositories#collection/repository/url";

                    public static final String layout =
                        "http://apache.org/maven/project/profiles#collection/profile/repositories#collection/repository/layout";
                }
            }

            public static final class PluginRepositories
            {
                public static final String xUri =
                    "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories#collection";

                public static final class PluginRepository
                {
                    public static final String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories#collection/pluginRepository";

                    public static final class Releases
                    {
                        public static final String xUri =
                            "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories#collection/pluginRepository/releases";

                        public static final String enabled =
                            "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories#collection/pluginRepository/releases/enabled";

                        public static final String updatePolicy =
                            "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories#collection/pluginRepository/releases/updatePolicy";

                        public static final String checksumPolicy =
                            "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories#collection/pluginRepository/releases/checksumPolicy";
                    }

                    public static final class Snapshots
                    {
                        public static final String xUri =
                            "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories#collection/pluginRepository/snapshots";

                        public static final String enabled =
                            "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories#collection/pluginRepository/snapshots/enabled";

                        public static final String updatePolicy =
                            "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories#collection/pluginRepository/snapshots/updatePolicy";

                        public static final String checksumPolicy =
                            "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories#collection/pluginRepository/snapshots/checksumPolicy";
                    }

                    public static final String id =
                        "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories#collection/pluginRepository/id";

                    public static final String name =
                        "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories#collection/pluginRepository/name";

                    public static final String url =
                        "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories#collection/pluginRepository/url";

                    public static final String layout =
                        "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories#collection/pluginRepository/layout";
                }
            }

            public static final class Dependencies
            {
                public static final String xUri = "http://apache.org/maven/project/profiles#collection/profile/dependencies#collection";

                public static final class Dependency
                {
                    public static final String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/dependencies#collection/dependency";

                    public static final String groupId =
                        "http://apache.org/maven/project/profiles#collection/profile/dependencies#collection/dependency/groupId";

                    public static final String artifactId =
                        "http://apache.org/maven/project/profiles#collection/profile/dependencies#collection/dependency/artifactId";

                    public static final String version =
                        "http://apache.org/maven/project/profiles#collection/profile/dependencies#collection/dependency/version";

                    public static final String type =
                        "http://apache.org/maven/project/profiles#collection/profile/dependencies#collection/dependency/type";

                    public static final String classifier =
                        "http://apache.org/maven/project/profiles#collection/profile/dependencies#collection/dependency/classifier";

                    public static final String scope =
                        "http://apache.org/maven/project/profiles#collection/profile/dependencies#collection/dependency/scope";

                    public static final String systemPath =
                        "http://apache.org/maven/project/profiles#collection/profile/dependencies#collection/dependency/systemPath";

                    public static final class Exclusions
                    {
                        public static final String xUri =
                            "http://apache.org/maven/project/profiles#collection/profile/dependencies#collection/dependency/exclusions#collection";

                        public static final class Exclusion
                        {
                            public static final String xUri =
                                "http://apache.org/maven/project/profiles#collection/profile/dependencies#collection/dependency/exclusions#collection/exclusion";

                            public static final String artifactId =
                                "http://apache.org/maven/project/profiles#collection/profile/dependencies#collection/dependency/exclusions#collection/exclusion/artifactId";

                            public static final String groupId =
                                "http://apache.org/maven/project/profiles#collection/profile/dependencies#collection/dependency/exclusions#collection/exclusion/groupId";
                        }
                    }

                    public static final String optional =
                        "http://apache.org/maven/project/profiles#collection/profile/dependencies#collection/dependency/optional";
                }
            }

            public static final String reports = "http://apache.org/maven/project/profiles#collection/profile/reports";

            public static final class Reporting
            {
                public static final String xUri = "http://apache.org/maven/project/profiles#collection/profile/reporting";

                public static final String excludeDefaults =
                    "http://apache.org/maven/project/profiles#collection/profile/reporting/excludeDefaults";

                public static final String outputDirectory =
                    "http://apache.org/maven/project/profiles#collection/profile/reporting/outputDirectory";

                public static final class Plugins
                {
                    public static final String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins#collection";

                    public static final class Plugin
                    {
                        public static final String xUri =
                            "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins#collection/plugin";

                        public static final String groupId =
                            "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins#collection/plugin/groupId";

                        public static final String artifactId =
                            "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins#collection/plugin/artifactId";

                        public static final String version =
                            "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins#collection/plugin/version";

                        public static final String inherited =
                            "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins#collection/plugin/inherited";

                        public static final String configuration =
                            "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins#collection/plugin/configuration#set";

                        public static final class ReportSets
                        {
                            public static final String xUri =
                                "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins#collection/plugin/reportSets#collection";

                            public static final class ReportSet
                            {
                                public static final String xUri =
                                    "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins#collection/plugin/reportSets#collection/reportSet";

                                public static final String id =
                                    "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins#collection/plugin/reportSets#collection/reportSet/id";

                                public static final String configuration =
                                    "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins#collection/plugin/reportSets#collection/reportSet/configuration#set";

                                public static final String inherited =
                                    "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins#collection/plugin/reportSets#collection/reportSet/inherited";

                                public static final String reports =
                                    "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins#collection/plugin/reportSets#collection/reportSet/reports";
                            }
                        }
                    }
                }
            }

            public static final class DependencyManagement
            {
                public static final String xUri =
                    "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement";

                public static final class Dependencies
                {
                    public static final String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection";

                    public static final class Dependency
                    {
                        public static final String xUri =
                            "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency";

                        public static final String groupId =
                            "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency/groupId";

                        public static final String artifactId =
                            "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency/artifactId";

                        public static final String version =
                            "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency/version";

                        public static final String type =
                            "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency/type";

                        public static final String classifier =
                            "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency/classifier";

                        public static final String scope =
                            "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency/scope";

                        public static final String systemPath =
                            "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency/systemPath";

                        public static final class Exclusions
                        {
                            public static final String xUri =
                                "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency/exclusions#collection";

                            public static final class Exclusion
                            {
                                public static final String xUri =
                                    "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency/exclusions#collection/exclusion";

                                public static final String artifactId =
                                    "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency/exclusions#collection/exclusion/artifactId";

                                public static final String groupId =
                                    "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency/exclusions#collection/exclusion/groupId";
                            }
                        }

                        public static final String optional =
                            "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency/optional";
                    }
                }
            }

            public static final class DistributionManagement
            {
                public static final String xUri =
                    "http://apache.org/maven/project/profiles#collection/profile/distributionManagement";

                public static final class Repository
                {
                    public static final String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/repository";

                    public static final String uniqueVersion =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/repository/uniqueVersion";

                    public static final String id =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/repository/id";

                    public static final String name =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/repository/name";

                    public static final String url =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/repository/url";

                    public static final String layout =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/repository/layout";
                }

                public static final class SnapshotRepository
                {
                    public static final String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/snapshotRepository";

                    public static final String uniqueVersion =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/snapshotRepository/uniqueVersion";

                    public static final String id =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/snapshotRepository/id";

                    public static final String name =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/snapshotRepository/name";

                    public static final String url =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/snapshotRepository/url";

                    public static final String layout =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/snapshotRepository/layout";
                }

                public static final class Site
                {
                    public static final String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/site";

                    public static final String id =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/site/id";

                    public static final String name =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/site/name";

                    public static final String url =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/site/url";
                }

                public static final String downloadUrl =
                    "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/downloadUrl";

                public static final class Relocation
                {
                    public static final String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/relocation";

                    public static final String groupId =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/relocation/groupId";

                    public static final String artifactId =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/relocation/artifactId";

                    public static final String version =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/relocation/version";

                    public static final String message =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/relocation/message";
                }

                public static final String status =
                    "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/status";
            }

            public static final String properties = "http://apache.org/maven/project/profiles#collection/profile/properties";
        }
    }

    public static final class Modules
    {
        public static final String xUri = "http://apache.org/maven/project/modules#collection";

        public static final String module = "http://apache.org/maven/project/modules#collection/module";
    }

    public static final class Repositories
    {
        public static final String xUri = "http://apache.org/maven/project/repositories#collection";

        public static final class Repository
        {
            public static final String xUri = "http://apache.org/maven/project/repositories#collection/repository";

            public static final class Releases
            {
                public static final String xUri =
                    "http://apache.org/maven/project/repositories#collection/repository/releases";

                public static final String enabled =
                    "http://apache.org/maven/project/repositories#collection/repository/releases/enabled";

                public static final String updatePolicy =
                    "http://apache.org/maven/project/repositories#collection/repository/releases/updatePolicy";

                public static final String checksumPolicy =
                    "http://apache.org/maven/project/repositories#collection/repository/releases/checksumPolicy";
            }

            public static final class Snapshots
            {
                public static final String xUri =
                    "http://apache.org/maven/project/repositories#collection/repository/snapshots";

                public static final String enabled =
                    "http://apache.org/maven/project/repositories#collection/repository/snapshots/enabled";

                public static final String updatePolicy =
                    "http://apache.org/maven/project/repositories#collection/repository/snapshots/updatePolicy";

                public static final String checksumPolicy =
                    "http://apache.org/maven/project/repositories#collection/repository/snapshots/checksumPolicy";
            }

            public static final String id = "http://apache.org/maven/project/repositories#collection/repository/id";

            public static final String name = "http://apache.org/maven/project/repositories#collection/repository/name";

            public static final String url = "http://apache.org/maven/project/repositories#collection/repository/url";

            public static final String layout = "http://apache.org/maven/project/repositories#collection/repository/layout";
        }
    }

    public static final class PluginRepositories
    {
        public static final String xUri = "http://apache.org/maven/project/pluginRepositories#collection";

        public static final class PluginRepository
        {
            public static final String xUri =
                "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository";

            public static final class Releases
            {
                public static final String xUri =
                    "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository/releases";

                public static final String enabled =
                    "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository/releases/enabled";

                public static final String updatePolicy =
                    "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository/releases/updatePolicy";

                public static final String checksumPolicy =
                    "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository/releases/checksumPolicy";
            }

            public static final class Snapshots
            {
                public static final String xUri =
                    "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository/snapshots";

                public static final String enabled =
                    "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository/snapshots/enabled";

                public static final String updatePolicy =
                    "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository/snapshots/updatePolicy";

                public static final String checksumPolicy =
                    "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository/snapshots/checksumPolicy";
            }

            public static final String id =
                "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository/id";

            public static final String name =
                "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository/name";

            public static final String url =
                "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository/url";

            public static final String layout =
                "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository/layout";
        }
    }

    public static final class Dependencies
    {
        public static final String xUri = "http://apache.org/maven/project/dependencies#collection";

        public static final class Dependency
        {
            public static final String xUri = "http://apache.org/maven/project/dependencies#collection/dependency";

            public static final String groupId = "http://apache.org/maven/project/dependencies#collection/dependency/groupId";

            public static final String artifactId =
                "http://apache.org/maven/project/dependencies#collection/dependency/artifactId";

            public static final String version = "http://apache.org/maven/project/dependencies#collection/dependency/version";

            public static final String type = "http://apache.org/maven/project/dependencies#collection/dependency/type";

            public static final String classifier =
                "http://apache.org/maven/project/dependencies#collection/dependency/classifier";

            public static final String scope = "http://apache.org/maven/project/dependencies#collection/dependency/scope";

            public static final String systemPath =
                "http://apache.org/maven/project/dependencies#collection/dependency/systemPath";

            public static final class Exclusions
            {
                public static final String xUri =
                    "http://apache.org/maven/project/dependencies#collection/dependency/exclusions#collection";

                public static final class Exclusion
                {
                    public static final String xUri =
                        "http://apache.org/maven/project/dependencies#collection/dependency/exclusions#collection/exclusion";

                    public static final String artifactId =
                        "http://apache.org/maven/project/dependencies#collection/dependency/exclusions#collection/exclusion/artifactId";

                    public static final String groupId =
                        "http://apache.org/maven/project/dependencies#collection/dependency/exclusions#collection/exclusion/groupId";
                }
            }

            public static final String optional =
                "http://apache.org/maven/project/dependencies#collection/dependency/optional";
        }
    }

    public static final String reports = "http://apache.org/maven/project/reports";

    public static final class Reporting
    {
        public static final String xUri = "http://apache.org/maven/project/reporting";

        public static final String excludeDefaults = "http://apache.org/maven/project/reporting/excludeDefaults";

        public static final String outputDirectory = "http://apache.org/maven/project/reporting/outputDirectory";

        public static final class Plugins
        {
            public static final String xUri = "http://apache.org/maven/project/reporting/plugins#collection";

            public static final class Plugin
            {
                public static final String xUri = "http://apache.org/maven/project/reporting/plugins#collection/plugin";

                public static final String groupId =
                    "http://apache.org/maven/project/reporting/plugins#collection/plugin/groupId";

                public static final String artifactId =
                    "http://apache.org/maven/project/reporting/plugins#collection/plugin/artifactId";

                public static final String version =
                    "http://apache.org/maven/project/reporting/plugins#collection/plugin/version";

                public static final String inherited =
                    "http://apache.org/maven/project/reporting/plugins#collection/plugin/inherited";

                public static final String configuration =
                    "http://apache.org/maven/project/reporting/plugins#collection/plugin/configuration#set";

                public static final class ReportSets
                {
                    public static final String xUri =
                        "http://apache.org/maven/project/reporting/plugins#collection/plugin/reportSets#collection";

                    public static final class ReportSet
                    {
                        public static final String xUri =
                            "http://apache.org/maven/project/reporting/plugins#collection/plugin/reportSets#collection/reportSet";

                        public static final String id =
                            "http://apache.org/maven/project/reporting/plugins#collection/plugin/reportSets#collection/reportSet/id";

                        public static final String configuration =
                            "http://apache.org/maven/project/reporting/plugins#collection/plugin/reportSets#collection/reportSet/configuration#set";

                        public static final String inherited =
                            "http://apache.org/maven/project/reporting/plugins#collection/plugin/reportSets#collection/reportSet/inherited";

                        public static final String reports =
                            "http://apache.org/maven/project/reporting/plugins#collection/plugin/reportSets#collection/reportSet/reports";
                    }
                }
            }
        }
    }

    public static final class DependencyManagement
    {
        public static final String xUri = "http://apache.org/maven/project/dependencyManagement";

        public static final class Dependencies
        {
            public static final String xUri = "http://apache.org/maven/project/dependencyManagement/dependencies#collection";

            public static final class Dependency
            {
                public static final String xUri =
                    "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency";

                public static final String groupId =
                    "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency/groupId";

                public static final String artifactId =
                    "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency/artifactId";

                public static final String version =
                    "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency/version";

                public static final String type =
                    "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency/type";

                public static final String classifier =
                    "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency/classifier";

                public static final String scope =
                    "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency/scope";

                public static final String systemPath =
                    "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency/systemPath";

                public static final class Exclusions
                {
                    public static final String xUri =
                        "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency/exclusions#collection";

                    public static final class Exclusion
                    {
                        public static final String xUri =
                            "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency/exclusions#collection/exclusion";

                        public static final String artifactId =
                            "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency/exclusions#collection/exclusion/artifactId";

                        public static final String groupId =
                            "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency/exclusions#collection/exclusion/groupId";
                    }
                }

                public static final String optional =
                    "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency/optional";
            }
        }
    }

    public static final class DistributionManagement
    {
        public static final String xUri = "http://apache.org/maven/project/distributionManagement";

        public static final class Repository
        {
            public static final String xUri = "http://apache.org/maven/project/distributionManagement/repository";

            public static final String uniqueVersion =
                "http://apache.org/maven/project/distributionManagement/repository/uniqueVersion";

            public static final String id = "http://apache.org/maven/project/distributionManagement/repository/id";

            public static final String name = "http://apache.org/maven/project/distributionManagement/repository/name";

            public static final String url = "http://apache.org/maven/project/distributionManagement/repository/url";

            public static final String layout = "http://apache.org/maven/project/distributionManagement/repository/layout";
        }

        public static final class SnapshotRepository
        {
            public static final String xUri = "http://apache.org/maven/project/distributionManagement/snapshotRepository";

            public static final String uniqueVersion =
                "http://apache.org/maven/project/distributionManagement/snapshotRepository/uniqueVersion";

            public static final String id = "http://apache.org/maven/project/distributionManagement/snapshotRepository/id";

            public static final String name =
                "http://apache.org/maven/project/distributionManagement/snapshotRepository/name";

            public static final String url = "http://apache.org/maven/project/distributionManagement/snapshotRepository/url";

            public static final String layout =
                "http://apache.org/maven/project/distributionManagement/snapshotRepository/layout";
        }

        public static final class Site
        {
            public static final String xUri = "http://apache.org/maven/project/distributionManagement/site";

            public static final String id = "http://apache.org/maven/project/distributionManagement/site/id";

            public static final String name = "http://apache.org/maven/project/distributionManagement/site/name";

            public static final String url = "http://apache.org/maven/project/distributionManagement/site/url";
        }

        public static final String downloadUrl = "http://apache.org/maven/project/distributionManagement/downloadUrl";

        public static final class Relocation
        {
            public static final String xUri = "http://apache.org/maven/project/distributionManagement/relocation";

            public static final String groupId = "http://apache.org/maven/project/distributionManagement/relocation/groupId";

            public static final String artifactId =
                "http://apache.org/maven/project/distributionManagement/relocation/artifactId";

            public static final String version = "http://apache.org/maven/project/distributionManagement/relocation/version";

            public static final String message = "http://apache.org/maven/project/distributionManagement/relocation/message";
        }

        public static final String status = "http://apache.org/maven/project/distributionManagement/status";
    }

    public static final String properties = "http://apache.org/maven/project/properties";

}
