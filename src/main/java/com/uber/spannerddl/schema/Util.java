package com.uber.spannerddl.schema;

import com.google.cloud.solutions.spannerddl.diff.AstTreeUtils;
import com.google.cloud.solutions.spannerddl.parser.ASTkey_part;
import com.google.cloud.solutions.spannerddl.parser.ASTpath;
import com.google.cloud.solutions.spannerddl.parser.ASTstored_column;
import com.google.cloud.solutions.spannerddl.parser.ASTstored_column_list;
import com.google.cloud.solutions.spannerddl.parser.Node;
import java.util.List;
import java.util.stream.Collectors;

public class Util {
  /**
   * Get the list of columns names for a table or an index
   *
   * @param node Table or Index AST node
   * @return List of column names
   */
  public static List<String> getTokenColumns(Node node) {
    if (node == null) {
      return null;
    }
    List<ASTpath> astKeys = AstTreeUtils.getChildrenAssertType(node, ASTpath.class);
    return astKeys.stream().map(ASTpath::toString).collect(Collectors.toList());
  }

  /**
   * Get the list of columns names for a table or an index
   *
   * @param node Table or Index AST node
   * @return List of column names
   */
  public static List<String> getKeyColumns(Node node) {
    if (node == null) {
      return null;
    }
    List<ASTkey_part> astKeys = AstTreeUtils.getChildrenAssertType(node, ASTkey_part.class);
    return astKeys.stream().map(ASTkey_part::getKeyPath).collect(Collectors.toList());
  }

  /**
   * Get list of stored columns for an INDEX or SEARCH INDEX
   *
   * @param node The index or search index AST node
   * @return names of the stored columns
   */
  static List<String> getStoredColumns(Node node) {
    if (node == null) {
      return null;
    }

    ASTstored_column_list astStoredColumns =
        AstTreeUtils.getOptionalChildByType(node, ASTstored_column_list.class);
    if (astStoredColumns == null) {
      return null;
    }

    return astStoredColumns.getStoredColumns().stream()
        .map(ASTstored_column::toString)
        .collect(Collectors.toList());
  }
}
