# MapMatcher

Hamcrest matchers for `Map` and `List` focussing on readable error messages
that include the entire structure:

<a name="failure-message"></a>
```
java.lang.AssertionError: Expected a map containing
 foo: <2>
 bar: expected <3> but was <2>
 baz: <2>
list: a list containing
     0: <2>
     1: <3>
     2: expected <5> but was <4>
     3: expected <6> but was <missing>
 sub: a map containing
     a: <1>
     b: <2>
```

Comes from:

<a name="code"></a>
```
assertMap(Map.of(
    "foo", 2,
    "bar", 2,
    "baz", 2,
    "list", List.of(2, 3, 4),
    "sub", Map.of(
      "a", 1,
      "b", 2
    )
  ),
  matchesMap()
    .entry("foo", 2)
    .entry("bar", 3)
    .entry("baz", greaterThan(1))
    .entry("list", List.of(2, greaterThan(2), 5, 6))
    .entry("sub", matchesMap()
      .entry("a", 1)
      .entry("b", both(greaterThan(1)).and(lessThan(3)))));
```

Use it in maven with something like:
<a name="maven"></a>
```
<dependency>
  <groupId>io.github.nik9000</groupId>
  <artifactId>mapmatcher</artifactId>
  <version>0.0.2</version>
  <scope>test</scope>
</dependency>
```

Or use it in gradle with something like:
<a name="gradle"></a>
```
testImplementation 'io.github.nik9000:mapmatcher:0.0.2'
```
