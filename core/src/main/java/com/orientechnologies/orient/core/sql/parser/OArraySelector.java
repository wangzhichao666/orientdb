/* Generated By:JJTree: Do not edit this line. OArraySelector.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.sql.executor.OResult;

import java.lang.reflect.Array;
import java.util.*;

public class OArraySelector extends SimpleNode {

  protected ORid            rid;
  protected OInputParameter inputParam;
  protected OExpression     expression;
  protected OInteger        integer;

  public OArraySelector(int id) {
    super(id);
  }

  public OArraySelector(OrientSql p, int id) {
    super(p, id);
  }

  /**
   * Accept the visitor.
   **/
  public Object jjtAccept(OrientSqlVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    if (rid != null) {
      rid.toString(params, builder);
    } else if (inputParam != null) {
      inputParam.toString(params, builder);
    } else if (expression != null) {
      expression.toString(params, builder);
    } else if (integer != null) {
      integer.toString(params, builder);
    }
  }

  public Integer getValue(OIdentifiable iCurrentRecord, Object iResult, OCommandContext ctx) {
    Object result = null;
    if (inputParam != null) {
      result = inputParam.getValue(ctx.getInputParameters());
    } else if (expression != null) {
      result = expression.execute(iCurrentRecord, ctx);
    } else if (integer != null) {
      result = integer;
    }

    if (result == null) {
      return null;
    }
    if (result instanceof Number) {
      return ((Number) result).intValue();
    }
    return null;
  }

  public Object getValue(OResult iCurrentRecord, Object iResult, OCommandContext ctx) {
    Object result = null;
    if (inputParam != null) {
      result = inputParam.getValue(ctx.getInputParameters());
    } else if (expression != null) {
      result = expression.execute(iCurrentRecord, ctx);
    } else if (integer != null) {
      result = integer;
    }

    if (result == null) {
      return null;
    }
    if (result instanceof Number) {
      return ((Number) result).intValue();
    }
    return result;
  }

  public boolean needsAliases(Set<String> aliases) {
    if (expression != null) {
      return expression.needsAliases(aliases);
    }
    return false;
  }

  public OArraySelector copy() {
    OArraySelector result = new OArraySelector(-1);

    result.rid = rid == null ? null : rid.copy();
    result.inputParam = inputParam == null ? null : inputParam.copy();
    result.expression = expression == null ? null : expression.copy();
    result.integer = integer == null ? null : integer.copy();

    return result;
  }

  @Override public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    OArraySelector that = (OArraySelector) o;

    if (rid != null ? !rid.equals(that.rid) : that.rid != null)
      return false;
    if (inputParam != null ? !inputParam.equals(that.inputParam) : that.inputParam != null)
      return false;
    if (expression != null ? !expression.equals(that.expression) : that.expression != null)
      return false;
    if (integer != null ? !integer.equals(that.integer) : that.integer != null)
      return false;

    return true;
  }

  @Override public int hashCode() {
    int result = rid != null ? rid.hashCode() : 0;
    result = 31 * result + (inputParam != null ? inputParam.hashCode() : 0);
    result = 31 * result + (expression != null ? expression.hashCode() : 0);
    result = 31 * result + (integer != null ? integer.hashCode() : 0);
    return result;
  }

  public void extractSubQueries(SubQueryCollector collector) {
    if (expression != null) {
      expression.extractSubQueries(collector);
    }
  }

  public boolean refersToParent() {
    if (expression != null && expression.refersToParent()) {
      return true;
    }
    return false;
  }

  public void setValue(OResult currentRecord, Object target, Object value, OCommandContext ctx) {
    Object idx = null;
    if (this.rid != null) {
      idx = this.rid.toRecordId(currentRecord, ctx);
    } else if (inputParam != null) {
      idx = inputParam.getValue(ctx.getInputParameters());
    } else if (expression != null) {
      idx = expression.execute(currentRecord, ctx);
    } else if (integer != null) {
      idx = integer.getValue();
    }

    if (target instanceof Set && idx instanceof Number) {
      setValue((Set) target, ((Number) idx).intValue(), value, ctx);
    } else if (target instanceof List && idx instanceof Number) {
      setValue((List) target, ((Number) idx).intValue(), value, ctx);
    } else if (target instanceof Map) {
      setValue((Map) target, idx, value, ctx);
    } else if (target.getClass().isArray() && idx instanceof Number) {
      setArrayValue(target, ((Number) idx).intValue(), value, ctx);
    }
  }

  public void setValue(List target, int idx, Object value, OCommandContext ctx) {
    int originalSize = target.size();
    for (int i = originalSize; i <= idx; i++) {
      if (i >= originalSize) {
        target.add(null);
      }
    }
    target.set(idx, value);
  }

  public void setValue(Set target, int idx, Object value, OCommandContext ctx) {
    Set result = new LinkedHashSet<>();
    int originalSize = target.size();
    int max = Math.max(idx, originalSize - 1);
    Iterator targetIterator = target.iterator();
    for (int i = 0; i <= max; i++) {
      Object next = null;
      if (targetIterator.hasNext()) {
        next = targetIterator.next();
      }
      if (i == idx) {
        result.add(value);
      } else if (i < originalSize) {
        result.add(next);
      } else {
        result.add(null);
      }
      target.clear();
      target.addAll(result);
    }
  }

  public void setValue(Map target, Object idx, Object value, OCommandContext ctx) {
    target.put(idx, value);
  }

  private void setArrayValue(Object target, int idx, Object value, OCommandContext ctx) {
    if (idx >= 0 && idx < Array.getLength(target)) {
      Array.set(target, idx, value);
    }
  }
}
/* JavaCC - OriginalChecksum=f87a5543b1dad0fb5f6828a0663a7c9e (do not edit this line) */
