package org.apache.maven.project.builder;

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
public class ProjectUri
{
    public static String baseUri = "http://apache.org/maven";

    public static String xUri = "http://apache.org/maven/project";

    public static class Parent
    {
        public static String xUri = "http://apache.org/maven/project/parent";

        public static String artifactId = "http://apache.org/maven/project/parent/artifactId";

        public static String groupId = "http://apache.org/maven/project/parent/groupId";

        public static String version = "http://apache.org/maven/project/parent/version";

        public static String relativePath = "http://apache.org/maven/project/parent/relativePath";
    }

    public static String modelVersion = "http://apache.org/maven/project/modelVersion";

    public static String groupId = "http://apache.org/maven/project/groupId";

    public static String artifactId = "http://apache.org/maven/project/artifactId";

    public static String packaging = "http://apache.org/maven/project/packaging";

    public static String name = "http://apache.org/maven/project/name";

    public static String version = "http://apache.org/maven/project/version";

    public static String description = "http://apache.org/maven/project/description";

    public static String url = "http://apache.org/maven/project/url";

    public static class Prerequisites
    {
        public static String xUri = "http://apache.org/maven/project/prerequisites";

        public static String maven = "http://apache.org/maven/project/prerequisites/maven";
    }

    public static class IssueManagement
    {
        public static String xUri = "http://apache.org/maven/project/issueManagement";

        public static String system = "http://apache.org/maven/project/issueManagement/system";

        public static String url = "http://apache.org/maven/project/issueManagement/url";
    }

    public static class CiManagement
    {
        public static String xUri = "http://apache.org/maven/project/ciManagement";

        public static String system = "http://apache.org/maven/project/ciManagement/system";

        public static String url = "http://apache.org/maven/project/ciManagement/url";

        public static class Notifiers
        {
            public static String xUri = "http://apache.org/maven/project/ciManagement/notifiers#collection";

            public static class Notifier
            {
                public static String xUri =
                    "http://apache.org/maven/project/ciManagement/notifiers#collection/notifier";

                public static String type =
                    "http://apache.org/maven/project/ciManagement/notifiers#collection/notifier/type";

                public static String sendOnError =
                    "http://apache.org/maven/project/ciManagement/notifiers#collection/notifier/sendOnError";

                public static String sendOnFailure =
                    "http://apache.org/maven/project/ciManagement/notifiers#collection/notifier/sendOnFailure";

                public static String sendOnSuccess =
                    "http://apache.org/maven/project/ciManagement/notifiers#collection/notifier/sendOnSuccess";

                public static String sendOnWarning =
                    "http://apache.org/maven/project/ciManagement/notifiers#collection/notifier/sendOnWarning";

                public static String address =
                    "http://apache.org/maven/project/ciManagement/notifiers#collection/notifier/address";

                public static String configuration =
                    "http://apache.org/maven/project/ciManagement/notifiers#collection/notifier/configuration";
            }
        }
    }

    public static String inceptionYear = "http://apache.org/maven/project/inceptionYear";

    public static class MailingLists
    {
        public static String xUri = "http://apache.org/maven/project/mailingLists#collection";

        public static class MailingList
        {
            public static String xUri = "http://apache.org/maven/project/mailingLists#collection/mailingList";

            public static String name = "http://apache.org/maven/project/mailingLists#collection/mailingList/name";

            public static String subscribe =
                "http://apache.org/maven/project/mailingLists#collection/mailingList/subscribe";

            public static String unsubscribe =
                "http://apache.org/maven/project/mailingLists#collection/mailingList/unsubscribe";

            public static String post = "http://apache.org/maven/project/mailingLists#collection/mailingList/post";

            public static String archive =
                "http://apache.org/maven/project/mailingLists#collection/mailingList/archive";

            public static String otherArchives =
                "http://apache.org/maven/project/mailingLists#collection/mailingList/otherArchives";
        }
    }

    public static class Developers
    {
        public static String xUri = "http://apache.org/maven/project/developers#collection";

        public static class Developer
        {
            public static String xUri = "http://apache.org/maven/project/developers#collection/developer";

            public static String id = "http://apache.org/maven/project/developers#collection/developer/id";

            public static String name = "http://apache.org/maven/project/developers#collection/developer/name";

            public static String email = "http://apache.org/maven/project/developers#collection/developer/email";

            public static String url = "http://apache.org/maven/project/developers#collection/developer/url";

            public static String organization =
                "http://apache.org/maven/project/developers#collection/developer/organization";

            public static String organizationUrl =
                "http://apache.org/maven/project/developers#collection/developer/organizationUrl";

            public static String roles =
                "http://apache.org/maven/project/developers#collection/developer/roles#collection";

            public static String timezone = "http://apache.org/maven/project/developers#collection/developer/timezone";

            public static String properties =
                "http://apache.org/maven/project/developers#collection/developer/properties";
        }
    }

    public static class Contributors
    {
        public static String xUri = "http://apache.org/maven/project/contributors#collection";

        public static class Contributor
        {
            public static String xUri = "http://apache.org/maven/project/contributors#collection/contributor";

            public static String name = "http://apache.org/maven/project/contributors#collection/contributor/name";

            public static String email = "http://apache.org/maven/project/contributors#collection/contributor/email";

            public static String url = "http://apache.org/maven/project/contributors#collection/contributor/url";

