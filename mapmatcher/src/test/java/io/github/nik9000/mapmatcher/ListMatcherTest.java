/*
 * Copyright 2021 Nikolas Everett
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.nik9000.mapmatcher;

import static io.github.nik9000.mapmatcher.ListMatcher.matchesList;
import static io.github.nik9000.mapmatcher.MapMatcher.assertMap;
import static io.github.nik9000.mapmatcher.MapMatcher.matchesMap;
import static io.github.nik9000.mapmatcher.MapMatcherTest.SUBMATCHER;
import static io.github.nik9000.mapmatcher.MapMatcherTest.SUBMATCHER_ERR;
import static io.github.nik9000.mapmatcher.MapMatcherTest.assertDescribeTo;
import static io.github.nik9000.mapmatcher.MapMatcherTest.assertMismatch;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ListMatcherTest {
  @Test
  void emptyMatchesEmpty() {
    assertThat(List.of(), matchesList());
  }

  @Test
  @Disabled("just identified")
  void emptyMismatch() {
    assertMismatch(List.of(1), matchesList(), equalTo(""));
  }

  @Test
  void missing() {
    assertMismatch(List.of(), matchesList().item("foo"), equalTo("""
        a list containing
        0: expected "foo" but was <missing>"""));
  }

  @Test
  void wrongSimpleValue() {
    assertMismatch(List.of("foo"), matchesList().item("bar"), equalTo("""
        a list containing
        0: expected "bar" but was "foo"
        """.strip()));
  }

  @Test
  void extra() {
    StringBuilder mismatch = new StringBuilder();
    mismatch.append("a list containing\n");
    mismatch.append("0: <1>\n");
    mismatch.append("1: <unexpected> but was <2>");
    assertMismatch(List.of(1, 2), matchesList().item(1), equalTo(mismatch.toString()));
  }

  @Test
  void manyExtra() {
    assertMismatch(List.of(1, 2, 3), matchesList().item(1), equalTo("""
        a list containing
        0: <1>
        1: <unexpected> but was <2>
        2: <unexpected> but was <3>"""));
  }

  @Test
  void manyWrongSimpleValue() {
    assertMismatch(List.of(5, 6, 7), matchesList().item(1).item(6).item(10), equalTo("""
        a list containing
        0: expected <1> but was <5>
        1: <6>
        2: expected <10> but was <7>"""));
  }

  @Test
  void subMap() {
    assertMismatch(List.of(Map.of("bar", 2), 2), matchesList().item(Map.of("bar", 1)).item(2),
        equalTo("""
            a list containing
            0: a map containing
            bar: expected <1> but was <2>
            1: <2>"""));
  }

  @Test
  void subMapMatcher() {
    assertMismatch(List.of(Map.of("bar", 2), 2),
        matchesList().item(matchesMap().entry("bar", 1)).item(2), equalTo("""
            a list containing
            0: a map containing
            bar: expected <1> but was <2>
            1: <2>"""));
  }

  @Test
  void subList() {
    assertMismatch(List.of(List.of(2), 2), matchesList().item(List.of(1)).item(2), equalTo("""
        a list containing
        0: a list containing
          0: expected <1> but was <2>
        1: <2>"""));
  }

  @Test
  void subListMatcher() {
    assertMismatch(List.of(List.of(2), 2), matchesList().item(matchesList().item(1)).item(2),
        equalTo("""
            a list containing
            0: a list containing
              0: expected <1> but was <2>
            1: <2>"""));
  }


  @Test
  void subMatcher() {
    assertMismatch(List.of(2.0, 2), matchesList().item(SUBMATCHER).item(2), equalTo("""
        a list containing
        0: %ERR
        1: <2>""".replace("%ERR", SUBMATCHER_ERR)));
  }

  @Test
  void subMatcherAsValue() {
    Object item0 = SUBMATCHER;
    assertMismatch(List.of(2.0, 2), matchesList().item(item0).item(2), equalTo("""
        a list containing
        0: %ERR
        1: <2>""".replace("%ERR", SUBMATCHER_ERR)));
  }

  @Test
  void provideList() {
    assertMismatch(List.of(List.of(1), Map.of("bar", 2), 2.0),
        matchesList(List.of(List.of(1), Map.of("bar", 1), closeTo(1.0, 0.5))), equalTo("""
            a list containing
            0: a list containing
              0: <1>
            1: a map containing
            bar: expected <1> but was <2>
            2: %ERR""".replace("%ERR", SUBMATCHER_ERR)));
  }

  @Test
  public void immutable() {
    ListMatcher matcher = matchesList();
    assertMap(List.of("a"), matcher.item("a"));
    assertMap(List.of(), matcher);
  }

  @Test
  void emptyDescribeTo() {
    assertDescribeTo(matchesList(), equalTo("an empty list"));
  }

  @Test
  void simpleDescribeTo() {
    assertDescribeTo(matchesList().item(1).item(3), equalTo("""
        a list containing
        0: <1>
        1: <3>"""));
  }

  @Test
  void subListDescribeTo() {
    assertDescribeTo(matchesList().item(1).item(matchesList().item(0)), equalTo("""
        a list containing
        0: <1>
        1: a list containing
          0: <0>"""));
  }

  @Test
  void subMapDescribeTo() {
    assertDescribeTo(matchesList().item(1).item(matchesMap().entry("foo", 0)), equalTo("""
        a list containing
        0: <1>
        1: a map containing
        foo: <0>"""));
  }
}
