# Changelog

The [git commit history](https://github.com/takari/maven-wrapper/commits/master)
is the detailed source of all changes. The following provides most information
at an easier glance.

## Version 0.5.4 - soonish

- Adapt mvnw.cmd to also honour MVNW_VERBOSE
  - see https://github.com/takari/maven-wrapper/pull/115
  - contributed by Brian de Alwis https://github.com/briandealwis
- Updated documentation to include plugin version

## Version 0.5.3 - 2019-02-25

- MVNW_REPOURL check bug in mvnw
  - fixes https://github.com/takari/maven-wrapper/issues/109
  - see https://github.com/takari/maven-wrapper/pull/111
  - contributed by  Piotrek Żygieło https://github.com/pzygielo and 
	Sebastian Peters https://github.com/sepe81,

Release performed by Manfred Moser - http://www.simpligility.com

## Version 0.5.2 - 2019-02-20

- Re-applied TLS1.2 fix for Windows
  - see https://github.com/takari/maven-wrapper/pull/108
  - contributed by Ralph Schaer https://github.com/ralscha

Release performed by Manfred Moser - http://www.simpligility.com

## Version 0.5.1 - 2019-02-19

- Corrected check for MVNW_REPOURL in mvnw.cmd
  - fixes https://github.com/takari/maven-wrapper/issues/106
  - see https://github.com/takari/maven-wrapper/pull/107
  - contributed by Manfred Moser http://www.simpligility.com

Release performed by Manfred Moser - http://www.simpligility.com

## Version 0.5.0 - 2019-02-18

- update to Maven 3.6.0 as default
- Use TLS 1.2 on Windows
  - see https://github.com/takari/maven-wrapper/pull/89
  - contributed by Julian Hyde https://github.com/julianhyde
- Fix compile path for cygwin
  - see https://github.com/takari/maven-wrapper/pull/83
  - contributed by Dániel Kovács https://github.com/AFulgens
- Fix for path with spaces on Windows 
  - see https://github.com/takari/maven-wrapper/pull/101
  - contributed by Paul Stöhr https://github.com/CiTuX
- Support for authentication to corporate repository manager
  - see https://github.com/takari/maven-wrapper/pull/96
  - see https://github.com/takari/maven-wrapper/pull/86
  - contributed by Sebastian Peters https://github.com/sepe81,
    Sander Wartenberg https://github.com/sanderino666 and
    Manfred Moser http://www.simpligility.com
- Consistent license header across all Java source
  - see https://github.com/takari/maven-wrapper/pull/105
  - fixes https://github.com/takari/maven-wrapper/issues/104
  - contributed by Manfred Moser http://www.simpligility.com
- Support MVNW_REPOURL environment variable usage
  -  during wrapper install and wrapper usage
  - for wrapper and maven distro download
  - in scripts and wrapper binary
    - contributed by Manfred Moser http://www.simpligility.com

Release performed by Manfred Moser - http://www.simpligility.com

## Version 0.4.2 - 2018-07-02

- update to Maven 3.5.4 as default
- dependency updates
- end of line in properties file
- parent pom upgrade

Release performed by Manfred Moser - http://www.simpligility.com

## Version 0.4.0 - 2018-02-19

- Allow for wrapper to work without the jar file
  - see https://github.com/takari/maven-wrapper/pull/60
  - see https://github.com/takari/maven-wrapper/pull/70
  - fixed https://github.com/takari/maven-wrapper/issues/24
  - contributed by  Christofer Dutz https://github.com/chrisdutz
  - and Manfred Moser - http://www.simpligility.com
- Change URLs used to official Maven repo URL
  - done as part of above changes

Release performed by Manfred Moser - http://www.simpligility.com

## Version 0.3.0 - 2017-10-25

- Set title on shell window in Windows
  - see https://github.com/takari/maven-wrapper/pull/66
  - contributed by https://github.com/hcgpragt
- Change default Maven version to 3.5.2 in wrapper config
  - contributed by Manfred Moser - http://www.simpligility.com
- Respect MVNW_VERBOSE when printing progress reporting and diagnostic info
  - see https://github.com/takari/maven-wrapper/pull/67
  - contributed by Konstantin Sobolev https://github.com/eprst
- Smaller chunk size for download progress reporting
  - contributed by Manfred Moser - http://www.simpligility.com

Release performed by Manfred Moser - http://www.simpligility.com

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
