/*
 * Copyright 2021 Nikolas Everett
 * SPDX-License-Identifier: Apache-2.0
 */

package com.github.nik9000.mapmatcher;

import static com.github.nik9000.mapmatcher.ListMatcher.matchesList;
import static com.github.nik9000.mapmatcher.MapMatcher.assertMap;
import static com.github.nik9000.mapmatcher.MapMatcher.matchesMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

class MapMatcherTest {
  @Test
  void emptyMatchesEmpty() {
    assertThat(Map.of(), matchesMap());
  }

  @Test
  void missing() {
    StringBuilder mismatch = new StringBuilder();
    mismatch.append("a map containing\n");
    mismatch.append("foo: expected \"bar\" but was <missing>");
    assertMismatch(Map.of(), matchesMap().entry("foo", "bar"), equalTo(mismatch.toString()));
  }

  @Test
  void wrongSimpleValue() {
    StringBuilder mismatch = new StringBuilder();
    mismatch.append("a map containing\n");
    mismatch.append("foo: expected \"bar\" but was \"baz\"");
    assertMismatch(Map.of("foo", "baz"),
        matchesMap().entry("foo", "bar"),
        equalTo(mismatch.toString()));
  }

  @Test
  void extra() {
    StringBuilder mismatch = new StringBuilder();
    mismatch.append("a map containing\n");
    mismatch.append("bar: expected <1> but was <missing>\n");
    mismatch.append("foo: <unexpected> but was <1>");
    assertMismatch(Map.of("foo", 1), matchesMap().entry("bar", 1), equalTo(mismatch.toString()));
  }

  /**
   * When there are extra entries in the comparison map we iterate them in order.
   */
  @Test
  void manyExtra() {
    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("foo", 1);
    map.put("baz", 2);
    StringBuilder mismatch = new StringBuilder();
    mismatch.append("a map containing\n");
    mismatch.append("bar: expected <1> but was <missing>\n");
    mismatch.append("foo: <unexpected> but was <1>\n");
    mismatch.append("baz: <unexpected> but was <2>");
    assertMismatch(map, matchesMap().entry("bar", 1), equalTo(mismatch.toString()));
  }

  @Test
  void manyWrongSimpleValue() {
    StringBuilder mismatch = new StringBuilder();
    mismatch.append("a map containing\n");
    mismatch.append("foo: expected <2> but was <1>\n");
    mismatch.append("bar: <2>\n");
    mismatch.append("baz: expected <4> but was <3>");
    assertMismatch(Map.of("foo", 1, "bar", 2, "baz", 3),
        matchesMap().entry("foo", 2).entry("bar", 2).entry("baz", 4),
        equalTo(mismatch.toString()));
  }

  @Test
  void subMap() {
    StringBuilder mismatch = new StringBuilder();
    mismatch.append("a map containing\n");
    mismatch.append("foo: a map containing\n");
    mismatch.append("  bar: expected <1> but was <2>\n");
    mismatch.append("baz: <2>");
    assertMismatch(Map.of("foo", Map.of("bar", 2), "baz", 2),
        matchesMap().entry("foo", matchesMap().entry("bar", 1)).entry("baz", 2),
        equalTo(mismatch.toString()));
  }

  @Test
  void subList() {
    StringBuilder mismatch = new StringBuilder();
    mismatch.append("a map containing\n");
    mismatch.append("foo: a list containing\n");
    mismatch.append("    0: expected <1> but was <2>\n");
    mismatch.append("bar: <2>");
    assertMismatch(Map.of("foo", List.of(2), "bar", 2),
        matchesMap().entry("foo", matchesList().item(1)).entry("bar", 2),
        equalTo(mismatch.toString()));
  }

