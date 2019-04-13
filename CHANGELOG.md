Changelog
===

## 5.3

Release date: April ?, 2019

* Update Jetty from `9.4.15.v20190215` to `9.4.17.v20190418`
  * [9.4.17 changelog](https://github.com/eclipse/jetty.project/releases/tag/jetty-9.4.17.v20190418)

## 5.2

Release date: March 22, 2019

* [JENKINS-56591](https://issues.jenkins-ci.org/browse/JENKINS-56591) cipher exclusion configurable
* Update Jetty from `9.4.12.v20180830` to `9.4.15.v20190215`
  * [9.4.15 changelog](https://github.com/eclipse/jetty.project/releases/tag/jetty-9.4.15.v20190215)

## 5.1

Release date: Oct 06, 2018

* Update Jetty from `9.4.11.v20180605` to `9.4.12.v20180830`
  * [9.4.12 changelog](https://github.com/eclipse/jetty.project/releases/tag/jetty-9.4.12.v20180830)

## 5.0

Release date: Aug 29, 2018

* [JENKINS-52358](https://issues.jenkins-ci.org/browse/JENKINS-52358) - Fix broken Http/2 support (regression in 4.4)
* [JENKINS-52121](https://issues.jenkins-ci.org/browse/JENKINS-52121) - `sessionEviction` flag was missing in the embedded Winstone Documentation
* [JENKINS-53239](https://issues.jenkins-ci.org/browse/JENKINS-53239) - Use default Jetty `QueuedThreadPool` instead of the custom `winstone.BoundedExecutorService`. 
Related issues:
  * [JENKINS-33412](https://issues.jenkins-ci.org/browse/JENKINS-33412) - Jenkins locks when started in HTTPS mode on a host with 37+ processors.
  * [JENKINS-52804](https://issues.jenkins-ci.org/browse/JENKINS-52804) - 
  Jenkins web GUI hangs when using a computer with more then 70 CPU's.
  * [JENKINS-51136](https://issues.jenkins-ci.org/browse/JENKINS-51136) - 
  Jenkins UI hangs 2.117 - 2.119.

Breaking changes:

* `winstone.BoundedExecutorService` class was removed
* `handlerCountMax` and `handlerCountMaxIdle` parameters are now deprecated and do not have any effect

## 4.4

Release date: Jun 14, 2018

* [PR #49](https://github.com/jenkinsci/winstone/pull/49) -
Update Jetty from `9.4.8.v20171121` to `9.4.11.v20180605`
  * [9.4.11 changelog](https://github.com/eclipse/jetty.project/releases/tag/jetty-9.4.11.v20180605)
  * [9.4.10 changelog](https://github.com/eclipse/jetty.project/releases/tag/jetty-9.4.10.v20180503)
  * [9.4.9 changelog](https://github.com/eclipse/jetty.project/releases/tag/jetty-9.4.9.v20180320)

## 4.3

Release date: May 05, 2018

* [PR-47](https://github.com/jenkinsci/winstone/pull/47) - 
Option to enable [Jetty JMX](https://www.eclipse.org/jetty/documentation/9.4.x/jmx-chapter.html) 

## 4.2

Release date: April 1, 2018

* [PR #44](https://github.com/jenkinsci/winstone/pull/44) -
Update Jetty from `9.4.5.v20170502` to `9.4.8.v20171121` 
  * [9.4.8 changelog](https://github.com/eclipse/jetty.project/releases/tag/jetty-9.4.8.v20171121)
  * [9.4.7 changelog](https://github.com/eclipse/jetty.project/releases/tag/jetty-9.4.7.v20170914)
  * [9.4.6 changelog](https://github.com/eclipse/jetty.project/releases/tag/jetty-9.4.6.v20170531)
* [PR #46](https://github.com/jenkinsci/winstone/pull/46) -
Internal: Update codebase to use the standard [Jenkins parent POM](https://github.com/jenkinsci/pom) 

## 4.1.2

Release date: Feb 23, 2017

* [JENKINS-49596](https://issues.jenkins-ci.org/browse/JENKINS-49596) -
Increase the default idle session eviction timeout to 30 minutes. 

## 4.1.1

Release date: Feb 22, 2017

* [JENKINS-49596](https://issues.jenkins-ci.org/browse/JENKINS-49596) -
Prevent User session memory leak by setting the default idle session eviction timeout to 3 minutes. 
* Allow configuring the session eviction timeout via the `-sessionEviction` command-line option.

##4.1

Release date: July 20, 2017

* add HTTP/2 Support (disabled per default)

## 4.0

Release date: May 04, 2017

* Update Jetty from `9.2.15.v20160210` to `9.4.5.v20170502`
* Remove the deprecated [SPDY](http://www.eclipse.org/jetty/documentation/9.1.5.v20140505/spdy.html) protocol mode.
* [JENKINS-40693](https://issues.jenkins-ci.org/browse/JENKINS-40693) - 
Jetty 9.4.5: Prevent the <i>400 Bad Host header</i> error for <code>HttpChannelOverHttp</code> when operating behind reverse proxy
([Jetty issue #592](https://github.com/eclipse/jetty.project/issues/592)).
* Update minimal Java requirement to Java 8

## Previous releases

See the commit history.
