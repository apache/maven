[PROPOSAL] Route-M Auto-Mirror Configuration for Maven 3
=======================================================

Introduction
------------

Currently, configuring Maven to use mirrors is an extremely manual process. The opportunities for human error involved in this configuration severely limit our practical ability to build a decentralized, distributed server infrastructure for serving Maven repositories. Moreover, this manual configuration problem makes the use of in-house repository managers unnecessarily difficult and error-prone.

In addition to the configuration difficulties related to using mirrors, Maven's centralized repository structure is very difficult to serve. Maven has achieved a level of popularity such that the central repository requires incredibly high bandwidth to avoid network errors when users resolve dependencies for their projects. This problem is exacerbated by the usage design in tools like m2eclipse, that seems to encourage users to re-resolve the central repository's indexes very frequently. We've reached a point in the evolution of Maven where it's not feasible to host the central repository on donated infrastructure. The central repository is therefore unscalable, and therefore the design for resolving artifacts needs to be revisited.

As part of this redesign, an automatic mirror-selection algorithm built directly into Maven would allow the use of a distributed network of Maven repositories. This would transform the scalability of Maven's repository infrastructure at a stroke. At the same time, an automatic mirror-selector would eliminate many of the configuration errors and confusion common to new users working in customized environments, as in the presence of a corporate repository manager.

This proposal involves using a lightweight, centralized - but distributable - "routing table" document that describes the available mirrors, which is used by Maven to automatically select a mirror for a given repository. The routing table in question is meant to support various metrics that will allow Maven to give preference to the most appropriate mirrors, say those with better performance characteristics, closer geographical locations, etc. Once Maven makes a selection for a given repository, it will store that selection in a separate configuration file within $HOME/.m2 to avoid incurring the same selection cost next time around. Finally, Maven will support configuration of this auto-selection logic, to supply a different URL for the routing table, select which routing table URL discovery strategies should be used, and provide authentication for accessing the routing table URL.


Backward Compatibility
----------------------

While this new auto-mirroring feature will transform the user experience and allow better scaling for the repository infrastructure, it's also critical not to interfere with configurations that are already working. Therefore, the auto-mirroring logic will defer to any mirrors configured in the Maven settings.xml. This allows the auto-mirror selector to remain in the background, dormant, until it's used without a specific mirror configuration in the settings.


The Routing Table Model
-----------------------

The anatomy of a routing table document is fairly simple. This is a JSON document that consists of an array of mirror entries. Each mirror entry contains its own URL, id, enabled flag, and list of canonical repository URLs it mirrors. Additionally, it currently contains a simple weight integer, which allows Maven to favor selection of mirrors with a higher weight than others. Eventually, it may make sense to extend the mirror specification to include geolocation attributes like latitude and longitude, or other advanced selection data. For now, a simple weight rating will allow Maven to make a weighted-random selection to help spread load effectively over all available mirrors.


Discovering the Routing Table
-----------------------------

For the most bare-bones use case, Maven should be configured by default to load a routing table document hosted on http://repository.apache.org. This location will be the fall-back URL even for users whose environment and configuration reference other routing-table URLs.

In addition to the default routing table, it will also be possible to retrieve the routing table in two other ways. First, if the user has configured a property called 'router-url' in the $HOME/.m2/conf/automirror.properties file. If this property is available, Maven will attempt a HTTP GET to retrieve the routing table from that URL. If supplied, the 'router-user' and 'router-password' properties can be used to configure authentication for this request. 

If there is no router-url property or the configuration file doesn't exist, Maven will automatically attempt to locate a routing table URL using DNS. It will determine your localhost's DNS domain and attempt to retrieve a TXT record for the entry '_maven.domain.org'. If it finds a TXT record by this name, it will attempt a HTTP GET to retrieve the routing table JSON document. Once again, the 'router-user' and 'router-password' properties can be used to authenticate this request. If neither of these discovery attempts work, Maven will fall back to the canonical routing table URL. If the user configures the 'discovery-strategies' property to 'none', this DNS discovery step will be skipped.

While it would be possible to host the default routing table on the main maven.apache.org site, using repository.apache.org offers the flexibility to store or generate the routing table. For instance, we could store mirror records in a CouchDB instance, or generate them based on the contents and configuration of a repository manager. Using a more dynamic source for the canonical routing table gives us the opportunity to write a UI for maintaining the mirror entries. For example, it would be relatively straightforward to write a Javascript- and CouchDB-based application to support simple operations like enabling or disabling a mirror, updating a mirror's weight, or adding and removing mirrors.

[ORIGINAL README FOLLOWS]
=========================

Homepage
========
<http://maven.apache.org/>

Wiki
====
<https://cwiki.apache.org/confluence/display/MAVEN/Index>

Issue Tracker
=============
<http://jira.codehaus.org/browse/MNG>

License
=======
[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)
