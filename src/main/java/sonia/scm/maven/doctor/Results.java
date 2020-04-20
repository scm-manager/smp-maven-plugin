package sonia.scm.maven.doctor;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class Results implements Iterable<Result> {
  private final List<Result> resultList;

  Results(List<Result> resultList) {
    this.resultList = resultList;
  }

  public boolean hasError() {
    return resultList.stream()
      .anyMatch(result -> result.getType() == Result.Type.ERROR);
  }

  public Stream<Result> stream() {
    return resultList.stream();
  }

  @Override
  public Iterator<Result> iterator() {
    return resultList.iterator();
  }
}
