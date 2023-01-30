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

  ---
  Offline Mode Design
  ---
  John Casey
  ---
  2005-04-08
  ---

Offline Mode Design

* UPDATE: 18-April-2005

  We cannot take the approach outlined below of detecting which remote
  repositories are "really" offline, since offline mode is more of a behavior,
  and this will lead to counter-intuitive results. A different feature may exist
  unimplemented, which is to detect when the network is down and provide better
  feedback for that case. However, when offline mode is declared, nothing
  earmarked as remote should be accessed, regardless of whether it is actually
  a physically local resource.

  NOTE: One side-effect of this design change is that all offline-mode code can
  be isolated to maven-core, maven-plugin-descriptor, and [possibly]
  maven-artifact. Usage of maven-wagon will be determined by an offline-aware
  manager.

* Assumptions: What is Offline?

  For the purposes of determining the areas sensitive to offline status,
  it is definitely useful to define what the offline state really means.

  [[1]] This is obvious, but the network/internet is unavailable.

  [[2]] Localhost (127.0.0.1) may also be unavailable if the whole
        network stack is offline.

  [[3]] "Remote" repositories referenced using the file:// protocol may
        be available. However, if that file:// url references a
        file-share, as in the case of an NFS or SMB mount, that will
        be unavailable.

  So, offline mode has several implications, some of which may not be
  altogether obvious:

  * Localhost may be unavailable. Therefore, even locally installed
    server processes which work by conversing over a port may fail.

  * Not all "remote" repositories will fail. Specifically, if the remote
    repo uses the file:// protocol, and it doesn't refer to a shared
    filesystem, it will continue to be available.

  The question remaining is: Which level of offline mode will we support? It
  seems reasonable to assume that users will be able to tell when localhost is
  not active (in most cases, localhost should be available, even if the rest of
  the network is not). Therefore, let's concentrate on the state where no
  network <beyond localhost> exists, and leave the more extreme state to users
  to diagnose and correct as necessary.

* Why is Offline Mode Important?

  Offline mode is essential for breaking the requirement that m2 operate in a
  network-connected environment. It means legitimizing a development environment
  in which there is no network connection, and providing a robust m2 service
  offering in such circumstances. Introduction of offline mode allows m2 to
  anticipate the inevitable network failures that accompany being physically
  disconnected from the network, and adjust it's behavior accordingly.

  It is more than simply understanding that m2 cannot go and check for the
  latest version of some snapshot artifact. If m2 is offline, SCM operations
  cannot succeed; no artifact downloads can take place, regardless of whether
  they are snapshot versions; artifact deployment cannot take place; certain
  types of tests cannot be setup, since the container used to run them cannot be
  reached or started.

  All of these operations will produce their own unique errors in the absence of
  a coordinated offline strategy. In addition, efforts to unite these failing
  behaviors behind a consistent user interface is much, much more difficult if
  the system can't tell whether it has access to the network required by these
  operations.

  Offline mode really means anticipating a lack of network connectivity, and as
  a result turning off certain services provided by m2 and providing a coherent
  way of predicting and reporting when network-related failures will take place.
  It means warning users that since the network is missing, certain features and
  operations will be unavailable, rather than simply waiting for those
  operations to fail, then trying to help users decipher the error messages they
  get as a result.

* Implications for Resolution

** Dependency Resolution

  This one is obvious...we only have access to the repositories using
  the file:// protocol and living on a truly local filesystem when
  offline.

** Plugin Resolution

  This is similar to dependency resolution. Plugin repositories not
  using file:// or not residing on a local (not shared) filesystem will
  be unavailable.


* Implications for Mojo Execution

** Deployment mojos

  The concept of deployment is dependent on the availability of a some
  remote repository. Just as above, if that repository is not using
  file:// (which is highly likely to be the case), or the repository is
  not on a local filesystem, deployment will fail when offline.

** Testing mojos

  This can be a problem if the tests are more than simple unit tests;
  that is, if they require configuration of a server process, and
  subsequent testing in-container.

  Since we're only going to concern ourselves with states where localhost is
  still active, we only need to worry about this case when the server container
  is <<not>> installed on localhost. This allows the popular pattern of starting
  a server container in-JVM, running tests against it, and shutting it down.

