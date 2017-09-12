# Changelog

The [git commit history](https://github.com/takari/maven-wrapper/commits/master) is the detailed source of all changes.
The following provides most information at an easier glance.

## Version 0.2.2 and 0.2.3 - 2017-09-12

- Print out Maven Wrapper version with each invocation
  - see https://github.com/takari/maven-wrapper/pull/48
  - contributed by Tadaya Tsuyukubo - https://github.com/ttddyy
- Verbose mode
  - only print out basedir and version in verbose mode
  - set by environment variable MVNW_VERBOSE="true"
  - fixed NPE with https://github.com/takari/maven-wrapper/pull/65
  - contributed by Manfred Moser - http://www.simpligility.com
  - and Tadaya Tsuyukubo https://github.com/ttddyy

Release performed by Manfred Moser - http://www.simpligility.com

## Version 0.2.1 - 2017-04-21

- Compile wrapper with Java 1.5 language level to allow usage with Java1.5 +
  - see https://github.com/takari/maven-wrapper/pull/45
  - contributed by Mark Chesney - https://github.com/mches

Release performed by Manfred Moser - http://www.simpligility.com

## Version 0.2.0 - 2017-04-17

- Use Apple-recommended strategy for locating JAVA_HOME
  - see https://github.com/takari/maven-wrapper/pull/14
  - contributed by Karsten Sperling - https://github.com/ksperling
- Be defensive about creating directories for files
  - see https://github.com/takari/maven-wrapper/pull/29
  - contributed by Dave Syer - https://github.com/dsyer 
- Fix for paths with spaces on Windows
  - see https://github.com/takari/maven-wrapper/pull/21
  - contributed by Andreas Ahlenstorf - https://github.com/aahlenst
  - also see https://github.com/takari/maven-wrapper/pull/33
  - contributed by https://github.com/vicaya
  - fixes https://github.com/takari/maven-wrapper/issues/31
  - tested on Linux, OSX and Windows
- Removed unused MAVEN_CMD_LINE_ARGS
  - see https://github.com/takari/maven-wrapper/pull/28
  - contributed by Michal Domagala - https://github.com/michaldo
- Fix for stale wrapper jar file
  - see https://github.com/takari/maven-wrapper/pull/36
  - contributed by Mark Chesney - https://github.com/mches
  - and Manfred Moser - http://www.simpligility.com
  - fixes https://github.com/takari/maven-wrapper/issues/37
- Plugin and other project updates
  - contributed by Manfred Moser - http://www.simpligility.com
- Support for spaces in path on Unix
  - see https://github.com/takari/maven-wrapper/pull/25
  - contributed by https://github.com/beverage
  - and Manfred Moser - http://www.simpligility.com
- Upgraded default Maven version to 3.5.0
  - contributed by Manfred Moser - http://www.simpligility.com
  - and Fred Bricon
- Support for simple shell like dash
  - fixes https://github.com/takari/maven-wrapper/issues/34
  - tested by Manfred Moser  - http://www.simpligility.com
- Support for Solaris
  - see https://github.com/takari/maven-wrapper/pull/40
  - fixes https://github.com/takari/maven-wrapper/issues/34
  - contributed by Pelisse Romain https://github.com/rpelisse

Release performed by Manfred Moser - http://www.simpligility.com

## Version 0.1.6 - 2016-10-17

See the commit [git commit history](https://github.com/takari/maven-wrapper/commits/master).

## Version 0.1.5 and earlier

See the commit [git commit history](https://github.com/takari/maven-wrapper/commits/master).