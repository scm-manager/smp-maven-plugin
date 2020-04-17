package sonia.scm.maven.doctor.rules;

import com.google.common.collect.ImmutableList;
import sonia.scm.maven.doctor.Rule;

import java.util.List;

public final class Rules {

  private static final List<Rule> INSTANCES = ImmutableList.of(
    new NameRule(),
    new VersionRule(),
    new MissingPostInstallRule(),
    new UiPluginsVersionRule()
  );

  private Rules() {}

  public static Iterable<Rule> get() {
    return INSTANCES;
  }

}