            public static String organization =
                "http://apache.org/maven/project/contributors#collection/contributor/organization";

            public static String organizationUrl =
                "http://apache.org/maven/project/contributors#collection/contributor/organizationUrl";

            public static String roles =
                "http://apache.org/maven/project/contributors#collection/contributor/roles#collection";

            public static String timezone =
                "http://apache.org/maven/project/contributors#collection/contributor/timezone";

            public static String properties =
                "http://apache.org/maven/project/contributors#collection/contributor/properties";
        }
    }

    public static class Licenses
    {
        public static String xUri = "http://apache.org/maven/project/licenses#collection";

        public static class License
        {
            public static String xUri = "http://apache.org/maven/project/licenses#collection/license";

            public static String name = "http://apache.org/maven/project/licenses#collection/license/name";

            public static String url = "http://apache.org/maven/project/licenses#collection/license/url";

            public static String distribution =
                "http://apache.org/maven/project/licenses#collection/license/distribution";

            public static String comments = "http://apache.org/maven/project/licenses#collection/license/comments";
        }
    }

    public static class Scm
    {
        public static String xUri = "http://apache.org/maven/project/scm";

        public static String connection = "http://apache.org/maven/project/scm/connection";

        public static String developerConnection = "http://apache.org/maven/project/scm/developerConnection";

        public static String tag = "http://apache.org/maven/project/scm/tag";

        public static String url = "http://apache.org/maven/project/scm/url";
    }

    public static class Organization
    {
        public static String xUri = "http://apache.org/maven/project/organization";

        public static String name = "http://apache.org/maven/project/organization/name";

        public static String url = "http://apache.org/maven/project/organization/url";
    }

    public static class Build
    {
        public static String xUri = "http://apache.org/maven/project/build";

        public static String sourceDirectory = "http://apache.org/maven/project/build/sourceDirectory";

        public static String scriptSourceDirectory = "http://apache.org/maven/project/build/scriptSourceDirectory";

        public static String testSourceDirectory = "http://apache.org/maven/project/build/testSourceDirectory";

        public static String outputDirectory = "http://apache.org/maven/project/build/outputDirectory";

        public static String testOutputDirectory = "http://apache.org/maven/project/build/testOutputDirectory";

        public static class Extensions
        {
            public static String xUri = "http://apache.org/maven/project/build/extensions#collection";

            public static class Extension
            {
                public static String xUri = "http://apache.org/maven/project/build/extensions#collection/extension";

                public static String groupId =
                    "http://apache.org/maven/project/build/extensions#collection/extension/groupId";

                public static String artifactId =
                    "http://apache.org/maven/project/build/extensions#collection/extension/artifactId";

                public static String version =
                    "http://apache.org/maven/project/build/extensions#collection/extension/version";
            }
        }

        public static String defaultGoal = "http://apache.org/maven/project/build/defaultGoal";

        public static class Resources
        {
            public static String xUri = "http://apache.org/maven/project/build/resources#collection";

            public static class Resource
            {
                public static String xUri = "http://apache.org/maven/project/build/resources#collection/resource";

                public static String targetPath =
                    "http://apache.org/maven/project/build/resources#collection/resource/targetPath";

                public static String filtering =
                    "http://apache.org/maven/project/build/resources#collection/resource/filtering";

                public static String directory =
                    "http://apache.org/maven/project/build/resources#collection/resource/directory";

                public static String includes =
                    "http://apache.org/maven/project/build/resources#collection/resource/includes#collection";

                public static String excludes =
                    "http://apache.org/maven/project/build/resources#collection/resource/excludes#collection";
            }
        }

        public static class TestResources
        {
            public static String xUri = "http://apache.org/maven/project/build/testResources#collection";

            public static class TestResource
            {
                public static String xUri =
                    "http://apache.org/maven/project/build/testResources#collection/testResource";

                public static String targetPath =
                    "http://apache.org/maven/project/build/testResources#collection/testResource/targetPath";

                public static String filtering =
                    "http://apache.org/maven/project/build/testResources#collection/testResource/filtering";

                public static String directory =
                    "http://apache.org/maven/project/build/testResources#collection/testResource/directory";

                public static String excludes =
                    "http://apache.org/maven/project/build/testResources#collection/testResource/excludes";

                public static class Includes
                {
                    public static String xUri =
                        "http://apache.org/maven/project/build/testResources#collection/testResource/includes";

                    public static String include =
                        "http://apache.org/maven/project/build/testResources#collection/testResource/includes/include";
                }
            }
        }

        public static String directory = "http://apache.org/maven/project/build/directory";

        public static String finalName = "http://apache.org/maven/project/build/finalName";

        public static String filters = "http://apache.org/maven/project/build/filters";

        public static class PluginManagement
        {
            public static String xUri = "http://apache.org/maven/project/build/pluginManagement";

            public static class Plugins
            {
                public static String xUri = "http://apache.org/maven/project/build/pluginManagement/plugins#collection";

                public static class Plugin
                {
                    public static String xUri =
                        "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin";

                    public static String groupId =
                        "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/groupId";

                    public static String artifactId =
                        "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/artifactId";

                    public static String version =
                        "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/version";

                    public static String extensions =
                        "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/extensions";

                    public static class Executions
                    {
                        public static String xUri =
                            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/executions";

