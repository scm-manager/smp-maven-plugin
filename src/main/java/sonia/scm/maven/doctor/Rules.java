package sonia.scm.maven.doctor;

import com.google.common.collect.ImmutableList;
import sonia.scm.maven.doctor.rules.MinCoreVersionRule;
import sonia.scm.maven.doctor.rules.MissingPostInstallRule;
import sonia.scm.maven.doctor.rules.NameRule;
import sonia.scm.maven.doctor.rules.UiPluginsVersionRule;
import sonia.scm.maven.doctor.rules.VersionRule;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public final class Rules implements Iterable<Rule> {

  private final List<Rule> ruleList;

  private Rules(List<Rule> instances) {
    this.ruleList = instances;
  }

  public static Rules of(Rule... rules) {
    return new Rules(ImmutableList.copyOf(rules));
  }

  public static Rules of(Iterable<Rule> rules) {
    return new Rules(ImmutableList.copyOf(rules));
  }

  public static Rules all() {
    return new Rules(ImmutableList.of(
      new NameRule(),
      new VersionRule(),
      new MissingPostInstallRule(),
      new UiPluginsVersionRule(),
      new MinCoreVersionRule()
    ));
  }

  public Results validate(Context context) {
    List<Result> results = ruleList.stream()
      .map(rule -> rule.validate(context))
      .collect(Collectors.toList());
    return new Results(results);
  }

  @Override
  public Iterator<Rule> iterator() {
    return ruleList.iterator();
  }
}
