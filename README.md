Blankningsregistret
===============

[![Build Status](https://travis-ci.org/w3stling/blankningsregistret.svg?branch=master)](https://travis-ci.org/w3stling/blankningsregistret)
[![Download](https://api.bintray.com/packages/apptastic/maven-repo/blankningsregistret/images/download.svg)](https://bintray.com/apptastic/maven-repo/blankningsregistret/_latestVersion)
[![Javadoc](https://img.shields.io/badge/javadoc-2.0.1-blue.svg)](https://w3stling.github.io/blankningsregistret/javadoc/2.0.1)
[![License](http://img.shields.io/:license-MIT-blue.svg?style=flat-round)](http://apptastic-software.mit-license.org)   
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=com.apptastic%3Ablankningsregistret&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.apptastic%3Ablankningsregistret)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=com.apptastic%3Ablankningsregistret&metric=coverage)](https://sonarcloud.io/component_measures?id=com.apptastic%3Ablankningsregistret&metric=Coverage)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=com.apptastic%3Ablankningsregistret&metric=bugs)](https://sonarcloud.io/component_measures?id=com.apptastic%3Ablankningsregistret&metric=bugs)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=com.apptastic%3Ablankningsregistret&metric=vulnerabilities)](https://sonarcloud.io/component_measures?id=com.apptastic%3Ablankningsregistret&metric=vulnerabilities)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=com.apptastic%3Ablankningsregistret&metric=code_smells)](https://sonarcloud.io/component_measures?id=com.apptastic%3Ablankningsregistret&metric=code_smells)


[Blankningsregistret][1] is a Swedish financial registry maintained by
the [Finansinspektionen][2] (FI). It contains information short selling.

FI will on a daily basis, normally shortly after 15:30 o'clock, publish significant
net short positions in shares (>0.5 percent) in the document below.

This Java library makes it easier to automate data extraction from Blankningsregistret.

Examples
--------

### Search short positions
Get all net short positions published.

```java
Blankningsregistret registry = new Blankningsregistret();

List<NetShortPosition> positions = registry.search()
        .collect(Collectors.toList());
```

Get all net short positions published in Hennes & Maurtiz AB (ISIN SE0000106270).

```java
Blankningsregistret registry = new Blankningsregistret();

List<NetShortPosition> positions = registry.search()
        .filter(p -> p.getIsin().equals("SE0000106270"))
        .collect(Collectors.toList());
```

Find Goldman Sachs International latest short position. 

```java
Blankningsregistret registry = new Blankningsregistret();

Optional<NetShortPosition> position = registry.search()
        .filter(p -> p.getPositionHolder().equals("Goldman Sachs International"))
        .findFirst();
```

Java System Properties
----------------------
| Key | Description | Default |
| :--- | :--- | :--- |
| https.proxyHost | The host name of the proxy server. |   |
| https.proxyPort | The port number of the proxy server. |   |

Download
--------

Download [the latest JAR][3] or grab via [Maven][4] or [Gradle][5].

### Maven setup
Add JCenter repository for resolving artifact:
```xml
<project>
    ...
    <repositories>
        <repository>
            <id>jcenter</id>
            <url>https://jcenter.bintray.com</url>
        </repository>
    </repositories>
    ...
</project>
```

Add dependency declaration:
```xml
<project>
    ...
    <dependencies>
        <dependency>
            <groupId>com.apptastic</groupId>
            <artifactId>blankningsregistret</artifactId>
            <version>2.0.1</version>
        </dependency>
    </dependencies>
    ...
</project>
```

### Gradle setup
Add JCenter repository for resolving artifact:
```groovy
repositories {
    jcenter()
}
```

Add dependency declaration:
```groovy
dependencies {
    implementation 'com.apptastic:blankningsregistret:2.0.1'
}
```

Blankningsregistret library requires at minimum Java 11.

License
-------

    MIT License
    
    Copyright (c) 2018, Apptastic Software
    
    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:
    
    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.
    
    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.


[1]: https://www.fi.se/en/our-registers/short-selling/
[2]: https://www.fi.se
[3]: https://bintray.com/apptastic/maven-repo/blankningsregistret/_latestVersion
[4]: https://maven.apache.org
[5]: https://gradle.org