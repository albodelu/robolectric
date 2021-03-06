package org.robolectric.annotation.processing;

import static com.google.common.truth.Truth.assertThat;
import static org.truth0.Truth.ASSERT;
import static com.google.testing.compile.JavaFileObjects.*;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static org.robolectric.annotation.processing.RobolectricProcessor.PACKAGE_OPT;
import static org.robolectric.annotation.processing.RobolectricProcessor.SHOULD_INSTRUMENT_PKG_OPT;
import static org.robolectric.annotation.processing.validator.Utils.ROBO_SOURCE;
import static org.robolectric.annotation.processing.validator.Utils.SHADOW_PROVIDER_SOURCE;
import static org.robolectric.annotation.processing.validator.Utils.SHADOW_EXTRACTOR_SOURCE;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class RobolectricProcessorTest {
  private static final Map<String,String> DEFAULT_OPTS = new HashMap<>();

  static {
    DEFAULT_OPTS.put(PACKAGE_OPT, "org.robolectric");
  }

  @Test
  public void robolectricProcessor_supportsPackageOption() {
    ASSERT.that(new RobolectricProcessor().getSupportedOptions()).contains(PACKAGE_OPT);
  }

  @Test
  public void robolectricProcessor_supportsShouldInstrumentPackageOption() {
    ASSERT.that(
        new RobolectricProcessor().getSupportedOptions()).contains(SHOULD_INSTRUMENT_PKG_OPT);
  }

  @Test
  public void unannotatedSource_shouldCompile() {
    ASSERT.about(javaSources())
      .that(ImmutableList.of(
          ROBO_SOURCE,
          SHADOW_PROVIDER_SOURCE,
          SHADOW_EXTRACTOR_SOURCE,
          forSourceString("HelloWorld", "final class HelloWorld {}")))
      .processedWith(new RobolectricProcessor())
      .compilesWithoutError();
    //.and().generatesNoSources(); Should add this assertion onces
    // it becomes available in compile-testing
  }

  @Test
  public void generatedFile_shouldHandleInnerClassCollisions() {
    // Because the Generated annotation has a retention of "source", it can't
    // be tested by a unit test - must run a source-level test.
    ASSERT.about(javaSources())
      .that(ImmutableList.of(
          ROBO_SOURCE,
          SHADOW_PROVIDER_SOURCE,
          SHADOW_EXTRACTOR_SOURCE,
          forResource("org/robolectric/annotation/processing/shadows/ShadowDummy.java"),
          forResource("org/robolectric/annotation/processing/shadows/ShadowOuterDummy.java"),
          forResource("org/robolectric/annotation/processing/shadows/ShadowUniqueDummy.java")))
      .processedWith(new RobolectricProcessor(DEFAULT_OPTS))
      .compilesWithoutError()
      .and()
      .generatesSources(forResource("org/robolectric/Robolectric_InnerClassCollision.java"));
  }

  @Test
  public void generatedFile_shouldSkipNonPublicClasses() {
    ASSERT.about(javaSources())
      .that(ImmutableList.of(
          ROBO_SOURCE,
          SHADOW_PROVIDER_SOURCE,
          SHADOW_EXTRACTOR_SOURCE,
          forResource("org/robolectric/annotation/processing/shadows/ShadowPrivate.java"),
          forResource("org/robolectric/annotation/processing/shadows/ShadowOuterDummy2.java"),
          forResource("org/robolectric/annotation/processing/shadows/ShadowDummy.java")))
      .processedWith(new RobolectricProcessor(DEFAULT_OPTS))
      .compilesWithoutError()
      .and()
      .generatesSources(forResource("org/robolectric/Robolectric_HiddenClasses.java"));
  }

  @Test
  public void generatedFile_shouldHandleAnythingShadows() {
    ASSERT.about(javaSources())
      .that(ImmutableList.of(
          ROBO_SOURCE,
          SHADOW_PROVIDER_SOURCE,
          SHADOW_EXTRACTOR_SOURCE,
          forResource("org/robolectric/annotation/processing/shadows/ShadowAnything.java"),
          forResource("org/robolectric/annotation/processing/shadows/ShadowDummy.java")))
      .processedWith(new RobolectricProcessor(DEFAULT_OPTS))
      .compilesWithoutError()
      .and()
      .generatesSources(forResource("org/robolectric/Robolectric_Anything.java"));
  }

  @Test
  public void generatedFile_shouldHandleClassNameOnlyShadows() {
    ASSERT.about(javaSources())
      .that(ImmutableList.of(
          SHADOW_PROVIDER_SOURCE,
          SHADOW_EXTRACTOR_SOURCE,
          forResource("org/robolectric/annotation/processing/shadows/ShadowClassNameOnly.java"),
          forResource("org/robolectric/annotation/processing/shadows/ShadowDummy.java")))
      .processedWith(new RobolectricProcessor(DEFAULT_OPTS))
      .compilesWithoutError()
      .and()
      .generatesSources(forResource("org/robolectric/Robolectric_ClassNameOnly.java"));
  }

  @Test
  public void generatedFile_shouldNotGenerateShadowOfMethodsForExcludedClasses() {
    ASSERT.about(javaSources())
        .that(ImmutableList.of(
            SHADOW_PROVIDER_SOURCE,
            SHADOW_EXTRACTOR_SOURCE,
            forResource("org/robolectric/annotation/processing/shadows/ShadowExcludedFromAndroidSdk.java")))
        .processedWith(new RobolectricProcessor(DEFAULT_OPTS))
        .compilesWithoutError()
        .and()
        .generatesSources(forResource("org/robolectric/Robolectric_NoExcludedTypes.java"));
  }

  @Test
  public void generatedFile_shouldUseSpecifiedPackage() throws IOException {
    StringBuffer expected = new StringBuffer();
    BufferedReader reader = new BufferedReader(new InputStreamReader(RobolectricProcessorTest.class.getClassLoader().getResourceAsStream("org/robolectric/Robolectric_ClassNameOnly.java")));

    String line;
    while ((line = reader.readLine()) != null) {
      expected.append(line).append("\n");
    }
    line = expected.toString();
    line = line.replace("package org.robolectric", "package my.test.pkg");

    Map<String,String> opts = new HashMap<>();
    opts.put(PACKAGE_OPT, "my.test.pkg");

    ASSERT.about(javaSources())
      .that(ImmutableList.of(
          SHADOW_PROVIDER_SOURCE,
          SHADOW_EXTRACTOR_SOURCE,
          forResource("org/robolectric/annotation/processing/shadows/ShadowClassNameOnly.java"),
          forResource("org/robolectric/annotation/processing/shadows/ShadowDummy.java")))
      .processedWith(new RobolectricProcessor(opts))
      .compilesWithoutError()
      .and()
      .generatesSources(forSourceString("Shadows", line));
  }

  @Test
  public void shouldGenerateMetaInfServicesFile() {
    ASSERT.about(javaSources())
        .that(ImmutableList.of(
            SHADOW_PROVIDER_SOURCE,
            SHADOW_EXTRACTOR_SOURCE,
            forResource("org/robolectric/annotation/processing/shadows/ShadowClassNameOnly.java"),
            forResource("org/robolectric/annotation/processing/shadows/ShadowDummy.java")))
        .processedWith(new RobolectricProcessor(DEFAULT_OPTS))
        .compilesWithoutError()
        .and()
        .generatesFiles(forResource("META-INF/services/org.robolectric.internal.ShadowProvider"));
  }

  @Test
  public void shouldGracefullyHandleUnrecognisedAnnotation() {
    ASSERT.about(javaSources())
      .that(ImmutableList.of(
          ROBO_SOURCE,
          SHADOW_PROVIDER_SOURCE,
          SHADOW_EXTRACTOR_SOURCE,
          forResource("org/robolectric/annotation/TestWithUnrecognizedAnnotation.java")))
      .processedWith(new RobolectricProcessor())
      .compilesWithoutError();
  }

  @Test
  public void shouldGracefullyHandleNoAnythingClass_withNoRealObject() {
    ASSERT.about(javaSource())
      .that(forResource("org/robolectric/annotation/processing/shadows/ShadowAnything.java"))
      .processedWith(new RobolectricProcessor())
      .failsToCompile();
  }

  @Test
  public void shouldGracefullyHandleNoAnythingClass_withFoundOnImplementsAnnotation() {
    ASSERT.about(javaSources())
      .that(ImmutableList.of(
          SHADOW_PROVIDER_SOURCE,
          SHADOW_EXTRACTOR_SOURCE,
          forResource("org/robolectric/annotation/processing/shadows/ShadowRealObjectWithCorrectAnything.java")))
      .processedWith(new RobolectricProcessor())
      .failsToCompile();
  }

  @Test
  public void shouldGenerateGenericShadowOf() {
    ASSERT.about(javaSources())
      .that(ImmutableList.of(
          ROBO_SOURCE,
          SHADOW_PROVIDER_SOURCE,
          SHADOW_EXTRACTOR_SOURCE,
          forResource("org/robolectric/annotation/processing/shadows/ShadowDummy.java"),
          forResource("org/robolectric/annotation/processing/shadows/ShadowParameterizedDummy.java")))
      .processedWith(new RobolectricProcessor(DEFAULT_OPTS))
      .compilesWithoutError()
      .and()
      .generatesSources(forResource("org/robolectric/Robolectric_Parameterized.java"));
  }

  @Test
  public void generatedShadowProvider_canConfigureInstrumentingPackages() {
    Map<String, String> options = new HashMap<>();
    options.put(PACKAGE_OPT, "org.robolectric");
    options.put(SHOULD_INSTRUMENT_PKG_OPT, "false");

    ASSERT.about(javaSources())
    .that(ImmutableList.of(
        ROBO_SOURCE,
        SHADOW_PROVIDER_SOURCE,
        SHADOW_EXTRACTOR_SOURCE,
        forResource("org/robolectric/annotation/processing/shadows/ShadowAnything.java"),
        forResource("org/robolectric/annotation/processing/shadows/ShadowDummy.java")))
    .processedWith(new RobolectricProcessor(options))
    .compilesWithoutError()
    .and()
    .generatesSources(forResource("org/robolectric/Robolectric_EmptyProvidedPackageNames.java"));
  }

  @Test
  public void shouldGenerateJavadocJson() throws Exception {
    ASSERT.about(javaSources())
        .that(ImmutableList.of(
            ROBO_SOURCE,
            forResource("org/robolectric/annotation/processing/shadows/DocumentedObjectShadow.java")))
        .processedWith(new RobolectricProcessor())
        .compilesWithoutError();
    JsonParser jsonParser = new JsonParser();
    String jsonFile = "build/docs/json/org.robolectric.Robolectric.DocumentedObject.json";
    JsonElement json = jsonParser.parse(new BufferedReader(new FileReader(jsonFile)));
    assertThat(((JsonObject) json).get("documentation").getAsString())
        .isEqualTo("Robolectric Javadoc goes here!\n");

    // must list imported classes, including inner classes...
    assertThat(((JsonObject) json).get("imports").getAsJsonArray())
        .containsExactly(
            new JsonPrimitive("org.robolectric.Robolectric"),
            new JsonPrimitive("org.robolectric.annotation.Implementation"),
            new JsonPrimitive("org.robolectric.annotation.Implements"),
            new JsonPrimitive("org.robolectric.annotation.Resetter"),
            new JsonPrimitive("java.util.Map"),
            new JsonPrimitive("org.robolectric.annotation.processing.shadows.DocumentedObjectShadow.SomeEnum")
        );
  }
}
