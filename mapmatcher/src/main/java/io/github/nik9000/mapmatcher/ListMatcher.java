/*
 * Copyright 2021 Nikolas Everett
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.nik9000.mapmatcher;

import static java.util.Collections.emptyList;
import static io.github.nik9000.mapmatcher.MapMatcher.describeEntry;
import static io.github.nik9000.mapmatcher.MapMatcher.describeEntryMissing;
import static io.github.nik9000.mapmatcher.MapMatcher.describeEntryUnexepected;
import static io.github.nik9000.mapmatcher.MapMatcher.describeEntryValue;
import static io.github.nik9000.mapmatcher.MapMatcher.describeMatcher;
import static io.github.nik9000.mapmatcher.MapMatcher.maxKeyWidthForMatcher;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matcher for {@link List Lists} that reports all errors at once.
 */
public class ListMatcher extends TypeSafeMatcher<List<?>> {
  /**
   * Create a {@linkplain ListMatcher} that matches empty lists.
   */
  public static ListMatcher matchesList() {
    return new ListMatcher(emptyList());
  }

  private final List<Matcher<?>> matchers;

  private ListMatcher(List<Matcher<?>> matchers) {
    this.matchers = matchers;
  }

  /**
   * Expect a value.
   *
   * @return a new {@link ListMatcher} that expects another item
   */
  public ListMatcher item(Object value) {
    return item(equalTo(value));
  }

  /**
   * Expect a {@link Matcher}.
   *
   * @return a new {@link ListMatcher} that expects another item
   */
  public ListMatcher item(Matcher<?> valueMatcher) {
    List<Matcher<?>> matchers = new ArrayList<>(this.matchers);
    matchers.add(valueMatcher);
    return new ListMatcher(matchers);
  }

  /**
   * {@inheritDoc}
   *
   * @hidden
   */
  @Override
  public void describeTo(Description description) {
    describeTo(keyWidth(emptyList()), description);
  }

  int keyWidth(List<?> item) {
    int max = Integer.toString(matchers.size()).length();
    Iterator<?> value = item.iterator();
    Iterator<Matcher<?>> matcher = matchers.iterator();
    while (matcher.hasNext()) {
      max = Math.max(max,
          maxKeyWidthForMatcher(value.hasNext() ? value.next() : null, matcher.next()));
    }
    return max;
  }

  void describeTo(int keyWidth, Description description) {
    if (matchers.isEmpty()) {
      description.appendText("an empty list");
      return;
    }
    description.appendText("a list containing");
    int index = 0;
    for (Matcher<?> matcher : matchers) {
      describeMatcher(keyWidth, index++, matcher, description);
    }
  }

  @Override
  protected boolean matchesSafely(List<?> item) {
    if (item.size() != matchers.size()) {
      return false;
    }
    Iterator<?> value = item.iterator();
    Iterator<Matcher<?>> matcher = matchers.iterator();
    while (matcher.hasNext()) {
      if (false == matcher.next().matches(value.next())) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected void describeMismatchSafely(List<?> item, Description description) {
    describePotentialMismatch(keyWidth(item), item, description);
  }

  void describePotentialMismatch(int keyWidth, List<?> item, Description description) {
    if (matchers.isEmpty()) {
      description.appendText("an empty list");
      return;
    }
    description.appendText("a list containing");
    int maxKeyWidth = Integer.toString(Math.max(item.size(), matchers.size())).length();
    String keyFormat = "%" + maxKeyWidth + "s";

    Iterator<?> value = item.iterator();
    Iterator<Matcher<?>> matcher = matchers.iterator();
    int index = 0;
    while (matcher.hasNext()) {
      describeEntry(keyWidth, String.format(Locale.ROOT, keyFormat, index++), description);
      if (false == value.hasNext()) {
        describeEntryMissing(matcher.next(), description);
        continue;
      }
      describeEntryValue(keyWidth, matcher.next(), value.next(), description);
    }
    while (value.hasNext()) {
      describeEntry(keyWidth, String.format(Locale.ROOT, keyFormat, index++), description);
      describeEntryUnexepected(value.next(), description);
    }
  }
}