                        public static class Execution
                        {
                            public static String xUri =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/executions/execution";

                            public static String id =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/executions/execution/id";

                            public static String phase =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/executions/execution/phase";

                            public static String goals =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/executions/execution/goals";

                            public static String inherited =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/executions/execution/inherited";

                            public static String configuration =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/executions/execution/configuration";
                        }
                    }

                    public static class Dependencies
                    {
                        public static String xUri =
                            "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies";

                        public static class Dependency
                        {
                            public static String xUri =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies/dependency";

                            public static String groupId =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies/dependency/groupId";

                            public static String artifactId =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies/dependency/artifactId";

                            public static String version =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies/dependency/version";

                            public static String type =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies/dependency/type";

                            public static String classifier =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies/dependency/classifier";

                            public static String scope =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies/dependency/scope";

                            public static String systemPath =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies/dependency/systemPath";

                            public static class Exclusions
                            {
                                public static String xUri =
                                    "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies/dependency/exclusions";

                                public static class Exclusion
                                {
                                    public static String xUri =
                                        "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies/dependency/exclusions/exclusion";

                                    public static String artifactId =
                                        "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies/dependency/exclusions/exclusion/artifactId";

                                    public static String groupId =
                                        "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies/dependency/exclusions/exclusion/groupId";
                                }
                            }

                            public static String optional =
                                "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/dependencies/dependency/optional";
                        }
                    }

                    public static String goals =
                        "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/goals";

                    public static String inherited =
                        "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/inherited";

                    public static String configuration =
                        "http://apache.org/maven/project/build/pluginManagement/plugins#collection/plugin/configuration#collection";
                }
            }
        }

        public static class Plugins
        {
            public static String xUri = "http://apache.org/maven/project/build/plugins#collection";

            public static class Plugin
            {
                public static String xUri = "http://apache.org/maven/project/build/plugins#collection/plugin";

                public static String groupId =
                    "http://apache.org/maven/project/build/plugins#collection/plugin/groupId";

                public static String artifactId =
                    "http://apache.org/maven/project/build/plugins#collection/plugin/artifactId";

                public static String version =
                    "http://apache.org/maven/project/build/plugins#collection/plugin/version";

                public static String extensions =
                    "http://apache.org/maven/project/build/plugins#collection/plugin/extensions";

                public static class Executions
                {
                    public static String xUri =
                        "http://apache.org/maven/project/build/plugins#collection/plugin/executions#collection";

                    public static class Execution
                    {
                        public static String xUri =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/executions#collection/execution";

                        public static String id =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/executions#collection/execution/id";

                        public static String phase =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/executions#collection/execution/phase";

                        public static String goals =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/executions#collection/execution/goals";

                        public static String inherited =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/executions#collection/execution/inherited";

                        public static String configuration =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/executions#collection/execution/configuration";
                    }
                }

                public static class Dependencies
                {
                    public static String xUri =
                        "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection";

                    public static class Dependency
                    {
                        public static String xUri =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency";

                        public static String groupId =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency/groupId";

                        public static String artifactId =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency/artifactId";

                        public static String version =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency/version";

                        public static String type =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency/type";

                        public static String classifier =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency/classifier";

                        public static String scope =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency/scope";

                        public static String systemPath =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency/systemPath";

                        public static class Exclusions
                        {
                            public static String xUri =
                                "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency/exclusions";

                            public static class Exclusion
                            {
                                public static String xUri =
                                    "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency/exclusions/exclusion";

                                public static String artifactId =
                                    "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency/exclusions/exclusion/artifactId";

                                public static String groupId =
                                    "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency/exclusions/exclusion/groupId";
                            }
                        }

                        public static String optional =
                            "http://apache.org/maven/project/build/plugins#collection/plugin/dependencies#collection/dependency/optional";
                    }
                }

                public static String goals = "http://apache.org/maven/project/build/plugins#collection/plugin/goals";

                public static String inherited =
                    "http://apache.org/maven/project/build/plugins#collection/plugin/inherited";

                public static String configuration =
                    "http://apache.org/maven/project/build/plugins#collection/plugin/configuration#collection";
            }
        }
    }

    public static class Profiles
    {
        public static String xUri = "http://apache.org/maven/project/profiles#collection";

        public static class Profile
        {
            public static String xUri = "http://apache.org/maven/project/profiles#collection/profile";

            public static String id = "http://apache.org/maven/project/profiles#collection/profile/id";

            public static class Activation
            {
                public static String xUri = "http://apache.org/maven/project/profiles#collection/profile/activation";

                public static String activeByDefault =
                    "http://apache.org/maven/project/profiles#collection/profile/activation/activeByDefault";

                public static String jdk = "http://apache.org/maven/project/profiles#collection/profile/activation/jdk";

                public static class Os
                {
                    public static String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/activation/os";

                    public static String name =
                        "http://apache.org/maven/project/profiles#collection/profile/activation/os/name";

                    public static String family =
                        "http://apache.org/maven/project/profiles#collection/profile/activation/os/family";

                    public static String arch =
                        "http://apache.org/maven/project/profiles#collection/profile/activation/os/arch";

                    public static String version =
                        "http://apache.org/maven/project/profiles#collection/profile/activation/os/version";
                }

                public static class Property
                {
                    public static String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/activation/property";

