package com.ascend.flockr.util.sqlparser;

import com.ascend.flockr.exception.DefinedException;
import com.ascend.flockr.exception.ErrorEntity;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
public class SQLStatementUtil {

  public static Completable validate(String query) {
    return SqlParser.parseSelectQuery(query).ignoreElement();
  }

  public static Single<Pair<List<String>, Select>> extractColumnsAndSelectQuery(String query) {
    return SqlParser.parseSelectQuery(query)
        .filter(selectQuery -> selectQuery.getSelectBody() instanceof PlainSelect)
        .switchIfEmpty(
            Single.error(new DefinedException(ErrorEntity.SINGLE_SELECT_CLAUSE_VALIDATION)))
        .filter(
            selectQuery -> {
              FromItem fromItem = ((PlainSelect) selectQuery.getSelectBody()).getFromItem();
              return fromItem != null;
            })
        .switchIfEmpty(
            Single.error(new DefinedException(ErrorEntity.SELECT_CLAUSE_FROM_VALIDATION)))
        .map(
            selectQuery -> {
              List<String> columnNames = SqlParser.extractColumnNames(selectQuery, Boolean.TRUE);
              return Pair.of(columnNames, selectQuery);
            });
  }

  public static Single<Select> validateSingleColumnQueryAndGetSelectQuery(
      String query, String columnName) {
    return SQLStatementUtil.extractColumnsAndSelectQuery(query)
        .filter(columnsLimitPair -> columnsLimitPair.getLeft().size() == 1)
        .switchIfEmpty(
            Single.error(
                new DefinedException(
                    ErrorEntity.SELECT_CLAUSE_SINGLE_COLUMN_VALIDATION, columnName)))
        .filter(columnsLimitPair -> columnsLimitPair.getLeft().get(0).equals(columnName))
        .switchIfEmpty(
            Single.error(
                new DefinedException(
                    ErrorEntity.SELECT_CLAUSE_SINGLE_COLUMN_NAME_VALIDATION, columnName)))
        .map(Pair::getRight);
  }

  public static PlainSelect addLimit(PlainSelect query, Long limit) {
    Limit limitClause = new Limit();
    limitClause.setRowCount(new LongValue(limit));
    query.setLimit(limitClause);
    return query;
  }
}
