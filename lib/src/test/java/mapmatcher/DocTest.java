package mapmatcher;

import static mapmatcher.ListMatcher.matchesList;
import static mapmatcher.MapMatcher.assertMap;
import static mapmatcher.MapMatcher.matchesMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class DocTest {
  @Test
  public void codeMatches() throws IOException {
    String file = Files.readString(Path.of("src/test/java/mapmatcher/DocTest.java"), StandardCharsets.UTF_8);
    int start = file.indexOf("// CODESTART\n");
    assert start > 0;
    start = file.indexOf('\n', start) + 1;
    int end = file.indexOf("// CODEEND\n", start);
    end = file.lastIndexOf('\n', end);
    assert end > 0;
    assertThat(file.substring(start, end).replaceAll("(?m)^      ", ""), equalTo(readmeChunk("code")));
  }

  @Test
  public void errorMessageMatches() throws IOException {
    try {
      // CODESTART
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
            .entry("b", both(greaterThan(1)).and(lessThan(3)))
          )
        );
      // CODEEND
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
}
