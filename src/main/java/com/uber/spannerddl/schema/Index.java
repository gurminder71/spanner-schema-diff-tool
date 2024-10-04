package com.uber.spannerddl.schema;

import com.google.cloud.solutions.spannerddl.diff.AstTreeUtils;
import com.google.cloud.solutions.spannerddl.parser.ASTcolumns;
import com.google.cloud.solutions.spannerddl.parser.ASTcreate_index_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTcreate_search_index_statement;
import com.google.cloud.solutions.spannerddl.parser.ASTpartition_key;
import com.google.cloud.solutions.spannerddl.parser.ASTtable;
import com.google.cloud.solutions.spannerddl.parser.ASTtoken_key_list;
import java.util.List;

/** Index contains name, table, columns and stored columns */
public class Index {
  public String name;
  public String table;
  public List<String> columns;
  public List<String> stored_columns;

  /** Constructor for regular index */
  public Index(ASTcreate_index_statement index) {
    name = index.getIndexName();
    table = AstTreeUtils.getChildByType(index, ASTtable.class).toString();
    columns = Util.getKeyColumns(AstTreeUtils.getChildByType(index, ASTcolumns.class));
    stored_columns = Util.getStoredColumns(index);
  }

  /* Constructor for search index */
  public Index(ASTcreate_search_index_statement index) {
    name = index.getName();
    table = AstTreeUtils.getChildByType(index, ASTtable.class).toString();
    columns = Util.getTokenColumns(AstTreeUtils.getChildByType(index, ASTtoken_key_list.class));

    // combine partition columns also in the columns
    List<String> partitionColumns =
        Util.getKeyColumns(AstTreeUtils.getOptionalChildByType(index, ASTpartition_key.class));
    if (partitionColumns != null) {
      columns.addAll(partitionColumns);
    }

    stored_columns = Util.getStoredColumns(index);
  }
}
