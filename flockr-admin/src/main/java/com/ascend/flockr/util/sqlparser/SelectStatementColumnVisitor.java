package com.ascend.flockr.util.sqlparser;

import com.ascend.flockr.exception.DefinedException;
import com.ascend.flockr.exception.ErrorEntity;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitor;

@Slf4j
public class SelectStatementColumnVisitor implements SelectItemVisitor {

  private final Boolean aliasRequired;
  private final List<String> columnNames = new ArrayList<>();
  private final ColumnNameExtractor columnNameExtractor;

  public SelectStatementColumnVisitor() {
    this.aliasRequired = Boolean.TRUE;
    this.columnNameExtractor = new ColumnNameExtractor(Boolean.FALSE, this.columnNames);
  }

  public SelectStatementColumnVisitor(Boolean aliasRequired) {
    this.aliasRequired = aliasRequired;
    this.columnNameExtractor = new ColumnNameExtractor(!aliasRequired, this.columnNames);
  }

  public List<String> columnNames() {
    return this.columnNames;
  }

  @Override
  public void visit(SelectExpressionItem item) {
    if (item.getAlias() != null) {
      columnNames.add(item.getAlias().getName());
      return;
    }

    item.getExpression().accept(columnNameExtractor);
  }

  @Override
  public void visit(AllColumns columns) {
    String message =
        "SELECT " + columns.getClass().getSimpleName() + " " + columns + " NOT SUPPORTED";
    log.error(message);
    throw new DefinedException(ErrorEntity.UNSUPPORTED_ALL_SELECT, message);
  }

  @Override
  public void visit(AllTableColumns columns) {
    String message =
        "SELECT " + columns.getClass().getSimpleName() + " " + columns + " NOT SUPPORTED";
    log.error(message);
    throw new DefinedException(ErrorEntity.UNSUPPORTED_ALL_SELECT, message);
  }
}
