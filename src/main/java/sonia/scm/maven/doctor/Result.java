package sonia.scm.maven.doctor;

import javax.annotation.Nullable;

public class Result {

  public enum Type {
    OK, WARN, ERROR;
  }

  public static Result ok(String message) {
    return new Result(Type.OK, message, null);
  }

  public static ResultBuilder warn(String message) {
    return new ResultBuilder(Type.WARN, message);
  }

  public static ResultBuilder error(String message) {
    return new ResultBuilder(Type.ERROR, message);
  }

  private final Type type;
  private final String message;
  private final Runnable fix;

  public Result(Type type, String message, @Nullable Runnable fix) {
    this.type = type;
    this.message = message;
    this.fix = fix;
  }

  public boolean isFixable() {
    return fix != null;
  }

  public Type getType() {
    return type;
  }

  public String getMessage() {
    return message;
  }

  public void fix() {
    if (fix != null) {
      fix.run();
    }
  }

  public static class ResultBuilder {

    private final Type type;
    private final String message;
    private Runnable runnable;

    public ResultBuilder(Type type, String message) {
      this.type = type;
      this.message = message;
    }

    public ResultBuilder withFix(Runnable runnable) {
      this.runnable = runnable;
      return this;
    }

    public Result build() {
      return new Result(type, message, runnable);
    }
  }
}