  @Test
  void big() throws IOException {
    assertMap(read("es-response.json"),
        matchesMap().entry("took", 57.0)
            .entry("timed_out", false)
            .entry("_shards",
                matchesMap().entry("total", 1.0)
                    .entry("successful", 1.0)
                    .entry("skipped", 0.0)
                    .entry("failed", 0.0))
            .entry("hits",
                matchesMap()
                    .entry("total", matchesMap().entry("value", 10000.0).entry("relation", "gte"))
                    .entry("max_score", 1.0)
                    .entry("hits",
                        matchesList().item(matchesMap().entry("_index", "nyc_taxis")
                            .entry("_id", "SIjZyXcBsaR104_0ECjx")
                            .entry("_score", 1.0)
                            .entry("_source",
                                matchesMap().entry("extra", 0.5)
                                    .entry("tolls_amount", 0.0)
                                    .entry("passenger_count", 1.0)
                                    .entry("store_and_fwd_flag", "N")
                                    .entry("tip_amount", 1.76)
                                    .entry("mta_tax", 0.5)
                                    .entry("improvement_surcharge", 0.3)
                                    .entry("fare_amount", 7.5)
                                    .entry("dropoff_datetime", "2015-07-23 21:45:16")
                                    .entry("total_amount", 10.56)
                                    .entry("rate_code_id", "1")
                                    .entry("payment_type", "1")
                                    .entry("vendor_id", "2")
                                    .entry("pickup_datetime", "2015-07-23 21:37:38")
                                    .entry("trip_distance", 1.59)
                                    .entry("pickup_location",
                                        matchesList().item(closeTo(-73.97788, 0.000005))
                                            .item(closeTo(40.75482, 0.000005)))
                                    .entry("dropoff_location",
                                        matchesList().item(closeTo(-73.95908, 0.000005))
                                            .item(closeTo(40.76345, 0.000005))))))));
  }

  @Test
  public void immutable() {
    MapMatcher matcher = matchesMap();
    assertMap(Map.of("a", "a"), matcher.entry("a", "a"));
    assertMap(Map.of(), matcher);
  }

  private Map<?, ?> read(String file) throws IOException {
    try (InputStream data = Thread.currentThread()
        .getContextClassLoader()
        .getResourceAsStream(file)) {
      return new Gson().fromJson(new InputStreamReader(data, StandardCharsets.UTF_8), Map.class);
    }
  }

  @Test
  void emptyDescribeTo() {
    assertDescribeTo(matchesMap(), equalTo("an empty map"));
  }

  @Test
  void simpleDescribeTo() {
    StringBuilder description = new StringBuilder();
    description.append("a map containing\n");
    description.append("foo: <1>\n");
    description.append("bar: <3>");
    assertDescribeTo(matchesMap().entry("foo", 1).entry("bar", 3), equalTo(description.toString()));
  }

  @Test
  void subListDescribeTo() {
    StringBuilder description = new StringBuilder();
    description.append("a map containing\n");
    description.append("foo: <1>\n");
    description.append("bar: a list containing\n");
    description.append("    0: <0>");
    assertDescribeTo(matchesMap().entry("foo", 1).entry("bar", matchesList().item(0)),
        equalTo(description.toString()));
  }

  @Test
  void subMapDescribeTo() {
    StringBuilder description = new StringBuilder();
    description.append("a map containing\n");
    description.append("foo: <1>\n");
    description.append("bar: a map containing\n");
    description.append("  baz: <0>");
    assertDescribeTo(matchesMap().entry("foo", 1).entry("bar", matchesMap().entry("baz", 0)),
        equalTo(description.toString()));
  }

  static <T> void assertMismatch(T v, Matcher<? super T> matcher,
      Matcher<String> mismatchDescriptionMatcher) {
    assertMap(v, not(matcher));
    StringDescription description = new StringDescription();
    matcher.describeMismatch(v, description);
    assertThat(description.toString(), mismatchDescriptionMatcher);
  }

  static void assertDescribeTo(Matcher<?> matcher, Matcher<String> describeToMatcher) {
    StringDescription description = new StringDescription();
    matcher.describeTo(description);
    assertThat(description.toString(), describeToMatcher);
  }
}
