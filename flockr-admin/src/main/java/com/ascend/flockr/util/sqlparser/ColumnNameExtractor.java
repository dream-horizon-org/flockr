package com.ascend.flockr.util.sqlparser;

import com.ascend.flockr.exception.DefinedException;
import com.ascend.flockr.exception.ErrorEntity;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.conditional.XorExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.SubSelect;

@Slf4j
public class ColumnNameExtractor implements ExpressionVisitor {

  private final Boolean expressionAsString;
  private final List<String> columnNames;

  public ColumnNameExtractor(Boolean expressionAsString, List<String> columnNames) {
    this.expressionAsString = expressionAsString;
    this.columnNames = columnNames;
  }

  @Override
  public void visit(Column expression) {
    columnNames.add(expression.getColumnName());
  }

  @Override
  public void visit(BitwiseRightShift expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(BitwiseLeftShift expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(NullValue expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(Function expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(SignedExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(JdbcParameter expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(JdbcNamedParameter expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(DoubleValue expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(LongValue expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(HexValue expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(DateValue expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(TimeValue expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(TimestampValue expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(Parenthesis expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(StringValue expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(Addition expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(Division expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(IntegerDivision expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(Multiplication expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(Subtraction expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(AndExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(OrExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(XorExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(Between expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(EqualsTo expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(GreaterThan expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(GreaterThanEquals expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(InExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(FullTextSearch expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(IsNullExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(IsBooleanExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(LikeExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(MinorThan expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(MinorThanEquals expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(NotEqualsTo expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(SubSelect expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(CaseExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(WhenClause expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(ExistsExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(AnyComparisonExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(Concat expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(Matches expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(BitwiseAnd expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(BitwiseOr expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(BitwiseXor expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(CastExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(TryCastExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(Modulo expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(AnalyticExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(ExtractExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(IntervalExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(OracleHierarchicalExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(RegExpMatchOperator expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(JsonExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(JsonOperator expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(RegExpMySQLOperator expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(UserVariable expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(NumericBind expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(KeepExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(MySQLGroupConcat expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(ValueListExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(RowConstructor expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(RowGetExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(OracleHint expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(TimeKeyExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(DateTimeLiteralExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(NotExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(NextValExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(CollateExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(SimilarToExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(ArrayExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(ArrayConstructor expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(VariableAssignment expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(XMLSerializeExpr expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(TimezoneExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(JsonAggregateFunction expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(JsonFunction expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(ConnectByRootOperator expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(OracleNamedFunctionParameter expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(AllColumns expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(AllTableColumns expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(AllValue expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(IsDistinctExpression expression) {
    visitExpression(expression);
  }

  @Override
  public void visit(GeometryDistance expression) {
    visitExpression(expression);
  }

  private void visitExpression(Expression expression) {
    if (expressionAsString) {
      columnNames.add(expression.toString());
      return;
    }

    String message = "COLUMN " + expression.getClass().getSimpleName() + " " + expression;
    log.error(message + " NOT SUPPORTED");
    throw new DefinedException(ErrorEntity.UNSUPPORTED_SELECT_CLAUSE, message);
  }
}
