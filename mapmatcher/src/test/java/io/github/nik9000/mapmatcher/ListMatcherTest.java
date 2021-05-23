/*
 * Copyright 2021 Nikolas Everett
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.nik9000.mapmatcher;

import static io.github.nik9000.mapmatcher.ListMatcher.matchesList;
import static io.github.nik9000.mapmatcher.MapMatcher.assertMap;
import static io.github.nik9000.mapmatcher.MapMatcher.matchesMap;
import static io.github.nik9000.mapmatcher.MapMatcherTest.assertDescribeTo;
import static io.github.nik9000.mapmatcher.MapMatcherTest.assertMismatch;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ListMatcherTest {
  @Test
  void emptyMatchesEmpty() {
    assertThat(List.of(), matchesList());
  }

  @Test
  void missing() {
    StringBuilder mismatch = new StringBuilder();
    mismatch.append("a list containing\n");
    mismatch.append("0: expected \"foo\" but was <missing>");
    assertMismatch(List.of(), matchesList().item("foo"), equalTo(mismatch.toString()));
  }

  @Test
  void wrongSimpleValue() {
    StringBuilder mismatch = new StringBuilder();
    mismatch.append("a list containing\n");
    mismatch.append("0: expected \"bar\" but was \"foo\"");
    assertMismatch(List.of("foo"), matchesList().item("bar"), equalTo(mismatch.toString()));
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
    StringBuilder mismatch = new StringBuilder();
    mismatch.append("a list containing\n");
    mismatch.append("0: <1>\n");
    mismatch.append("1: <unexpected> but was <2>\n");
    mismatch.append("2: <unexpected> but was <3>");
    assertMismatch(List.of(1, 2, 3), matchesList().item(1), equalTo(mismatch.toString()));
  }

  @Test
  void manyWrongSimpleValue() {
    StringBuilder mismatch = new StringBuilder();
    mismatch.append("a list containing\n");
    mismatch.append("0: expected <1> but was <5>\n");
    mismatch.append("1: <6>\n");
    mismatch.append("2: expected <10> but was <7>");
    assertMismatch(List.of(5, 6, 7),
        matchesList().item(1).item(6).item(10),
        equalTo(mismatch.toString()));
  }

  @Test
  void subMap() {
    StringBuilder mismatch = new StringBuilder();
    mismatch.append("a list containing\n");
    mismatch.append("0: a map containing\n");
    mismatch.append("bar: expected <1> but was <2>\n");
    mismatch.append("1: <2>");
    assertMismatch(List.of(Map.of("bar", 2), 2),
        matchesList().item(Map.of("bar", 1)).item(2),
        equalTo(mismatch.toString()));
  }

  @Test
  void subMapMatcher() {
    StringBuilder mismatch = new StringBuilder();
    mismatch.append("a list containing\n");
    mismatch.append("0: a map containing\n");
    mismatch.append("bar: expected <1> but was <2>\n");
    mismatch.append("1: <2>");
    assertMismatch(List.of(Map.of("bar", 2), 2),
        matchesList().item(matchesMap().entry("bar", 1)).item(2),
        equalTo(mismatch.toString()));
  }

  @Test
  void subList() {
    StringBuilder mismatch = new StringBuilder();
    mismatch.append("a list containing\n");
    mismatch.append("0: a list containing\n");
    mismatch.append("  0: expected <1> but was <2>\n");
    mismatch.append("1: <2>");
    assertMismatch(List.of(List.of(2), 2),
        matchesList().item(List.of(1)).item(2),
        equalTo(mismatch.toString()));
  }

  @Test
  void subListMatcher() {
    StringBuilder mismatch = new StringBuilder();
    mismatch.append("a list containing\n");
    mismatch.append("0: a list containing\n");
    mismatch.append("  0: expected <1> but was <2>\n");
    mismatch.append("1: <2>");
    assertMismatch(List.of(List.of(2), 2),
        matchesList().item(matchesList().item(1)).item(2),
        equalTo(mismatch.toString()));
  }


  @Test
  void subMatcher() {
    StringBuilder mismatch = new StringBuilder();
    mismatch.append("a list containing\n");
    mismatch.append("0: expected a numeric value within <0.5> of <1.0> but");
    mismatch.append(" <2.0> differed by <0.5> more than delta <0.5>\n");
    mismatch.append("1: <2>");
    assertMismatch(List.of(2.0, 2),
        matchesList().item(closeTo(1.0, 0.5)).item(2),
        equalTo(mismatch.toString()));
  }

  @Test
  void subMatcherAsValue() {
    StringBuilder mismatch = new StringBuilder();
    mismatch.append("a list containing\n");
    mismatch.append("0: expected a numeric value within <0.5> of <1.0> but");
    mismatch.append(" <2.0> differed by <0.5> more than delta <0.5>\n");
    mismatch.append("1: <2>");
    Object item0 = closeTo(1.0, 0.5);
    assertMismatch(List.of(2.0, 2),
        matchesList().item(item0).item(2),
        equalTo(mismatch.toString()));
  }

  @Test
  void provideList() {
    StringBuilder mismatch = new StringBuilder();
    mismatch.append("a list containing\n");
    mismatch.append("0: a list containing\n");
    mismatch.append("  0: <1>\n");
    mismatch.append("1: a map containing\n");
    mismatch.append("bar: expected <1> but was <2>\n");
    mismatch.append("2: expected a numeric value within <0.5> of <1.0> but");
    mismatch.append(" <2.0> differed by <0.5> more than delta <0.5>");

    assertMismatch(List.of(List.of(1), Map.of("bar", 2), 2.0),
        matchesList(List.of(List.of(1), Map.of("bar", 1), closeTo(1.0, 0.5))),
        equalTo(mismatch.toString()));
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
    StringBuilder description = new StringBuilder();
    description.append("a list containing\n");
    description.append("0: <1>\n");
    description.append("1: <3>");
    assertDescribeTo(matchesList().item(1).item(3), equalTo(description.toString()));
  }

  @Test
  void subListDescribeTo() {
    StringBuilder description = new StringBuilder();
    description.append("a list containing\n");
    description.append("0: <1>\n");
    description.append("1: a list containing\n");
    description.append("  0: <0>");
    assertDescribeTo(matchesList().item(1).item(matchesList().item(0)),
        equalTo(description.toString()));
  }

  @Test
  void subMapDescribeTo() {
    StringBuilder description = new StringBuilder();
    description.append("a list containing\n");
    description.append("0: <1>\n");
    description.append("1: a map containing\n");
    description.append("foo: <0>");
    assertDescribeTo(matchesList().item(1).item(matchesMap().entry("foo", 0)),
        equalTo(description.toString()));
  }
}
