/*
 * Copyright 2021 Nikolas Everett
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.nik9000.mapmatcher;

import static io.github.nik9000.mapmatcher.ListMatcher.matchesList;
import static io.github.nik9000.mapmatcher.MapMatcher.assertMap;
import static io.github.nik9000.mapmatcher.MapMatcher.matchesMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

class MapMatcherTest {
  static final Matcher<Double> SUBMATCHER = closeTo(1.0, .5);
  static final String SUBMATCHER_ERR = "expected a numeric value within <0.5> of <1.0> "
      + "but <2.0> differed by <0.5> more than delta <0.5>";

  @Test
  void emptyMatchesEmpty() {
    assertThat(Map.of(), matchesMap());
  }

  @Test
  void expectedEmptyMismatch() {
    assertMismatch(Map.of("foo", "bar"), matchesMap(), equalTo("""
        an empty map
        foo: <unexpected> but was "bar"
        """.strip()));
  }

  @Test
  void missing() {
    assertMismatch(Map.of(), matchesMap().entry("foo", "bar"), equalTo("""
        a map containing
        foo: expected "bar" but was <missing>"""));
  }

  @Test
  void wrongSimpleValue() {
    assertMismatch(Map.of("foo", "baz"), matchesMap().entry("foo", "bar"), equalTo("""
        a map containing
        foo: expected "bar" but was "baz"
        """.strip()));
  }

  @Test
  void extra() {
    assertMismatch(Map.of("foo", 1), matchesMap().entry("bar", 1), equalTo("""
        a map containing
        bar: expected <1> but was <missing>
        foo: <unexpected> but was <1>"""));
  }

  /**
   * When there are extra entries in the comparison map we iterate them in order.
   */
  @Test
  void manyExtra() {
    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("foo", 1);
    map.put("baz", 2);
    assertMismatch(map, matchesMap().entry("bar", 1), equalTo("""
        a map containing
        bar: expected <1> but was <missing>
        foo: <unexpected> but was <1>
        baz: <unexpected> but was <2>"""));
  }

  @Test
  void extraOk() {
    assertMap(Map.of("foo", 1), matchesMap().extraOk());
  }

  @Test
  void extraOkMismatchSimple() {
    assertMismatch(Map.of("foo", 1), matchesMap().entry("bar", 1).extraOk(), equalTo("""
        a map containing
        bar: expected <1> but was <missing>
        foo: <1> unexpected but ok"""));
  }

  @Test
  void extraOkMismatchExtraMap() {
    assertMismatch(Map.of("foo", Map.of("i", 1)), matchesMap().entry("bar", 1).extraOk(),
        equalTo("""
            a map containing
            bar: expected <1> but was <missing>
            foo: <{i=1}> unexpected but ok"""));
  }

  @Test
  void extraOkMismatchExtraList() {
    assertMismatch(Map.of("foo", List.of(1)), matchesMap().entry("bar", 1).extraOk(), equalTo("""
        a map containing
        bar: expected <1> but was <missing>
        foo: <[1]> unexpected but ok"""));
  }

  @Test
  void manyWrongSimpleValue() {
    assertMismatch(Map.of("foo", 1, "bar", 2, "baz", 3),
        matchesMap().entry("foo", 2).entry("bar", 2).entry("baz", 4), equalTo("""
            a map containing
            foo: expected <2> but was <1>
            bar: <2>
            baz: expected <4> but was <3>"""));
  }

  @Test
  void nullValue() {
    Map<String, Object> map = new HashMap<>();
    map.put("a", "foo");
    map.put("b", null);
    assertMap(map, expectNull());
  }

  @Test
  void expectedNull() {
    assertMismatch(Map.of("a", "foo", "b", "bar"), expectNull(), equalTo("""
        a map containing
        a: "foo"
        b: expected null but was "bar"
        """.trim()));
  }

  private MapMatcher expectNull() {
    return matchesMap().entry("a", "foo").entry("b", null);
  }

  @Test
  void expectedButWasNull() {
    Map<String, Object> map = new HashMap<>();
    map.put("a", "foo");
    map.put("b", null);
    assertMismatch(map, matchesMap().entry("a", "foo").entry("b", "bar"), equalTo("""
        a map containing
        a: "foo"
        b: expected "bar" but was null"""));
  }

  @Test
  void subMap() {
    assertMismatch(Map.of("foo", Map.of("bar", 2), "baz", 2),
        matchesMap().entry("foo", Map.of("bar", 1)).entry("baz", 2), equalTo("""
            a map containing
            foo: a map containing
              bar: expected <1> but was <2>
            baz: <2>"""));
  }

  @Test
  void subMapMismatchEmpty() {
    assertMismatch(Map.of(),
        matchesMap().entry("foo", Map.of("bar", 1)).entry("baz", 2), equalTo("""
            a map containing
            foo: expected a map but was <missing>
            baz: expected <2> but was <missing>"""));
  }

  @Test
  void subMapMatcher() {
    assertMismatch(Map.of("foo", Map.of("bar", 2), "baz", 2),
        matchesMap().entry("foo", matchesMap().entry("bar", 1)).entry("baz", 2), equalTo("""
            a map containing
            foo: a map containing
              bar: expected <1> but was <2>
            baz: <2>"""));
  }

  @Test
  void subEmptyMap() {
    StringBuilder mismatch = new StringBuilder();
    mismatch.append("a map containing\n");
    mismatch.append("foo: an empty map\n");
    mismatch.append("  bar: <unexpected> but was <2>\n");
    mismatch.append("baz: <2>");
    assertMismatch(Map.of("foo", Map.of("bar", 2), "baz", 2),
        matchesMap().entry("foo", Map.of()).entry("baz", 2),
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
        matchesMap().entry("foo", List.of(1)).entry("bar", 2),
        equalTo(mismatch.toString()));
  }

  @Test
  void subListMismatchEmpty() {
    assertMismatch(Map.of(),
        matchesMap().entry("foo", List.of(1)).entry("baz", 2), equalTo("""
            a map containing
            foo: expected a list but was <missing>
            baz: expected <2> but was <missing>"""));
  }

  @Test
  void subListMatcher() {
    assertMismatch(Map.of("foo", List.of(2), "bar", 2),
        matchesMap().entry("foo", matchesList().item(1)).entry("bar", 2), equalTo("""
            a map containing
            foo: a list containing
                0: expected <1> but was <2>
            bar: <2>"""));
  }

  @Test
  void subEmptyList() {
    StringBuilder mismatch = new StringBuilder();
    mismatch.append("a map containing\n");
    mismatch.append("foo: an empty list\n");
    mismatch.append("    0: <unexpected> but was <2>\n");
    mismatch.append("bar: <2>");
    assertMismatch(Map.of("foo", List.of(2), "bar", 2),
        matchesMap().entry("foo", List.of()).entry("bar", 2),
        equalTo(mismatch.toString()));
  }

  @Test
  void subMatcher() {
    assertMismatch(Map.of("foo", 2.0, "bar", 2),
        matchesMap().entry("foo", SUBMATCHER).entry("bar", 2), equalTo("""
            a map containing
            foo: %ERR
            bar: <2>""".replace("%ERR", SUBMATCHER_ERR)));
  }

  @Test
  void subMatcherAsValue() {
    Object foo = SUBMATCHER;
    assertMismatch(Map.of("foo", 2.0, "bar", 2), matchesMap().entry("foo", foo).entry("bar", 2),
        equalTo("""
            a map containing
            foo: %ERR
            bar: <2>""".replace("%ERR", SUBMATCHER_ERR)));
  }

  @Test
  void provideMap() {
    /*
     * Iteration order of the specification map gives the order of the
     * error message so we use a LinkedHashMap to preserve our order.
     */
    Map<String, Object> spec = new LinkedHashMap<>();
    spec.put("foo", List.of(1));
    spec.put("bar", Map.of("a", 2));
    spec.put("baz", SUBMATCHER);
    assertMismatch(Map.of("foo", List.of(2), "bar", Map.of("a", 2), "baz", 2.0),
        matchesMap(spec),
        equalTo("""
            a map containing
            foo: a list containing
                0: expected <1> but was <2>
            bar: a map containing
                a: <2>
            baz: %ERR""".replace("%ERR", SUBMATCHER_ERR)));
  }

  @Test
  void provideMapContainingNullMatch() {
    Map<String, Object> map = new HashMap<>();
    map.put("foo", 1);
    map.put("bar", null);
    assertMap(map, provideMapContainingNull());
  }


  @Test
  void provideMapContainingNullMismatch() {
    assertMismatch(Map.of("foo", 1, "bar", "c"),
        provideMapContainingNull(),
        equalTo("""
            a map containing
            foo: <1>
            bar: expected null but was "c"
            """.trim()));
  }

  private MapMatcher provideMapContainingNull() {
    /*
     * Iteration order of the specification map gives the order of the
     * error message so we use a LinkedHashMap to preserve our order.
     */
    Map<String, Object> spec = new LinkedHashMap<>();
    spec.put("foo", 1);
    spec.put("bar", null);
    return matchesMap(spec);
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
    assertDescribeTo(matchesMap().entry("foo", 1).entry("bar", 3), equalTo("""
        a map containing
        foo: <1>
        bar: <3>"""));
  }

  @Test
  void subListDescribeTo() {
    assertDescribeTo(matchesMap().entry("foo", 1).entry("bar", matchesList().item(0)), equalTo("""
        a map containing
        foo: <1>
        bar: a list containing
            0: <0>"""));
  }

  @Test
  void subMapDescribeTo() {
    assertDescribeTo(matchesMap().entry("foo", 1).entry("bar", matchesMap().entry("baz", 0)),
        equalTo("""
            a map containing
            foo: <1>
            bar: a map containing
              baz: <0>"""));
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
