package com.ascend.flockr.util.sqlparser;

import com.ascend.flockr.exception.DefinedException;
import com.ascend.flockr.exception.ErrorEntity;
import io.reactivex.Single;
import io.vertx.reactivex.core.Promise;
import io.vertx.reactivex.core.Vertx;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;

@Slf4j
class SqlParser {

  /**
   * complex process could take significant time for very complex queries
   *
   * <p>{@see <a href=https://github.com/JSQLParser/JSqlParser/issues/1981>Complex Parsing
   * Issue</a>}
   */
  static Single<Statement> parseQuery(String query, boolean allowComplexParsing, long timeout) {
    CCJSqlParser parser =
        CCJSqlParserUtil.newParser(query).withAllowComplexParsing(allowComplexParsing);
    return Vertx.currentContext()
        .owner()
        .rxExecuteBlocking(
            (Promise<Statement> promise) -> {
              try {
                Statement statement = (Statement) parser.Statement();
                promise.complete(statement);
              } catch (Exception ex) {
                promise.fail(ex);
              }
            })
        .toSingle()
        .timeout(timeout, TimeUnit.MILLISECONDS)
        .onErrorResumeNext(
            ex -> {
              if (ex instanceof TimeoutException) {
                // interrupt parsing thread, results in ParseException
                parser.interrupted = true;
                return Single.error(new JSQLParserException("Query Parsing Timed Out", ex));
              } else {
                return Single.error(ex);
              }
            });
  }

  static Single<Statement> parseQuery(String query) {
    return parseQuery(query, false, 500)
        .onErrorResumeNext(
            ex -> {
              log.error("Error parsing query. Retrying with allowComplexParsing: true", ex);
              return parseQuery(query, true, 1000);
            });
  }

  static List<String> extractColumnNames(Select selectQuery, Boolean aliasRequired) {
    SelectStatementColumnVisitor visitor = new SelectStatementColumnVisitor(aliasRequired);
    for (SelectItem selectItem : ((PlainSelect) selectQuery.getSelectBody()).getSelectItems()) {
      selectItem.accept(visitor);
    }

    return visitor.columnNames();
  }

  static Single<Select> parseSelectQuery(String query) {
    return parseQuery(query)
        .map(statement -> (Select) statement)
        .onErrorResumeNext(
            ex ->
                Single.error(
                    new DefinedException(ErrorEntity.QUERY_VALIDATION_FAILED, ex.getMessage())));
  }
}