                    public static String name =
                        "http://apache.org/maven/project/profiles#collection/profile/activation/property/name";

                    public static String value =
                        "http://apache.org/maven/project/profiles#collection/profile/activation/property/value";
                }

                public static class File
                {
                    public static String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/activation/file";

                    public static String missing =
                        "http://apache.org/maven/project/profiles#collection/profile/activation/file/missing";

                    public static String exists =
                        "http://apache.org/maven/project/profiles#collection/profile/activation/file/exists";
                }
            }

            public static class Build
            {
                public static String xUri = "http://apache.org/maven/project/profiles#collection/profile/build";

                public static String defaultGoal =
                    "http://apache.org/maven/project/profiles#collection/profile/build/defaultGoal";

                public static class Resources
                {
                    public static String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/build/resources#collection";

                    public static class Resource
                    {
                        public static String xUri =
                            "http://apache.org/maven/project/profiles#collection/profile/build/resources#collection/resource";

                        public static String targetPath =
                            "http://apache.org/maven/project/profiles#collection/profile/build/resources#collection/resource/targetPath";

                        public static String filtering =
                            "http://apache.org/maven/project/profiles#collection/profile/build/resources#collection/resource/filtering";

                        public static String directory =
                            "http://apache.org/maven/project/profiles#collection/profile/build/resources#collection/resource/directory";

                        public static String includes =
                            "http://apache.org/maven/project/profiles#collection/profile/build/resources#collection/resource/includes#collection";

                        public static String excludes =
                            "http://apache.org/maven/project/profiles#collection/profile/build/resources#collection/resource/excludes#collection";
                    }
                }

                public static class TestResources
                {
                    public static String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/build/testResources#collection";

                    public static class TestResource
                    {
                        public static String xUri =
                            "http://apache.org/maven/project/profiles#collection/profile/build/testResources#collection/testResource";

                        public static String targetPath =
                            "http://apache.org/maven/project/profiles#collection/profile/build/testResources#collection/testResource/targetPath";

                        public static String filtering =
                            "http://apache.org/maven/project/profiles#collection/profile/build/testResources#collection/testResource/filtering";

                        public static String directory =
                            "http://apache.org/maven/project/profiles#collection/profile/build/testResources#collection/testResource/directory";

                        public static String includes =
                            "http://apache.org/maven/project/profiles#collection/profile/build/testResources#collection/testResource/includes#collection";

                        public static String excludes =
                            "http://apache.org/maven/project/profiles#collection/profile/build/testResources#collection/testResource/excludes#collection";

                    }
                }

                public static String directory =
                    "http://apache.org/maven/project/profiles#collection/profile/build/directory";

                public static String finalName =
                    "http://apache.org/maven/project/profiles#collection/profile/build/finalName";

                public static String filters =
                    "http://apache.org/maven/project/profiles#collection/profile/build/filters";

                public static class PluginManagement
                {
                    public static String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement";

                    public static class Plugins
                    {
                        public static String xUri =
                            "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins";

                        public static class Plugin
                        {
                            public static String xUri =
                                "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin";

                            public static String groupId =
                                "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/groupId";

                            public static String artifactId =
                                "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/artifactId";

                            public static String version =
                                "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/version";

                            public static String extensions =
                                "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/extensions";

                            public static class Executions
                            {
                                public static String xUri =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/executions";

                                public static class Execution
                                {
                                    public static String xUri =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/executions/execution";

                                    public static String id =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/executions/execution/id";

                                    public static String phase =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/executions/execution/phase";

                                    public static String goals =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/executions/execution/goals";

                                    public static String inherited =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/executions/execution/inherited";

                                    public static String configuration =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/executions/execution/configuration";
                                }
                            }

                            public static class Dependencies
                            {
                                public static String xUri =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/dependencies";

                                public static class Dependency
                                {
                                    public static String xUri =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/dependencies/dependency";

                                    public static String groupId =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/dependencies/dependency/groupId";

                                    public static String artifactId =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/dependencies/dependency/artifactId";

                                    public static String version =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/dependencies/dependency/version";

                                    public static String type =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/dependencies/dependency/type";

                                    public static String classifier =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/dependencies/dependency/classifier";

                                    public static String scope =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/dependencies/dependency/scope";

                                    public static String systemPath =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/dependencies/dependency/systemPath";

                                    public static class Exclusions
                                    {
                                        public static String xUri =
                                            "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/dependencies/dependency/exclusions";

                                        public static class Exclusion
                                        {
                                            public static String xUri =
                                                "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/dependencies/dependency/exclusions/exclusion";

                                            public static String artifactId =
                                                "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/dependencies/dependency/exclusions/exclusion/artifactId";

                                            public static String groupId =
                                                "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/dependencies/dependency/exclusions/exclusion/groupId";
                                        }
                                    }

                                    public static String optional =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/dependencies/dependency/optional";
                                }
                            }

                            public static String goals =
                                "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/goals";

                            public static String inherited =
                                "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/inherited";

                            public static String configuration =
                                "http://apache.org/maven/project/profiles#collection/profile/build/pluginManagement/plugins/plugin/configuration";
                        }
                    }
                }

                public static class Plugins
                {
                    public static String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/build/plugins";

                    public static class Plugin
                    {
                        public static String xUri =
                            "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin";