** SCM mojos

  See below for discussion on SCM-related operations. Any mojo which
  carries out some analysis or other interaction with a SCM system
  will likely be unavailable when in offline mode.


* Implications for Subsystems

** Maven-Wagon

  Parts of Wagon will continue to function normally. These include:

  * The file wagon, provided the referenced location is on a local
    filesystem.

    It is not possible to determine whether a file-based location will
    be available except on a case-by-case basis (or a root-url by
    root-url basis). We may want to move the offline sensitivity entirely to
    Maven-Artifact, below, so we can be smarter about testing filesystem-based
    repositories, etc.

  * If not otherwise specified, all other wagons are assumed to be
    remote-only, and are therefore sensitive to offline mode.

** Maven-Artifact

  This is wholly dependent on Maven-Wagon, above.

  We could possibly use a flag on a particular Wagon to see whether it supports
  offline mode, and then test to see if the file-based basedir for an artifact
  repository works...if it doesn't work, we can mark that repository offline...

  OTOH, all offline-mode checks can probably be run from Wagon-based APIs.

** Maven-SCM

  In all but trivial examples, SCM operations cannot complete without
  having access to the versioning server. Therefore, it is assumed that
  any SCM-related activity will be unavailable when m2 is in offline
  mode.

** Maven-Core

  We'll examine the different parts of maven-core on a case-by-case
  basis, below:

*** DefaultLifecycleExecutor

  When binding goals to the project's configured lifecycle, each mojo
  descriptor should declare whether it requires online/offline status.
  This value should be a java.lang.Boolean, so it can implement 3VL
  (three value logic: yes, no, don't-care). The requiresOnline
  field in the mojo descriptor has the following semantics:

  [true] Online status is required for this mojo to function
         correctly.

  [false] <<(Default)>> Either status is acceptable for the mojo to
  				execute. It doesn't care.

  The majority of mojos will leave the requiresOnline == false,
  since online/offline status will be irrelevant, provided they have
  access to their required artifacts and other classpath elements. In the case
  of required artifacts and other classpath elements, this is assumed by the
  mojo API to be in a correct state, and will be handled by the Wagon
  modifications.


* Implementation Notes

** Accessibility of offline status

  Offline status should be indicated in the MavenSettings instance, since it
  can conceivably be set from either the settings.xml or the command-line.

  In the event the '-o' switch is the impetus for setting offline mode, this
  should result in modification of the active profile in the MavenSettings
  instance, just as definition of the active profile from the command-line
  should result in similar modification. This object is not meant to be
  static within the build process, but rather to be setup as an aggregation of
  all settings-related information passed into the system.

** Control over downloads

  Find the control point for m2 using maven-wagon. At this point, inject
  a offline status parameter which is used when retrieving the specific Wagon.

  If <<<offline == true>>>:

  * If the wagon is not bound to "file://", then ignore the request and print
    a debug message.

  * If the wagon is bound to "file://" then:

    Retrieve the file or base-url file to be "downloaded".

    * If the file (or more usefully, the base-url file) exists, proceed.

    * If the file (or base-url file) doesn't exist, assume that this location
      is part of a file-share. Ignore the request and print a debug message
      as above.

** Control over mojos in the lifecycle

  When binding a mojo to the project's lifecycle instance, check the mojo
  descriptor's requiredConnectivity field.

  * If <<<(offline == true) && (requiresOnline != true)>>>, bind
    the mojo to the lifecycle.

    In this case, the client is <<offline>>, and the mojo does not require
    online status.

  * If <<<(offline == false) && (requiresOnline == true)>>>, bind
    the mojo to the lifecycle.

    In this case, the client is <<online>>, and the mojo either requires
    <<online>> status, or else doesn't care.

  * Otherwise, don't bind the mojo. Log a debug message to indicate that it is
    sensitive the the online state of the application, and that this state is
    currently wrong for execution.

    <<NOTE:>> Do we want to fail when we cannot bind a mojo to the lifecycle
    because of offline/online status? That would probably indicate that the user
    was trying to do something they cannot succeed at for now...so we probably
    should throw an exception in this case.


