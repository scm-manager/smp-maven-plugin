package sonia.scm.maven.doctor.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.model.FileSet;
import sonia.scm.maven.doctor.Context;
import sonia.scm.maven.doctor.Result;
import sonia.scm.maven.doctor.Rule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Optional.empty;
import static java.util.Optional.of;

public class ValidPluginsJsonRule implements Rule {
  @Override
  public Result validate(Context context) {
    return context.getProject().getResources()
      .stream()
      .map(FileSet::getDirectory)
      .map(Paths::get)
      .filter(Files::isDirectory)
      .map(resourcesDirectory -> resourcesDirectory.resolve("locales"))
      .filter(Files::isDirectory)
      .flatMap(this::listLocales)
      .map(countryLocaleDirectory -> countryLocaleDirectory.resolve("plugins.json"))
      .filter(Files::isRegularFile)
      .map(this::validateJson)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .findFirst()
      .orElse(Result.ok("locale resources contain valid json (if present)"));
  }

  private Stream<Path> listLocales(Path localsDirectory) {
    try {
      return Files.list(localsDirectory);
    } catch (IOException e) {
      e.printStackTrace();
      return Stream.empty();
    }
  }

  private Optional<Result> validateJson(Path pluginsJsonFile) {
    try {
      new ObjectMapper().readTree(pluginsJsonFile.toFile());
      return empty();
    } catch (IOException e) {
      return of(Result.error(pluginsJsonFile + " contains invalid json: " + e.getLocalizedMessage()).build());
    }
  }
}