                        public static String groupId =
                            "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/groupId";

                        public static String artifactId =
                            "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/artifactId";

                        public static String version =
                            "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/version";

                        public static String extensions =
                            "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/extensions";

                        public static class Executions
                        {
                            public static String xUri =
                                "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/executions";

                            public static class Execution
                            {
                                public static String xUri =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/executions/execution";

                                public static String id =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/executions/execution/id";

                                public static String phase =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/executions/execution/phase";

                                public static String goals =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/executions/execution/goals";

                                public static String inherited =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/executions/execution/inherited";

                                public static String configuration =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/executions/execution/configuration";
                            }
                        }

                        public static class Dependencies
                        {
                            public static String xUri =
                                "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/dependencies";

                            public static class Dependency
                            {
                                public static String xUri =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/dependencies/dependency";

                                public static String groupId =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/dependencies/dependency/groupId";

                                public static String artifactId =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/dependencies/dependency/artifactId";

                                public static String version =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/dependencies/dependency/version";

                                public static String type =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/dependencies/dependency/type";

                                public static String classifier =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/dependencies/dependency/classifier";

                                public static String scope =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/dependencies/dependency/scope";

                                public static String systemPath =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/dependencies/dependency/systemPath";

                                public static class Exclusions
                                {
                                    public static String xUri =
                                        "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/dependencies/dependency/exclusions";

                                    public static class Exclusion
                                    {
                                        public static String xUri =
                                            "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/dependencies/dependency/exclusions/exclusion";

                                        public static String artifactId =
                                            "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/dependencies/dependency/exclusions/exclusion/artifactId";

                                        public static String groupId =
                                            "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/dependencies/dependency/exclusions/exclusion/groupId";
                                    }
                                }

                                public static String optional =
                                    "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/dependencies/dependency/optional";
                            }
                        }

                        public static String goals =
                            "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/goals";

                        public static String inherited =
                            "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/inherited";

                        public static String configuration =
                            "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/configuration";
                    }
                }
            }

            public static String modules = "http://apache.org/maven/project/profiles#collection/profile/modules";

            public static class Repositories
            {
                public static String xUri = "http://apache.org/maven/project/profiles#collection/profile/repositories";

                public static class Repository
                {
                    public static String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/repositories/repository";

                    public static class Releases
                    {
                        public static String xUri =
                            "http://apache.org/maven/project/profiles#collection/profile/repositories/repository/releases";

                        public static String enabled =
                            "http://apache.org/maven/project/profiles#collection/profile/repositories/repository/releases/enabled";

                        public static String updatePolicy =
                            "http://apache.org/maven/project/profiles#collection/profile/repositories/repository/releases/updatePolicy";

                        public static String checksumPolicy =
                            "http://apache.org/maven/project/profiles#collection/profile/repositories/repository/releases/checksumPolicy";
                    }

                    public static class Snapshots
                    {
                        public static String xUri =
                            "http://apache.org/maven/project/profiles#collection/profile/repositories/repository/snapshots";

                        public static String enabled =
                            "http://apache.org/maven/project/profiles#collection/profile/repositories/repository/snapshots/enabled";

                        public static String updatePolicy =
                            "http://apache.org/maven/project/profiles#collection/profile/repositories/repository/snapshots/updatePolicy";

                        public static String checksumPolicy =
                            "http://apache.org/maven/project/profiles#collection/profile/repositories/repository/snapshots/checksumPolicy";
                    }

                    public static String id =
                        "http://apache.org/maven/project/profiles#collection/profile/repositories/repository/id";

                    public static String name =
                        "http://apache.org/maven/project/profiles#collection/profile/repositories/repository/name";

                    public static String url =
                        "http://apache.org/maven/project/profiles#collection/profile/repositories/repository/url";

                    public static String layout =
                        "http://apache.org/maven/project/profiles#collection/profile/repositories/repository/layout";
                }
            }

            public static class PluginRepositories
            {
                public static String xUri =
                    "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories";

                public static class PluginRepository
                {
                    public static String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories/pluginRepository";

                    public static class Releases
                    {
                        public static String xUri =
                            "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories/pluginRepository/releases";

                        public static String enabled =
                            "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories/pluginRepository/releases/enabled";

                        public static String updatePolicy =
                            "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories/pluginRepository/releases/updatePolicy";

                        public static String checksumPolicy =
                            "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories/pluginRepository/releases/checksumPolicy";
                    }

                    public static class Snapshots
                    {
                        public static String xUri =
                            "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories/pluginRepository/snapshots";

                        public static String enabled =
                            "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories/pluginRepository/snapshots/enabled";

                        public static String updatePolicy =
                            "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories/pluginRepository/snapshots/updatePolicy";

                        public static String checksumPolicy =
                            "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories/pluginRepository/snapshots/checksumPolicy";
                    }

                    public static String id =
                        "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories/pluginRepository/id";

                    public static String name =
                        "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories/pluginRepository/name";

                    public static String url =
                        "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories/pluginRepository/url";

                    public static String layout =
                        "http://apache.org/maven/project/profiles#collection/profile/pluginRepositories/pluginRepository/layout";
                }
            }

            public static class Dependencies
            {
                public static String xUri = "http://apache.org/maven/project/profiles#collection/profile/dependencies";

