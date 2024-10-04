package com.uber.spannerddl.schema;

import com.google.cloud.solutions.spannerddl.parser.ASTannotation;
import com.google.cloud.solutions.spannerddl.parser.ASTcolumn_def;
import com.google.cloud.solutions.spannerddl.parser.ASTcreate_table_statement;
import com.google.cloud.solutions.spannerddl.parser.Node;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Table contains name, primary_keys and list of columns */
public class Table {
  public String name;
  public Boolean uql_enabled;
  public List<String> primary_keys;
  public List<Column> columns;

  public Table(ASTcreate_table_statement table) {
    name = table.getTableName();
    primary_keys = Util.getKeyColumns(table.getPrimaryKey());
    columns =
        table.getColumns().values().stream()
            .filter(c -> !Column.isTokenListColumn(c))
            .map(Column::new)
            .collect(Collectors.toList());

    // extract annotations
    Map<String, List<Annotation>> annotations = new LinkedHashMap<>();
    boolean hasUql = extractAnnotations(table, annotations);
    if (hasUql) {
      uql_enabled = Boolean.TRUE;
    }

    columns.forEach(c -> c.annotations = annotations.get(c.name));
  }

  // the schema.sql was tagged with annotations. extract the column specific annotations
  private static boolean extractAnnotations(
      ASTcreate_table_statement table, Map<String, List<Annotation>> annotations) {
    // the UQL column annotations are present before the column definition. these are accumulated
    // and then applied to the next column
    List<Annotation> pendingAnnotations = new ArrayList<>();

    // iterate all the table elements
    int totalAnnotations = 0;
    for (int i = 0, count = table.jjtGetNumChildren(); i < count; i++) {
      Node child = table.jjtGetChild(i);

      if (child instanceof ASTannotation) {
        // accumulate the annotations
        ASTannotation columnAnnotation = (ASTannotation) child;
        pendingAnnotations.add(new Annotation(columnAnnotation));

      } else if (child instanceof ASTcolumn_def) {
        annotations.put(((ASTcolumn_def) child).getColumnName(), pendingAnnotations);
        totalAnnotations += pendingAnnotations.size();
        pendingAnnotations = new ArrayList<>();
      }
    }

    return totalAnnotations > 0;
  }
}
