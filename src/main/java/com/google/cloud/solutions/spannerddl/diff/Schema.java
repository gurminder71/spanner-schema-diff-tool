package com.google.cloud.solutions.spannerddl.diff;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.cloud.solutions.spannerddl.parser.ASTcolumn_def;
import com.google.cloud.solutions.spannerddl.parser.ASTcolumn_type;
import com.google.cloud.solutions.spannerddl.parser.ASTcolumns;
import com.google.cloud.solutions.spannerddl.parser.ASTcreate_index_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTcreate_search_index_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTcreate_table_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTpartition_key;
import com.google.cloud.solutions.spannerddl.parser.ASTstored_column;
import com.google.cloud.solutions.spannerddl.parser.ASTstored_column_list;
import com.google.cloud.solutions.spannerddl.parser.ASTtable;
import com.google.cloud.solutions.spannerddl.parser.ASTtoken_key_list;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Spanner schema is defined using Google SQL. The parsed SQL is transformed to Schema and saved as
 * YAML so that downstream can use the information as YAML can be easily read
 */
public class Schema {
  public List<Table> tables;
  public List<Index> indexes;

  public Schema(
      Collection<ASTcreate_table_statement> ddlTables,
      Collection<ASTcreate_index_statement> ddlIndexes,
      Collection<ASTcreate_search_index_statement> ddlSearchIndexes) {
    tables = new ArrayList<>();
    indexes = new ArrayList<>();

    ddlTables.stream().map(Table::new).forEach(tables::add);
    ddlIndexes.stream().map(Index::new).forEach(indexes::add);
    ddlSearchIndexes.stream().map(Index::new).forEach(indexes::add);
  }

  public void writeToYaml(Path path) throws IOException {
    Representer representer =
        new Representer(new DumperOptions()) {
          @Override
          protected NodeTuple representJavaBeanProperty(
              Object javaBean, Property property, Object propertyValue, Tag customTag) {
            // if value of property is null, ignore it.
            if (propertyValue == null) {
              return null;
            }
            return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
          }
        };

    representer.setPropertyUtils(
        new PropertyUtils() {
          @Override
          protected Set<Property> createPropertySet(
              Class<? extends Object> type, BeanAccess bAccess) {
            // do not sort bean properties
            return new LinkedHashSet<>(getPropertiesMap(type, bAccess).values());
          }
        });

    Yaml yaml = new Yaml(representer);
    String output = yaml.dumpAsMap(this);
    Files.write(path, output.getBytes(UTF_8));
  }

  static class Table {
    public String name;
    public String[] primary_keys;
    public List<TableColumn> columns;

    public Table(ASTcreate_table_statement table) {
      name = table.getTableName();

      // string primaryKey is like "PRIMARY KEY (group_id_fingerprint ASC, group_id ASC)". the
      // primary_keys are extracted from it
      String primaryKey = table.getPrimaryKey().toString();
      primaryKey = primaryKey.replace("PRIMARY KEY (", "");
      primaryKey = primaryKey.replace(")", "");
      primaryKey = primaryKey.replace("ASC", "");
      primaryKey = primaryKey.replace(" ", "");
      primary_keys = primaryKey.split(",");

      columns =
          table.getColumns().values().stream()
              .filter(c -> !c.isHidden())
              .map(TableColumn::new)
              .collect(Collectors.toList());
    }
  }

  static class TableColumn {
    public String name;
    public String type;

    public TableColumn(ASTcolumn_def column) {
      name = column.getColumnName();
      type = column.getColumnType().toString();
    }
  }

  static class Index {
    public String name;
    public String table;
    public String[] columns;
    public List<String> stored_columns;

    public Index(ASTcreate_index_statement index) {
      name = index.getIndexName();
      table = AstTreeUtils.getChildByType(index, ASTtable.class).toString();
      columns = parseColumnList(AstTreeUtils.getChildByType(index, ASTcolumns.class).toString());
      stored_columns =
          parseStroredColumnList(
              AstTreeUtils.getOptionalChildByType(index, ASTstored_column_list.class));
    }

    public Index(ASTcreate_search_index_statement index) {
      name = index.getName();
      table = AstTreeUtils.getChildByType(index, ASTtable.class).toString();

      String columnsStr = AstTreeUtils.getChildByType(index, ASTtoken_key_list.class).toString();

      ASTpartition_key partitionKey =
          AstTreeUtils.getOptionalChildByType(index, ASTpartition_key.class);
      if (partitionKey != null) {
        // partitionStr is like "PARTITION BY organization_id ASC"
        String partitionStr = partitionKey.toString();
        partitionStr = partitionStr.replace("PARTITION BY ", "");

        // combine with the columnsStr so that it can be commonly processed
        columnsStr += "," + partitionStr;
      }

      columns = parseColumnList(columnsStr);
      stored_columns =
          parseStroredColumnList(
              AstTreeUtils.getOptionalChildByType(index, ASTstored_column_list.class));
    }
  }

  private static String[] parseColumnList(String columnsStr) {
    // columnStr is like "( org_unit_id ASC, name ASC )"
    columnsStr = columnsStr.replace("(", "");
    columnsStr = columnsStr.replace(")", "");
    columnsStr = columnsStr.replace("ASC", "");
    columnsStr = columnsStr.replace("DESC", "");
    columnsStr = columnsStr.replace(" ", "");
    return columnsStr.split(",");
  }

  private static List<String> parseStroredColumnList(ASTstored_column_list storedColumnList) {
    if (storedColumnList == null) {
      return null;
    }

    return storedColumnList.getStoredColumns().stream()
        .map(ASTstored_column::toString)
        .collect(Collectors.toList());
  }
}
