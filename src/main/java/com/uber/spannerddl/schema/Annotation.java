package com.uber.spannerddl.schema;

import com.google.cloud.solutions.spannerddl.parser.ASTannotation;
import java.util.LinkedHashMap;
import java.util.Map;

public class Annotation {
  public String ns;
  public String name;
  public Map<String, String> params;

  public Annotation(ASTannotation annotation) {
    String annotationName = annotation.getName();
    int pos = annotationName.indexOf('.');
    if (pos != -1) {
      ns = annotationName.substring(0, pos);
      name = annotationName.substring(pos + 1);
    } else {
      name = annotationName;
    }

    params = new LinkedHashMap<>();
    annotation.getParams().forEach(p -> params.put(p.getKey(), p.getValue()));
    if (params.isEmpty()) {
      params = null;
    }
  }
}
