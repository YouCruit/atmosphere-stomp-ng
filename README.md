Atmosphere Stomp NG [![Build Status](https://travis-ci.org/YouCruit/atmosphere-stomp-ng.svg?branch=master)](https://travis-ci.org/YouCruit/atmosphere-stomp-ng)
=============

This is a clean implementation (although inspired by) of atmosphere-stomp. Both plugins are
for accessing Atmosphere via STOMP. This plugin differs in that it allows the use of wildcards in subscriptions
and sends. 

Features:
* STOMP 1.0, 1.1 and 1.2 protocols
* Atmosphere
* Wildcard for SEND and SUBSCRIBE events
* Allow/Disallow SUBSCRIBE
* SEND are received by the server, so no security nightmares with intra-client communication without
  server vetting
* Supports MQ
* Configurable mapping-url per servlet/filter

**Setting up the development environment**

Setting up the different programs necessary to develop (not run)
it in IntelliJ.

* JDK 8 (JRE is untested)

** Running **

To build and start, run
```
./gradlew build
```

**Recommendations**
* Add the function ```gw () { $(git rev-parse --show-toplevel)/gradlew "$@" }``` to avoid having to do ```../../../gradlew```
* Only run ```gradlew build```, ```gradlew clean build``` should not be necessary and slows down development a *lot*.