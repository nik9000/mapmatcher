/*
 * Copyright 2021 Nikolas Everett
 * SPDX-License-Identifier: Apache-2.0
 */
package com.github.nik9000.mapmatcher;

import static org.hamcrest.Matchers.equalTo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.StringDescription;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matcher for {@link Map Maps} that reports all errors at once.
 */
public class MapMatcher extends TypeSafeMatcher<Map<?, ?>> {
  private static final int INDENT = 2;

  /**
   * Create an empty {@linkplain MapMatcher}.
   */
  public static MapMatcher matchesMap() {
    return new MapMatcher();
  }

  /**
   * Assert match. Shorter output on failure than {@link MatcherAssert#assertThat(Object, Matcher)}
   * that looks better for {@link MapMatcher}.
   */
  public static void assertMap(Map<?, ?> actual, MapMatcher matcher) {
    assertMap("", actual, matcher);
  }

  /**
   * Assert match. Shorter output on failure than {@link MatcherAssert#assertThat(Object, Matcher)}
   * that looks better for {@link MapMatcher}.
   */
  public static void assertMap(String reason, Map<?, ?> actual, MapMatcher matcher) {
    assertThat(reason, actual, matcher);
  }

  static <T> void assertThat(String reason, T actual, Matcher<? super T> matcher) {
    if (matcher.matches(actual)) {
      return;
    }
    
    Description description = new StringDescription();
    description.appendText(reason).appendText("Expected ");
    matcher.describeMismatch(actual, description);

    throw new AssertionError(description.toString());
  }

  private final Map<Object, Matcher<?>> matchers = new LinkedHashMap<>();

  private MapMatcher() {
  }

  /**
   * Expect a value.
   *
   * @return this for chaining
   */
  public MapMatcher entry(Object key, Object value) {
    return entry(key, equalTo(value));
  }

  /**
   * Expect a {@link Matcher}.
   *
   * @return this for chaining
   */
  public MapMatcher entry(Object key, Matcher<?> valueMatcher) {
    Matcher<?> old = matchers.put(key, valueMatcher);
    if (old != null) {
      throw new IllegalArgumentException("Already had an entry for [" + key + "]: " + old);
    }
    return this;
  }

  /**
   * {@inheritDoc}
   * @hidden
   */
  @Override
  public void describeTo(Description description) {
    describeTo(keyWidth(Map.of()), description);
  }

  int keyWidth(Map<?, ?> item) {
    int max = 0;
    for (Object k : item.keySet()) {
      max = Math.max(max, k.toString().length());
    }
    for (Map.Entry<Object, Matcher<?>> e : matchers.entrySet()) {
      max = Math.max(max, e.getKey().toString().length());
      max = Math.max(max, maxKeyWidthForMatcher(item.get(e.getKey()), e.getValue()));
    }
    return max;
  }

  static int maxKeyWidthForMatcher(Object item, Matcher<?> matcher) {
    if (matcher instanceof MapMatcher) {
      Map<?, ?> longestSubMap = item instanceof Map ? (Map<?, ?>) item : Map.of();
      return ((MapMatcher) matcher).keyWidth(longestSubMap) - INDENT;
    }
    if (matcher instanceof ListMatcher) {
      List<?> longestSubList = item instanceof List ? (List<?>) item : List.of();
      return ((ListMatcher) matcher).keyWidth(longestSubList) - INDENT;
    }
    return 0;
  }

  void describeTo(int keyWidth, Description description) {
    if (matchers.isEmpty()) {
      description.appendText("an empty map");
      return;
    }
    description.appendText("a map containing");
    for (Map.Entry<?, Matcher<?>> e : matchers.entrySet()) {
      describeMatcher(keyWidth, e.getKey(), e.getValue(), description);
    }
  }

  static void describeMatcher(int keyWidth, Object key, Matcher<?> matcher, Description description) {
    String keyFormat = "\n%" + keyWidth + "s";
    description.appendText(String.format(Locale.ROOT, keyFormat, key)).appendText(": ");
    if (matcher instanceof MapMatcher) {
      ((MapMatcher) matcher).describeTo(keyWidth + INDENT, description);
      return;
    }
    if (matcher instanceof ListMatcher) {
      ((ListMatcher) matcher).describeTo(keyWidth + INDENT, description);
      return;
    }
    description.appendDescriptionOf(matcher);
  }

  @Override
  protected boolean matchesSafely(Map<?, ?> item) {
    if (item.size() != matchers.size()) {
      return false;
    }
    if (false == item.keySet().equals(matchers.keySet())) {
      return false;
    }
    for (Map.Entry<Object, Matcher<?>> e : matchers.entrySet()) {
      if (false == item.containsKey(e.getKey())) {
        return false;
      }
      Object v = item.get(e.getKey());
      if (false == e.getValue().matches(v)) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected void describeMismatchSafely(Map<?, ?> item, Description description) {
    describePotentialMismatch(keyWidth(item), item, description);
  }

  void describePotentialMismatch(int keyWidth, Map<?, ?> item, Description description) {
    if (matchers.isEmpty()) {
      description.appendText("an empty map");
      return;
    }
    description.appendText("a map containing");
    int maxKeyWidth = Stream.concat(matchers.keySet().stream(), item.keySet().stream())
        .mapToInt(k -> k.toString().length())
        .max()
        .getAsInt();
    String keyFormat = "%" + maxKeyWidth + "s";

    for (Map.Entry<Object, Matcher<?>> e : matchers.entrySet()) {
      describeEntry(keyWidth, String.format(Locale.ROOT, keyFormat, e.getKey()), description);
      if (false == item.containsKey(e.getKey())) {
        describeEntryMissing(e.getValue(), description);
        continue;
      }
      describeEntryValue(keyWidth, e.getValue(), item.get(e.getKey()), description);
    }
    for (Map.Entry<?, ?> e : item.entrySet()) {
      if (false == matchers.containsKey(e.getKey())) {
        describeEntry(keyWidth, String.format(Locale.ROOT, keyFormat, e.getKey()), description);
        describeEntryUnexepected(e.getValue(), description);
      }
    }
  }

  static void describeEntry(int keyWidth, Object key, Description description) {
    String keyFormat = "\n%" + keyWidth + "s";
    description.appendText(String.format(Locale.ROOT, keyFormat, key)).appendText(": ");
  }

  static void describeEntryMissing(Matcher<?> matcher, Description description) {
    description.appendText("expected ").appendDescriptionOf(matcher);
    description.appendText(" but was <missing>");
  }

  static void describeEntryUnexepected(Object value, Description description) {
    description.appendText("<unexpected> but was ");
    description.appendValue(value);
  }

  static void describeEntryValue(int keyWidth, Matcher<?> matcher, Object v, Description description) {
    if (v instanceof Map && matcher instanceof MapMatcher) {
      ((MapMatcher) matcher).describePotentialMismatch(keyWidth + INDENT, (Map<?, ?>) v, description);
      return;
    }
    if (v instanceof List && matcher instanceof ListMatcher) {
      ((ListMatcher) matcher).describePotentialMismatch(keyWidth + INDENT, (List<?>) v, description);
      return;
    }
    if (false == matcher.matches(v)) {
      description.appendText("expected ").appendDescriptionOf(matcher).appendText(" but ");
      matcher.describeMismatch(v, description);
      return;
    }
    description.appendValue(v);
  }
}
