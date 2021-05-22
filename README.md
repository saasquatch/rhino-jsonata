# rhino-jsonata

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![JavaCI](https://github.com/saasquatch/rhino-jsonata/actions/workflows/JavaCI.yml/badge.svg)](https://github.com/saasquatch/rhino-jsonata/actions/workflows/JavaCI.yml)
[![](https://jitpack.io/v/saasquatch/rhino-jsonata.svg)](https://jitpack.io/#saasquatch/rhino-jsonata)

## Introduction

This is a [JSONata](https://jsonata.org/) library for Java backed by [jsonata-js](https://github.com/jsonata-js/jsonata) and [Rhino](https://github.com/mozilla/rhino).

### Compared to [JSONata4Java](https://github.com/IBM/JSONata4Java)

As the title suggests, this library is NOT a native implementation of JSONata in Java. If you are after a native implementation or you are very conscious about performance, then you should be looking at [JSONata4Java](https://github.com/IBM/JSONata4Java), which has a considerable performance advantage over this library for obvious reasons. What this library provides are behavioral consistency and feature parity with jsonata-js (for the most part, see the limitations section for more details), which can be valuable if you have a mixed environment where you need JSONata in both Java and JavaScript. One obvious discrepency between jsonata-js and JSONata4Java is the strictness of the parser. In jsonata-js, this expression `foo (` is considered invalid, while in JSONata4Java it is considered valid. Another advantage from the development perspective is that it stands on the shoulders of jsonata-js, so there is less code to maintain and less code to test.

## Quick start

The following is the equivalent of the [example provided by jsonata-js](https://github.com/jsonata-js/jsonata/tree/4c54db20a9782656e25aacd45df584e7c54210e6#quick-start).

```java
final ObjectMapper objectMapper = new ObjectMapper();
final JSONata jsonata = JSONata.create();
final JsonNode data = objectMapper.readTree(
    "{\"example\":[{\"value\":4},{\"value\":7},{\"value\":13}]}");
final JSONataExpression expression = jsonata.parse("$sum(example.value)");
final JsonNode result = expression.evaluate(data);
System.out.println(objectMapper.writeValueAsString(result)); // prints 24
```

For documentation on JSONata itself, please refer to the official [JSONata docs](https://docs.jsonata.org).

## Limitations

TODO

## Adding it to your project

### Add the repository

Maven

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
```

Gradle

```gradle
repositories {
  maven { url 'https://jitpack.io' }
}
```

### Add the dependency

Maven

```xml
<dependency>
  <groupId>com.github.saasquatch</groupId>
  <artifactId>rhino-jsonata</artifactId>
  <version>0.0.1</version>
</dependency>
```

Gradle

```gradle
implementation 'com.github.saasquatch:rhino-jsonata:0.0.1'
```

### Transitive Dependencies

This project requires Java 8. The only required transitive dependencies are [Rhino](https://github.com/mozilla/rhino), [Jackson](https://github.com/FasterXML/jackson), and [FindBugs (JSR305)](http://findbugs.sourceforge.net/).

### Unstable APIs

Anything marked with the `@Beta` or `@Internal` annotations are either experimental or considered private API, and can be modified in breaking ways or removed without warning.

## License

Unless explicitly stated otherwise all files in this repository are licensed under the Apache
License 2.0.

License boilerplate:

```
Copyright 2021 ReferralSaaSquatch.com Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
