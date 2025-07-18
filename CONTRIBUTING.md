<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
Contributing to Apache Maven
======================

You have found a bug or you have an idea for a cool new feature? Contributing
code is a great way to give something back to the open source community. Before
you dig right into the code, there are a few guidelines that we need
contributors to follow so that we can have a chance of keeping on top of
things.

Some of the ideas are documented in the [Maven Wiki][maven-wiki]
which might be interesting to read and for further discussion.

Getting Started
---------------

+ Make sure you have a [GitHub account](https://github.com/signup/free).
+ If you're planning to implement a new feature, it makes sense to discuss your changes
  on the [dev list](https://maven.apache.org/mailing-lists.html) first.
  This way you can make sure you're not wasting your time on something that isn't
  considered to be in Apache Maven's scope.
+ Submit a ticket for your issue, assuming one does not already exist.
  + Clearly describe the issue, including steps to reproduce when it is a bug.
  + Make sure you fill in the earliest version that you know has the issue.
+ Fork the repository on GitHub.

Making and Submitting Changes
--------------

We accept Pull Requests via GitHub. The [developer mailing list][dev-ml-list] is the
main channel of communication for contributors.  
There are some guidelines which will make applying PRs easier for us:
+ Create a topic branch from where you want to base your work (this is usually the master branch).
  Push your changes to a topic branch in your fork of the repository.
+ Make commits of logical units.
+ Respect the original code style: by using the same [codestyle][code-style],
  patches should only highlight the actual difference, not being disturbed by any formatting issues:
  + Only use spaces for indentation.
  + Create minimal diffs - disable on save actions like reformat source code or organize imports.
    If you feel the source code should be reformatted, create a separate PR for this change.
  + Check for unnecessary whitespace with `git diff --check` before committing.
+ Make sure you have added the necessary tests (JUnit/[Core IT tests][core-it]) for your changes.
+ Run all the tests with `mvn -Prun-its verify` to assure nothing else was accidentally broken.
+ Submit a pull request to the repository in the Apache organization.

If you plan to contribute on a regular basis, please consider filing a [contributor license agreement][cla].

Additional Resources
--------------------

+ [Contributing patches](https://maven.apache.org/guides/development/guide-maven-development.html#Creating_and_submitting_a_patch)
+ [Apache Maven JIRA project page](https://issues.apache.org/jira/projects/MNG/)
+ [Contributor License Agreement][cla]
+ [General GitHub documentation](https://help.github.com/)
+ [GitHub pull request documentation](https://docs.github.com/en/github/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request-from-a-fork)
+ [Apache Maven X Account](https://x.com/ASFMavenProject)
+ [Apache Maven Bluesky Account](https://bsky.app/profile/maven.apache.org)
+ [Apache Maven Mastodon Account](https://mastodon.social/deck/@ASFMavenProject@fosstodon.org)

[dev-ml-list]: https://maven.apache.org/mailing-lists.html
[code-style]: https://maven.apache.org/developers/conventions/code.html
[cla]: https://www.apache.org/licenses/#clas
[maven-wiki]: https://cwiki.apache.org/confluence/display/MAVEN/Index
[core-it]: https://maven.apache.org/core-its/core-it-suite/
