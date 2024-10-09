package com.uber.spannerddl.schema;

import com.google.cloud.solutions.spannerddl.parser.ASTcolumn_def;
import java.util.List;

/** Column contains name and type */
public class Column {
  public String name;
  public String type;
  public Boolean is_stored;
  public List<Annotation> annotations;

  public Column(ASTcolumn_def column) {
    name = column.getColumnName();
    type = column.getColumnTypeString();
    if (column.isStored()) {
      is_stored = column.isStored();
    }
  }

  public static boolean isTokenListColumn(ASTcolumn_def column) {
    return column.getColumnTypeString().equalsIgnoreCase("TOKENLIST");
  }
}
