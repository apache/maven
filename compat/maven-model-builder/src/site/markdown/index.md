---
title: Introduction
author:
  - Hervé Boutemy
date: 2013-03-19
---

<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Maven Model Builder

The effective model builder, with profile activation, inheritance, interpolation, ...

The main component is `ModelBuilder` ([javadoc](./apidocs/org/apache/maven/model/building/ModelBuilder.html), [source](./xref/org/apache/maven/model/building/ModelBuilder.html)), with its `DefaultModelBuilder` implementation ([javadoc](./apidocs/org/apache/maven/model/building/DefaultModelBuilder.html), [source](./xref/org/apache/maven/model/building/DefaultModelBuilder.html)) that manages the steps sequence.

The sequence is divided into 2 phases:

- phase 1
    - profile activation: see [available activators](./apidocs/org/apache/maven/model/profile/activation/package-summary.html). Notice that model interpolation hasn't happened yet, then interpolation for file-based activation is limited to `${basedir}` (since Maven 3), `${rootDirectory}` (since Maven 4), system properties and user properties
    - file model validation: `ModelValidator` ([javadoc](./apidocs/org/apache/maven/model/validation/ModelValidator.html)), with its `DefaultModelValidator` implementation ([source](./xref/org/apache/maven/model/validation/DefaultModelValidator.html))
