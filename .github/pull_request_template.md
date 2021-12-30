Following this checklist to help us incorporate your
contribution quickly and easily:

 - [ ] Make sure there is a [JIRA issue](https://issues.apache.org/jira/browse/MNG) filed
       for the change (usually before you start working on it).  Trivial changes like typos do not
       require a JIRA issue. Your pull request should address just this issue, without
       pulling in other changes.
 - [ ] Each commit in the pull request should have a meaningful subject line and body.
 - [ ] Format the pull request title like `[MNG-XXX] SUMMARY`, where you replace `MNG-XXX`
       and `SUMMARY` with the appropriate JIRA issue. Best practice is to use the JIRA issue
       title in the pull request title and in the first line of the commit message.
 - [ ] Write a pull request description that is detailed enough to understand what the pull request does, how, and why.
 - [ ] Run `mvn clean verify` to make sure basic checks pass. A more thorough check will
       be performed on your pull request automatically.
 - [ ] You have run the [Core IT][core-its] successfully.

If your pull request is about ~20 lines of code you don't need to sign an
[Individual Contributor License Agreement](https://www.apache.org/licenses/icla.pdf) if you are unsure
please ask on the developers list.

To make clear that you license your contribution under
the [Apache License Version 2.0, January 2004](http://www.apache.org/licenses/LICENSE-2.0)
you have to acknowledge this by using the following check-box.

 - [ ] I hereby declare this contribution to be licenced under the [Apache License Version 2.0, January 2004](http://www.apache.org/licenses/LICENSE-2.0)

 - [ ] In any other case, please file an [Apache Individual Contributor License Agreement](https://www.apache.org/licenses/icla.pdf).

[core-its]: https://maven.apache.org/core-its/core-it-suite/
