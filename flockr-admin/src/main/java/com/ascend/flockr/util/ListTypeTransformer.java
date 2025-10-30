package com.ascend.flockr.util;

import io.reactivex.functions.BiFunction;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ListTypeTransformer<T, R> implements BiFunction<List<T>, Function<T, R>, List<R>> {

  @Override
  public List<R> apply(List<T> inputList, Function<T, R> elementMapper) {
    return inputList.stream().map(elementMapper).collect(Collectors.toList());
  }
}
