~~ Licensed to the Apache Software Foundation (ASF) under one
~~ or more contributor license agreements.  See the NOTICE file
~~ distributed with this work for additional information
~~ regarding copyright ownership.  The ASF licenses this file
~~ to you under the Apache License, Version 2.0 (the
~~ "License"); you may not use this file except in compliance
~~ with the License.  You may obtain a copy of the License at
~~
~~ http://www.apache.org/licenses/LICENSE-2.0
~~
~~ Unless required by applicable law or agreed to in writing,
~~ software distributed under the License is distributed on an
~~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
~~ KIND, either express or implied.  See the License for the
~~ specific language governing permissions and limitations
~~ under the License.

 -----
 Introduction
 -----
 Hervé Boutemy
 -----
 2013-07-27
 -----

Maven Core

 Maven Core classes managing the whole build process.

* Reference Documentation

 * {{{./lifecycles.html}lifecycles}} and {{{./default-bindings.html}plugin bindings to <<<default>>> lifecycle}},

 * {{{./artifact-handlers.html}default artifact handlers}}, to manage {{{../maven-model/maven.html#class_dependency}dependency types}},

 * {{{./extension.html}extension descriptor}} and {{{./core-extensions.html}core extensions}},

 * {{{/guides/mini/guide-maven-classloading.html}classloader hierarchy}} done by <<<ClassRealmManager>>> component
 ({{{./apidocs/org/apache/maven/classrealm/ClassRealmManager.html}javadoc}}),
 with its <<<DefaultClassRealmManager>>> implementation
 ({{{./xref/org/apache/maven/classrealm/DefaultClassRealmManager.html}source}}), using
 {{{https://codehaus-plexus.github.io/plexus-classworlds/}Plexus Classworlds}},

* Useful entry points

 * <<<Maven>>> component ({{{./apidocs/org/apache/maven/Maven.html}javadoc}}),
 with its <<<DefaultMaven>>> implementation ({{{./xref/org/apache/maven/DefaultMaven.html}source}}), to drive
 a full <<<MavenSession>>> execution ({{{./apidocs/org/apache/maven/execution/MavenSession.html}javadoc}}

 * <<<ProjectBuilder>>> component ({{{./apidocs/org/apache/maven/project/ProjectBuilder.html}javadoc}}),
 with its <<<DefaultProjectBuilder>>> implementation
 ({{{./xref/org/apache/maven/project/DefaultProjectBuilder.html}source}}), to prepare {{{./apidocs/org/apache/maven/project/MavenProject.html}<<<MavenProject>>> descriptor}} from POM files,

 * <<<LifecycleExecutor>>> component ({{{./apidocs/org/apache/maven/lifecycle/LifecycleExecutor.html}javadoc}}),
 with its <<<DefaultLifecycleExecutor>>> implementation({{{/xref/org/apache/maven/lifecycle/DefaultLifecycleExecutor.html}source}}), to plan or execute tasks.\
   on plugin goals execution order:

   * <<in a given phase, goals order is not expected to be guaranteed nor finely tuned>>:
     it is just a consequence of the order obtained during {{{../maven-model-builder/}effective model building}},
     which combines profile activation+injection and inheritance assembly from parents,

   * known limitations are notably that:

     1. plugin goal execution in a child is usually simply appended (at end): you can't try to insert in the middle of pre-existing inherited executions,

     2. append happens at plugin level first, then goal level, independently from phases.
        This means for example that adding pluginA:goal2 to pre-existing (pluginA:goal1, pluginB:goal) will lead to (pluginA:goal1, pluginA:goal2, pluginB:goal)

   * see effective POM as shown by {{{/plugins/maven-help-plugin/effective-pom-mojo.html}<<<help:effective-pom>>>}} to see the effective plugins then goals order.

 * <<<MavenPluginManager>>> component ({{{./apidocs/org/apache/maven/plugin/MavenPluginManager.html}javadoc}}),
 with its <<<DefaultMavenPluginManager>>> implementation
 ({{{./xref/org/apache/maven/plugin/internal/DefaultMavenPluginManager.html}source}}),

 * {{{./apidocs/org/apache/maven/plugin/PluginParameterExpressionEvaluator.html}PluginParameterExpressionEvaluator}}, used to
 evaluate plugin parameters values during Mojo configuration,

 * <<<ExceptionHandler>>> component ({{{./apidocs/org/apache/maven/exception/ExceptionHandler.html}javadoc}}),
 with its <<<DefaultExceptionHandler>>> implementation
 ({{{./xref/org/apache/maven/exception/DefaultExceptionHandler.html}source}}), use to transform exception into useful end-user messages.

* Toolchains

 * {{{./toolchains.html}Toolchains descriptor reference}},

 * public API for toolchains-aware plugins: <<<ToolchainManager>>> component ({{{./apidocs/org/apache/maven/toolchain/ToolchainManager.html}javadoc}})
 with its <<<DefaultToolchainManager>>> implementation ({{{./xref/org/apache/maven/toolchain/DefaultToolchainManager.html}source}}),
 to get selected <<<Toolchain>>> ({{{./apidocs/org/apache/maven/toolchain/Toolchain.html}javadoc}}) instance,

 * internal <<<ToolchainManagerPrivate>>> component ({{{./apidocs/org/apache/maven/toolchain/ToolchainManagerPrivate.html}javadoc}})
 with its <<<DefaultToolchainManagerPrivate>>> implementation ({{{./xref/org/apache/maven/toolchain/DefaultToolchainManagerPrivate.html}source}}),
 to manage toolchain selection,

 * internal JDK toolchain implementation: <<<JavaToolchain>>> interface ({{{./apidocs/org/apache/maven/toolchain/java/JavaToolchain.html}javadoc}}),
 with its <<<JavaToolchainImpl>>> implementation
 ({{{./xref/org/apache/maven/toolchain/java/JavaToolchainImpl.html}source}}) and <<<JavaToolchainFactory>>>
 factory ({{{./xref/org/apache/maven/toolchain/java/JavaToolchainFactory.html}source}}).
