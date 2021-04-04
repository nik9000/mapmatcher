# MapMatcher

Hamcrest matchers for `Map` and `List` that match all elements at once
so the failure messages show the whole structure, calling out differences:

<a name="failure-message"></a>
```
java.lang.AssertionError: Expected a map containing
   list: a list containing
        0: <2>
        1: <3>
        2: expected <5> but was <4>
        3: expected <6> but was <missing>
element: expected <3> but was <2>
    sub: a map containing
        a: <1>
        b: <2>
```

Comes from:

<a name="code"></a>
```
assertMap(Map.of(
    "list", List.of(2, 3, 4),
    "element", 2,
    "sub", Map.of(
      "a", 1,
      "b", 2
    )
  ),
  matchesMap()
    .entry("list", matchesList().item(2).item(3).item(5).item(6))
    .entry("element", 3)
    .entry("sub", matchesMap()
      .entry("a", 1)
      .entry("b", both(greaterThan(1)).and(lessThan(3)))));
```
