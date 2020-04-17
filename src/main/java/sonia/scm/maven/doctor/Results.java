package sonia.scm.maven.doctor;

import java.util.Iterator;
import java.util.List;

public class Results implements Iterable<Result> {
  private final List<Result> resultList;

  Results(List<Result> resultList) {
    this.resultList = resultList;
  }

  public boolean hasError() {
    return resultList.stream()
      .anyMatch(result -> result.getType() == Result.Type.ERROR);
  }

  @Override
  public Iterator<Result> iterator() {
    return resultList.iterator();
  }
}