                public static class Dependency
                {
                    public static String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/dependencies/dependency";

                    public static String groupId =
                        "http://apache.org/maven/project/profiles#collection/profile/dependencies/dependency/groupId";

                    public static String artifactId =
                        "http://apache.org/maven/project/profiles#collection/profile/dependencies/dependency/artifactId";

                    public static String version =
                        "http://apache.org/maven/project/profiles#collection/profile/dependencies/dependency/version";

                    public static String type =
                        "http://apache.org/maven/project/profiles#collection/profile/dependencies/dependency/type";

                    public static String classifier =
                        "http://apache.org/maven/project/profiles#collection/profile/dependencies/dependency/classifier";

                    public static String scope =
                        "http://apache.org/maven/project/profiles#collection/profile/dependencies/dependency/scope";

                    public static String systemPath =
                        "http://apache.org/maven/project/profiles#collection/profile/dependencies/dependency/systemPath";

                    public static class Exclusions
                    {
                        public static String xUri =
                            "http://apache.org/maven/project/profiles#collection/profile/dependencies/dependency/exclusions";

                        public static class Exclusion
                        {
                            public static String xUri =
                                "http://apache.org/maven/project/profiles#collection/profile/dependencies/dependency/exclusions/exclusion";

                            public static String artifactId =
                                "http://apache.org/maven/project/profiles#collection/profile/dependencies/dependency/exclusions/exclusion/artifactId";

                            public static String groupId =
                                "http://apache.org/maven/project/profiles#collection/profile/dependencies/dependency/exclusions/exclusion/groupId";
                        }
                    }

                    public static String optional =
                        "http://apache.org/maven/project/profiles#collection/profile/dependencies/dependency/optional";
                }
            }

            public static String reports = "http://apache.org/maven/project/profiles#collection/profile/reports";

            public static class Reporting
            {
                public static String xUri = "http://apache.org/maven/project/profiles#collection/profile/reporting";

                public static String excludeDefaults =
                    "http://apache.org/maven/project/profiles#collection/profile/reporting/excludeDefaults";

                public static String outputDirectory =
                    "http://apache.org/maven/project/profiles#collection/profile/reporting/outputDirectory";

                public static class Plugins
                {
                    public static String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins";

                    public static class Plugin
                    {
                        public static String xUri =
                            "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins/plugin";

                        public static String groupId =
                            "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins/plugin/groupId";

                        public static String artifactId =
                            "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins/plugin/artifactId";

                        public static String version =
                            "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins/plugin/version";

                        public static String inherited =
                            "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins/plugin/inherited";

                        public static String configuration =
                            "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins/plugin/configuration";

                        public static class ReportSets
                        {
                            public static String xUri =
                                "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins/plugin/reportSets";

                            public static class ReportSet
                            {
                                public static String xUri =
                                    "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins/plugin/reportSets/reportSet";

                                public static String id =
                                    "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins/plugin/reportSets/reportSet/id";

                                public static String configuration =
                                    "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins/plugin/reportSets/reportSet/configuration";

                                public static String inherited =
                                    "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins/plugin/reportSets/reportSet/inherited";

                                public static String reports =
                                    "http://apache.org/maven/project/profiles#collection/profile/reporting/plugins/plugin/reportSets/reportSet/reports";
                            }
                        }
                    }
                }
            }

            public static class DependencyManagement
            {
                public static String xUri =
                    "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement";

                public static class Dependencies
                {
                    public static String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection";

                    public static class Dependency
                    {
                        public static String xUri =
                            "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency";

                        public static String groupId =
                            "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency/groupId";

                        public static String artifactId =
                            "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency/artifactId";

                        public static String version =
                            "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency/version";

                        public static String type =
                            "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency/type";

                        public static String classifier =
                            "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency/classifier";

                        public static String scope =
                            "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency/scope";

                        public static String systemPath =
                            "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency/systemPath";

                        public static class Exclusions
                        {
                            public static String xUri =
                                "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency/exclusions";

                            public static class Exclusion
                            {
                                public static String xUri =
                                    "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency/exclusions/exclusion";

                                public static String artifactId =
                                    "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency/exclusions/exclusion/artifactId";

                                public static String groupId =
                                    "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency/exclusions/exclusion/groupId";
                            }
                        }

                        public static String optional =
                            "http://apache.org/maven/project/profiles#collection/profile/dependencyManagement/dependencies#collection/dependency/optional";
                    }
                }
            }

            public static class DistributionManagement
            {
                public static String xUri =
                    "http://apache.org/maven/project/profiles#collection/profile/distributionManagement";

                public static class Repository
                {
                    public static String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/repository";

                    public static String uniqueVersion =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/repository/uniqueVersion";

                    public static String id =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/repository/id";

                    public static String name =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/repository/name";

                    public static String url =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/repository/url";

                    public static String layout =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/repository/layout";
                }

                public static class SnapshotRepository
                {
                    public static String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/snapshotRepository";

                    public static String uniqueVersion =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/snapshotRepository/uniqueVersion";

                    public static String id =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/snapshotRepository/id";

                    public static String name =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/snapshotRepository/name";

                    public static String url =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/snapshotRepository/url";

                    public static String layout =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/snapshotRepository/layout";
                }

                public static class Site
                {
                    public static String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/site";

