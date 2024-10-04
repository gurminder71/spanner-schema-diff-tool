package com.uber.spannerddl.schema;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.fail;

import com.google.cloud.solutions.spannerddl.diff.DdlDiffException;
import com.google.cloud.solutions.spannerddl.testUtils.ReadTestDatafile;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import org.junit.Test;

public class SchemaTest {
  @Test
  public void validateSchemaYAML() throws IOException {
    Map<String, String> tests = ReadTestDatafile.readDdlSegmentsFromFile("schemaDdl.txt");
    Map<String, String> expects =
        ReadTestDatafile.readDdlSegmentsFromFile("expectedSchemaYaml.txt", true);

    Iterator<Entry<String, String>> testIt = tests.entrySet().iterator();
    Iterator<Entry<String, String>> expectedIt = expects.entrySet().iterator();

    while (testIt.hasNext() && expectedIt.hasNext()) {
      Map.Entry<String, String> test = testIt.next();
      String segmentName = test.getKey();
      String expected = expectedIt.next().getValue();

      try {
        Schema schema = new Schema(test.getValue());
        String yamlContents = schema.writeToYaml();

        assertWithMessage("Mismatch for section " + segmentName)
            .that(yamlContents)
            .isEqualTo(expected);
      } catch (DdlDiffException e) {
        fail(
            "DdlDiffException when processing segment:\n'" + segmentName + "''\n" + e.getMessage());
      }
    }
  }
}
