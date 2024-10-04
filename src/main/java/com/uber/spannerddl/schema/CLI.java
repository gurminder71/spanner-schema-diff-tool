package com.uber.spannerddl.schema;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.cloud.solutions.spannerddl.diff.DdlDiff;
import com.google.cloud.solutions.spannerddl.diff.DdlDiffException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CLI {
  static final String DDL_FILE_OPT = "ddlFile";
  static final String SCHEMA_FILE_OPT = "schemaFile";
  static final String HELP_OPT = "help";

  static Path ddlPath;
  static Path schemaPath;

  private static void parseCommandLine(String[] args) {
    try {
      CommandLine commandLine = new DefaultParser().parse(buildOptions(), args);
      if (commandLine.hasOption(DdlDiff.HELP_OPT)) {
        printHelpAndExit();
      }

      ddlPath = new File(commandLine.getOptionValue(DDL_FILE_OPT)).toPath();
      schemaPath = new File(commandLine.getOptionValue(SCHEMA_FILE_OPT)).toPath();

    } catch (ParseException e) {
      System.err.println("Failed parsing command line: " + e.getMessage());
      printHelpAndExit();
    }
  }

  static Options buildOptions() {
    Options options = new Options();
    options.addOption(
        Option.builder()
            .longOpt(DDL_FILE_OPT)
            .desc("File path to the DDL definition")
            .hasArg()
            .argName("FILE")
            .type(File.class)
            .required()
            .build());
    options.addOption(
        Option.builder()
            .longOpt(SCHEMA_FILE_OPT)
            .desc("File path to the output schema YAML")
            .hasArg()
            .argName("FILE")
            .type(File.class)
            .required()
            .build());
    options.addOption(Option.builder().longOpt(HELP_OPT).desc("Show help").build());
    return options;
  }

  static void printHelpAndExit() {
    String usage = "Parse the Google Spanner DDL file and output a schema YAML file.";
    new HelpFormatter().printHelp(usage, buildOptions());
    System.exit(0);
  }

  public static void main(String[] args) {
    parseCommandLine(args);

    try {
      String ddl = new String(Files.readAllBytes(ddlPath), UTF_8);
      Schema schema = new Schema(ddl);

      String yamlContents = schema.writeToYaml();
      Files.write(schemaPath, yamlContents.getBytes(UTF_8));
    } catch (DdlDiffException e) {
      System.err.println("Failed to parse: " + e.getMessage());
      System.exit(1);
    } catch (IOException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }
}