                    public static String id =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/site/id";

                    public static String name =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/site/name";

                    public static String url =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/site/url";
                }

                public static String downloadUrl =
                    "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/downloadUrl";

                public static class Relocation
                {
                    public static String xUri =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/relocation";

                    public static String groupId =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/relocation/groupId";

                    public static String artifactId =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/relocation/artifactId";

                    public static String version =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/relocation/version";

                    public static String message =
                        "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/relocation/message";
                }

                public static String status =
                    "http://apache.org/maven/project/profiles#collection/profile/distributionManagement/status";
            }

            public static String properties = "http://apache.org/maven/project/profiles#collection/profile/properties";
        }
    }

    public static class Modules
    {
        public static String xUri = "http://apache.org/maven/project/modules#collection";

        public static String module = "http://apache.org/maven/project/modules#collection/module";
    }

    public static class Repositories
    {
        public static String xUri = "http://apache.org/maven/project/repositories#collection";

        public static class Repository
        {
            public static String xUri = "http://apache.org/maven/project/repositories#collection/repository";

            public static class Releases
            {
                public static String xUri =
                    "http://apache.org/maven/project/repositories#collection/repository/releases";

                public static String enabled =
                    "http://apache.org/maven/project/repositories#collection/repository/releases/enabled";

                public static String updatePolicy =
                    "http://apache.org/maven/project/repositories#collection/repository/releases/updatePolicy";

                public static String checksumPolicy =
                    "http://apache.org/maven/project/repositories#collection/repository/releases/checksumPolicy";
            }

            public static class Snapshots
            {
                public static String xUri =
                    "http://apache.org/maven/project/repositories#collection/repository/snapshots";

                public static String enabled =
                    "http://apache.org/maven/project/repositories#collection/repository/snapshots/enabled";

                public static String updatePolicy =
                    "http://apache.org/maven/project/repositories#collection/repository/snapshots/updatePolicy";

                public static String checksumPolicy =
                    "http://apache.org/maven/project/repositories#collection/repository/snapshots/checksumPolicy";
            }

            public static String id = "http://apache.org/maven/project/repositories#collection/repository/id";

            public static String name = "http://apache.org/maven/project/repositories#collection/repository/name";

            public static String url = "http://apache.org/maven/project/repositories#collection/repository/url";

            public static String layout = "http://apache.org/maven/project/repositories#collection/repository/layout";
        }
    }

    public static class PluginRepositories
    {
        public static String xUri = "http://apache.org/maven/project/pluginRepositories#collection";

        public static class PluginRepository
        {
            public static String xUri =
                "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository";

            public static class Releases
            {
                public static String xUri =
                    "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository/releases";

                public static String enabled =
                    "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository/releases/enabled";

                public static String updatePolicy =
                    "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository/releases/updatePolicy";

                public static String checksumPolicy =
                    "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository/releases/checksumPolicy";
            }

            public static class Snapshots
            {
                public static String xUri =
                    "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository/snapshots";

                public static String enabled =
                    "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository/snapshots/enabled";

                public static String updatePolicy =
                    "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository/snapshots/updatePolicy";

                public static String checksumPolicy =
                    "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository/snapshots/checksumPolicy";
            }

            public static String id =
                "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository/id";

            public static String name =
                "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository/name";

            public static String url =
                "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository/url";

            public static String layout =
                "http://apache.org/maven/project/pluginRepositories#collection/pluginRepository/layout";
        }
    }

    public static class Dependencies
    {
        public static String xUri = "http://apache.org/maven/project/dependencies#collection";

        public static class Dependency
        {
            public static String xUri = "http://apache.org/maven/project/dependencies#collection/dependency";

            public static String groupId = "http://apache.org/maven/project/dependencies#collection/dependency/groupId";

            public static String artifactId =
                "http://apache.org/maven/project/dependencies#collection/dependency/artifactId";

            public static String version = "http://apache.org/maven/project/dependencies#collection/dependency/version";

            public static String type = "http://apache.org/maven/project/dependencies#collection/dependency/type";

            public static String classifier =
                "http://apache.org/maven/project/dependencies#collection/dependency/classifier";

            public static String scope = "http://apache.org/maven/project/dependencies#collection/dependency/scope";

            public static String systemPath =
                "http://apache.org/maven/project/dependencies#collection/dependency/systemPath";

            public static class Exclusions
            {
                public static String xUri =
                    "http://apache.org/maven/project/dependencies#collection/dependency/exclusions";

                public static class Exclusion
                {
                    public static String xUri =
                        "http://apache.org/maven/project/dependencies#collection/dependency/exclusions/exclusion";

                    public static String artifactId =
                        "http://apache.org/maven/project/dependencies#collection/dependency/exclusions/exclusion/artifactId";

                    public static String groupId =
                        "http://apache.org/maven/project/dependencies#collection/dependency/exclusions/exclusion/groupId";
                }
            }

            public static String optional =
                "http://apache.org/maven/project/dependencies#collection/dependency/optional";
        }
    }

    public static String reports = "http://apache.org/maven/project/reports";

    public static class Reporting
    {
        public static String xUri = "http://apache.org/maven/project/reporting";

        public static String excludeDefaults = "http://apache.org/maven/project/reporting/excludeDefaults";