- phase 2, with optional plugin processing
    - Build up a raw model by re-reading the file and enriching it based on information available in the reactor. Some features:
        - Resolve version of versionless parents based on relativePath (including ci-friendly versions)
        - Resolve version of versionless dependencies that are part of the reactor
    - raw model validation: `ModelValidator` ([javadoc](./apidocs/org/apache/maven/model/validation/ModelValidator.html)), with its `DefaultModelValidator` implementation ([source](./xref/org/apache/maven/model/validation/DefaultModelValidator.html))
    - model normalization - merge duplicates: `ModelNormalizer` ([javadoc](./apidocs/org/apache/maven/model/normalization/ModelNormalizer.html)), with its `DefaultModelNormalizer` implementation ([source](./xref/org/apache/maven/model/normalization/DefaultModelNormalizer.html))
    - profile injection: `ProfileInjector` ([javadoc](./apidocs/org/apache/maven/model/profile/ProfileInjector.html)), with its `DefaultProfileInjector` implementation ([source](./xref/org/apache/maven/model/profile/DefaultProfileInjector.html))
    - parent resolution until [super-pom](./super-pom.html)
    - inheritance assembly (see [below](./index.html#Inheritance_Assembly))
    - model interpolation (see [below](./index.html#Model_Interpolation))
    - url normalization: `UrlNormalizer` ([javadoc](./apidocs/org/apache/maven/model/path/UrlNormalizer.html)), with its `DefaultUrlNormalizer` implementation ([source](./xref/org/apache/maven/model/path/DefaultUrlNormalizer.html))
    - model path translation: `ModelPathTranslator` ([javadoc](./apidocs/org/apache/maven/model/path/ModelPathTranslator.html)), with its `DefaultModelPathTranslator` implementation ([source](./xref/org/apache/maven/model/path/DefaultModelPathTranslator.html))
    - plugin management injection: `PluginManagementInjector` ([javadoc](./apidocs/org/apache/maven/model/management/PluginManagementInjector.html)), with its `DefaultPluginManagementInjector` implementation ([source](./xref/org/apache/maven/model/management/DefaultPluginManagementInjector.html))
    - _(optional)_ lifecycle bindings injection: `LifecycleBindingsInjector` ([javadoc](./apidocs/org/apache/maven/model/plugin/LifecycleBindingsInjector.html)), with its `DefaultLifecycleBindingsInjector` implementation in maven-core ([source](./maven-core/xref/org/apache/maven/model/plugin/DefaultLifecycleBindingsInjector.html))
    - dependency management import (for dependencies of type `pom` and scope `import` in the `<dependencyManagement>` section)
    - dependency management injection: `DependencyManagementInjector` ([javadoc](./apidocs/org/apache/maven/model/management/DependencyManagementInjector.html)), with its `DefaultDependencyManagementInjector` implementation ([source](./xref/org/apache/maven/model/management/DefaultDependencyManagementInjector.html))
    - model normalization - inject default values: `ModelNormalizer` ([javadoc](./apidocs/org/apache/maven/model/normalization/ModelNormalizer.html)), with its `DefaultModelNormalizer` implementation ([source](./xref/org/apache/maven/model/normalization/DefaultModelNormalizer.html))
    - _(optional)_ reports configuration: `ReportConfigurationExpander` ([javadoc](./apidocs/org/apache/maven/model/plugin/ReportConfigurationExpander.html)), with its `DefaultReportConfigurationExpander` implementation ([source](./xref/org/apache/maven/model/plugin/DefaultReportConfigurationExpander.html))
    - _(optional)_ reports conversion to decoupled site plugin: `ReportingConverter` ([javadoc](./apidocs/org/apache/maven/model/plugin/ReportingConverter.html)), with its `DefaultReportingConverter` implementation ([source](./xref/org/apache/maven/model/plugin/DefaultReportingConverter.html))
    - _(optional)_ plugins configuration: `PluginConfigurationExpander` ([javadoc](./apidocs/org/apache/maven/model/plugin/PluginConfigurationExpander.html)), with its `DefaultPluginConfigurationExpander` implementation ([source](./xref/org/apache/maven/model/plugin/DefaultPluginConfigurationExpander.html))
    - effective model validation: `ModelValidator` ([javadoc](./apidocs/org/apache/maven/model/validation/ModelValidator.html)), with its `DefaultModelValidator` implementation ([source](./xref/org/apache/maven/model/validation/DefaultModelValidator.html))

## Inheritance Assembly

Inheritance Assembly consists in filling current model empty fields with values taken from parent model. It is done in `InheritanceAssembler` ([javadoc](./apidocs/org/apache/maven/model/inheritance/InheritanceAssembler.html)), with its `DefaultInheritanceAssembler` implementation ([source](./xref/org/apache/maven/model/inheritance/DefaultInheritanceAssembler.html)).

By default, every model field is inherited as-is from parent, with a few exceptions that are intentionally not inherited: `modelVersion`, `artifactId`, `packaging`, `profiles` (injected in phase 1) and `prerequisites`.

Notice that the 5 URLs from the model (`project.url`, `project.scm.connection`, `project.scm.developerConnection`, `project.scm.url` and `project.distributionManagement.site.url`) have a special inheritance handling:

- if not configured in current model, the inherited value is the parent's one with current artifact id appended,
- since Maven 3.5.0, if `project.directory` POM property value is defined, it is used instead of artifact id: this permits default inheritance calculations when module directory name is not equal to artifact id. Notice that this property is not inherited from a POM to its child: child's POM will use child artifact id if property is not set.
- since Maven 3.6.1, inheritance can avoid appending any path to parent value by setting model attribute value to `false` for each url: `project/@child.project.url.inherit.append.path`, `project/distributionManagement/site/@child.site.url.inherit.append.path`, `project/scm/@child.scm.connection.inherit.append.path`, `project/scm/@child.scm.developerConnection.inherit.append.path` and `project/scm/@child.scm.url.inherit.append.path`.

## Model Interpolation

Model Interpolation consists in replacing `${...}` with calculated value. It is done in `StringVisitorModelInterpolator` ([javadoc](./apidocs/org/apache/maven/model/interpolation/StringVisitorModelInterpolator.html), [source](./xref/org/apache/maven/model/interpolation/StringVisitorModelInterpolator.html)).

Notice that model interpolation happens _after_ profile activation, and that profile activation doesn't benefit from every values: interpolation for file-based activation is limited to `${basedir}` (which was introduced in Maven 3 and is not deprecated in this context) and `${rootDirectory}` (introduced in Maven 4), system properties and user properties.

Values are evaluated in sequence from different syntaxes:

|value|evaluation result|common examples|
|:---|:---|:---|
|`project.*` <br />`pom.*` (_deprecated_) <br />`*` (_deprecated_)|POM content (see [POM reference](../maven-model/maven.html))|`${project.version}` <br />`${project.build.finalName}` <br />`${project.artifactId}` <br />`${project.build.directory}`|
|`project.basedir` <br />`pom.basedir` (_deprecated_) <br />`basedir` (_deprecated_)|the directory containing the `pom.xml` file|`${project.basedir}`|
|`project.baseUri` <br />`pom.baseUri` (_deprecated_)|the directory containing the `pom.xml` file as URI|`${project.baseUri}`|
|`project.rootDirectory`|the project's root directory (containing a `.mvn` directory or with the `root="true"` xml attribute)|`${project.rootDirectory}`|
|`build.timestamp` <br />`maven.build.timestamp`|the UTC timestamp of build start, in `yyyy-MM-dd'T'HH:mm:ss'Z'` default format, which can be overridden with `maven.build.timestamp.format` POM property|`${maven.build.timestamp}`|
|`*`|user properties, set from CLI with `-Dproperty=value`|`${skipTests}`|
|`*`|model properties, such as project properties set in the pom|`${any.key}`|
|`maven.home`|The path to the current Maven home.|`${maven.home}`|
|`maven.version`|The version number of the current Maven execution _(since 3.0.4)_. For example, "`3.0.5`".|`${maven.version}`|
|`maven.build.version`|The full build version of the current Maven execution _(since 3.0.4)_. For example, "`Apache Maven 3.2.2 (r01de14724cdef164cd33c7c8c2fe155faf9602da; 2013-02-19T14:51:28+01:00)`".|`${maven.build.version}`|
|`maven.repo.local`|The repository on the local machine Maven shall use to store installed and downloaded artifacts (POMs, JARs, etc).|`${user.home}/.m2/repository`|
|`*`|Java system properties (see [JDK reference](https://docs.oracle.com/javase/8/docs/api/java/lang/System.html#getProperties()))|`${user.home}` <br />`${java.home}`|
|`*`|User properties|`${foo}`|
|`env.*` <br />`*`|environment variables|`${env.PATH}`|
|`settings.*`|Local user settings (see [settings reference](../maven-settings/settings.html))|`${settings.localRepository}`|
|`changelist`  <br />`revision`  <br />`sha1`|CI friendly placeholders for the project version (see [Maven CI Friendly Versions](/maven-ci-friendly.html))|`1.0.0-${changelist}-SNAPSHOT`|

### Notice

- after model interpolation, `${...}` content can remain in the model that will be evaluated later when setting plugin parameters. This happens in particular with `settings.*` values for [Settings Model](../maven-settings/settings.html),
- encoding configuration have been defined as POM properties looking like POM content but not added to POM model to maintain compatibility with previous Maven versions:
    - `${project.build.sourceEncoding}` for [source files encoding](https://cwiki.apache.org/confluence/display/MAVEN/POM+Element+for+Source+File+Encoding) (defaults to `UTF-8` since Maven 4.0.0, no default value was provided in Maven 3.x, meaning that the platform encoding was used by plugins)
    - `${project.reporting.outputEncoding}` for [reporting output files encoding](https://cwiki.apache.org/confluence/display/MAVENOLD/Reporting+Encoding+Configuration) (defaults to `UTF-8` since Maven 4.0.0, no default value was provided in Maven 3.x, plugins usually defaulting to `UTF-8`)
