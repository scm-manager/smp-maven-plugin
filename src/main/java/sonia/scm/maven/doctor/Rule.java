package sonia.scm.maven.doctor;

public interface Rule {

  Result validate(Context context);

}