        public static String outputDirectory = "http://apache.org/maven/project/reporting/outputDirectory";

        public static class Plugins
        {
            public static String xUri = "http://apache.org/maven/project/reporting/plugins#collection";

            public static class Plugin
            {
                public static String xUri = "http://apache.org/maven/project/reporting/plugins#collection/plugin";

                public static String groupId =
                    "http://apache.org/maven/project/reporting/plugins#collection/plugin/groupId";

                public static String artifactId =
                    "http://apache.org/maven/project/reporting/plugins#collection/plugin/artifactId";

                public static String version =
                    "http://apache.org/maven/project/reporting/plugins#collection/plugin/version";

                public static String inherited =
                    "http://apache.org/maven/project/reporting/plugins#collection/plugin/inherited";

                public static String configuration =
                    "http://apache.org/maven/project/reporting/plugins#collection/plugin/configuration";

                public static class ReportSets
                {
                    public static String xUri =
                        "http://apache.org/maven/project/reporting/plugins#collection/plugin/reportSets";

                    public static class ReportSet
                    {
                        public static String xUri =
                            "http://apache.org/maven/project/reporting/plugins#collection/plugin/reportSets/reportSet";

                        public static String id =
                            "http://apache.org/maven/project/reporting/plugins#collection/plugin/reportSets/reportSet/id";

                        public static String configuration =
                            "http://apache.org/maven/project/reporting/plugins#collection/plugin/reportSets/reportSet/configuration";

                        public static String inherited =
                            "http://apache.org/maven/project/reporting/plugins#collection/plugin/reportSets/reportSet/inherited";

                        public static String reports =
                            "http://apache.org/maven/project/reporting/plugins#collection/plugin/reportSets/reportSet/reports";
                    }
                }
            }
        }
    }

    public static class DependencyManagement
    {
        public static String xUri = "http://apache.org/maven/project/dependencyManagement";

        public static class Dependencies
        {
            public static String xUri = "http://apache.org/maven/project/dependencyManagement/dependencies#collection";

            public static class Dependency
            {
                public static String xUri =
                    "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency";

                public static String groupId =
                    "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency/groupId";

                public static String artifactId =
                    "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency/artifactId";

                public static String version =
                    "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency/version";

                public static String type =
                    "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency/type";

                public static String classifier =
                    "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency/classifier";

                public static String scope =
                    "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency/scope";

                public static String systemPath =
                    "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency/systemPath";

                public static class Exclusions
                {
                    public static String xUri =
                        "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency/exclusions";

                    public static class Exclusion
                    {
                        public static String xUri =
                            "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency/exclusions/exclusion";

                        public static String artifactId =
                            "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency/exclusions/exclusion/artifactId";

                        public static String groupId =
                            "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency/exclusions/exclusion/groupId";
                    }
                }

                public static String optional =
                    "http://apache.org/maven/project/dependencyManagement/dependencies#collection/dependency/optional";
            }
        }
    }

    public static class DistributionManagement
    {
        public static String xUri = "http://apache.org/maven/project/distributionManagement";

        public static class Repository
        {
            public static String xUri = "http://apache.org/maven/project/distributionManagement/repository";

            public static String uniqueVersion =
                "http://apache.org/maven/project/distributionManagement/repository/uniqueVersion";

            public static String id = "http://apache.org/maven/project/distributionManagement/repository/id";

            public static String name = "http://apache.org/maven/project/distributionManagement/repository/name";

            public static String url = "http://apache.org/maven/project/distributionManagement/repository/url";

            public static String layout = "http://apache.org/maven/project/distributionManagement/repository/layout";
        }

        public static class SnapshotRepository
        {
            public static String xUri = "http://apache.org/maven/project/distributionManagement/snapshotRepository";

            public static String uniqueVersion =
                "http://apache.org/maven/project/distributionManagement/snapshotRepository/uniqueVersion";

            public static String id = "http://apache.org/maven/project/distributionManagement/snapshotRepository/id";

            public static String name =
                "http://apache.org/maven/project/distributionManagement/snapshotRepository/name";

            public static String url = "http://apache.org/maven/project/distributionManagement/snapshotRepository/url";

            public static String layout =
                "http://apache.org/maven/project/distributionManagement/snapshotRepository/layout";
        }

        public static class Site
        {
            public static String xUri = "http://apache.org/maven/project/distributionManagement/site";

            public static String id = "http://apache.org/maven/project/distributionManagement/site/id";

            public static String name = "http://apache.org/maven/project/distributionManagement/site/name";

            public static String url = "http://apache.org/maven/project/distributionManagement/site/url";
        }

        public static String downloadUrl = "http://apache.org/maven/project/distributionManagement/downloadUrl";

        public static class Relocation
        {
            public static String xUri = "http://apache.org/maven/project/distributionManagement/relocation";

            public static String groupId = "http://apache.org/maven/project/distributionManagement/relocation/groupId";

            public static String artifactId =
                "http://apache.org/maven/project/distributionManagement/relocation/artifactId";

            public static String version = "http://apache.org/maven/project/distributionManagement/relocation/version";

            public static String message = "http://apache.org/maven/project/distributionManagement/relocation/message";
        }

        public static String status = "http://apache.org/maven/project/distributionManagement/status";
    }

    public static String properties = "http://apache.org/maven/project/properties";

}
