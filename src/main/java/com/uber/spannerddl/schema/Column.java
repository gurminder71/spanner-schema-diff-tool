package com.uber.spannerddl.schema;

import com.google.cloud.solutions.spannerddl.parser.ASTcolumn_def;
import java.util.List;

/** Column contains name and type */
public class Column {
  public String name;
  public String type;
  public List<Annotation> annotations;

  public Column(ASTcolumn_def column) {
    name = column.getColumnName();
    type = column.getColumnTypeString();
  }
}
