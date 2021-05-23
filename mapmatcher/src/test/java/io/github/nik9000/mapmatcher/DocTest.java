/*
 * Copyright 2021 Nikolas Everett
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.nik9000.mapmatcher;

import static io.github.nik9000.mapmatcher.MapMatcher.assertMap;
import static io.github.nik9000.mapmatcher.MapMatcher.matchesMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Tests for the examples in the README file.
 */
public class DocTest {
  @Test
  void codeMatches() throws IOException {
    String path = DocTest.class.getName().replace('.', File.separatorChar);
    String file = Files.readString(Path.of("src/test/java/" + path + ".java"),
        StandardCharsets.UTF_8);
    int start = file.indexOf("// CODESTART\n");
    assert start > 0;
    start = file.indexOf('\n', start) + 1;
    int end = file.indexOf("// CODEEND\n", start);
    end = file.lastIndexOf('\n', end);
    assert end > 0;
    assertThat(file.substring(start, end).replaceAll("(?m)^      ", ""),
        equalTo(readmeChunk("code")));
  }

  @Test
  void maven() throws IOException {
    assertThat(readmeChunk("maven"), containsString("<version>" + lastRelease() + "</version>\n"));
  }

  @Test
  void gradle() throws IOException {
    assertThat(readmeChunk("gradle"),
      equalTo("testImplementation 'io.github.nik9000:mapmatcher:" + lastRelease() + "'"));
  }

  @Test
  void explicitExample() throws IOException {
    try {
      // @formatter:off
      // CODESTART
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
      // CODEEND
      // @formatter:on
    } catch (AssertionError e) {
      assertThat(e.toString(), equalTo(readmeChunk("failure-message")));
    }
  }

  private String readmeChunk(String name) throws IOException {
    String file = Files.readString(Path.of("../README.md"), StandardCharsets.UTF_8);
    int start = file.indexOf("<a name=\"" + name + "\"></a>");
    if (start < 0) {
      throw new IllegalArgumentException("Couldn't find [" + name + "]");
    }
    start = file.indexOf('\n', start) + 1;
    start = file.indexOf('\n', start) + 1;
    int end = file.indexOf("```", start);
    if (end < 0) {
      throw new IllegalArgumentException("Chunk [" + name + "] doesn't end");
    }
    return file.substring(start, end - 1);
  }

  private String lastRelease() {
    String version = System.getenv("version");
    assertThat("gradle will set this", version, notNullValue());
    if (false == version.endsWith("-SNAPSHOT")) {
      return version;
    }
    version = version.substring(0, version.length() - "-SNAPSHOT".length());
    int start = version.lastIndexOf(".") + 1;
    int end = version.length();
    String last = version.substring(start, end);
    int next = Integer.parseInt(last) - 1;
    return version.substring(0, start) + next;
  }
}
