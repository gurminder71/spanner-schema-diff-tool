package com.uber.spannerddl.schema;

import com.google.cloud.solutions.spannerddl.diff.DatabaseDefinition;
import com.google.cloud.solutions.spannerddl.diff.DdlDiff;
import com.google.cloud.solutions.spannerddl.diff.DdlDiffException;
import com.google.cloud.solutions.spannerddl.parser.ASTddl_statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
 * Spanner DDL schema is defined using Google SQL. Since parsing of the SQL statements is complex
 * and already handled by this diff generation in this JAR, the information is extracted from parsed
 * schema and available in Schema. The Schema can be used directly by callers. Or it can be saved as
 * YAML so that downstream can use the information as YAML can be easily read than SQL.
 */
public class Schema {
  public List<Table> tables;
  public List<Index> indexes;

  /**
   * Parse the Spanner schema DDL and extract the tables and indexes into Schema
   *
   * @param ddl Spanner schema DDL
   */
  public Schema(String ddl) throws DdlDiffException {
    List<ASTddl_statement> statements = DdlDiff.parseDdl(ddl, true);
    DatabaseDefinition db = DatabaseDefinition.create(statements);

    tables =
        db.tablesInCreationOrder().values().stream().map(Table::new).collect(Collectors.toList());
    indexes = db.indexes().values().stream().map(Index::new).collect(Collectors.toList());
    db.searchIndexes().values().stream().map(Index::new).forEach(indexes::add);
  }

  /**
   * Marshall the Schema as YAML that can be read by downstream. One use case is to generated UQL
   *
   * @return YAML string
   */
  public String writeToYaml() {
    Representer representer =
        new Representer(new DumperOptions()) {
          @Override
          protected NodeTuple representJavaBeanProperty(
              Object javaBean, Property property, Object propertyValue, Tag customTag) {
            // if value of property is null, ignore it.
            if (propertyValue == null) {
              return null;
            }

            // do not output empty annotations
            if (javaBean.getClass() == Column.class && property.getName().equals("annotations")) {
              if (((ArrayList<?>) propertyValue).isEmpty()) {
                return null;
              }
            }
            return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
          }
        };

    representer.setPropertyUtils(
        new PropertyUtils() {
          @Override
          protected Set<Property> createPropertySet(Class<?> type, BeanAccess bAccess) {
            // do not sort bean properties
            return new LinkedHashSet<>(getPropertiesMap(type, bAccess).values());
          }
        });

    Yaml yaml = new Yaml(representer);
    return yaml.dumpAsMap(this);
  }
}
