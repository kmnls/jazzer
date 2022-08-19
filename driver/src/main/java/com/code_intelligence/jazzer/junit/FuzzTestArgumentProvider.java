// Copyright 2022 Code Intelligence GmbH
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.code_intelligence.jazzer.junit;

import com.code_intelligence.jazzer.agent.Agent;
import com.code_intelligence.jazzer.runtime.FuzzedDataProviderImpl;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;

class FuzzTestArgumentProvider implements ArgumentsProvider, AnnotationConsumer<FuzzTest> {
  static {
    System.setProperty("jazzer.is_replayer", "**");
    // Apply all hooks, but no coverage or compare instrumentation.
    System.setProperty("jazzer.instrumentation_excludes", "**");
    //        Agent.premain(null, ByteBuddyAgent.install());
  }

  FuzzTest annotation;

  @Override
  public void accept(FuzzTest annotation) {
    this.annotation = annotation;
  }

  @Override
  public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext)
      throws IOException {
    return walkSeedCorpus(extensionContext.getRequiredTestClass())
        // @ParameterizedTest automatically closes all arguments.
        .map(FuzzedDataProviderImpl::withJavaData)
        .map(Arguments::of);
  }

  private Stream<byte[]> walkSeedCorpus(Class<?> testClass) throws IOException {
    String seedCorpusArg = annotation.seedCorpus();
    String seedCorpusPath =
        seedCorpusArg.isEmpty() ? defaultSeedCorpusPath(testClass) : seedCorpusArg;
    System.err.println(seedCorpusPath);
    URL seedCorpusUrl = testClass.getClassLoader().getResource(seedCorpusPath);
    if (seedCorpusUrl == null) {
      return Stream.of(new byte[] {});
    }
    // TODO: Handle the case where the resource directory is packaged in a JAR.
    if (!seedCorpusUrl.getProtocol().equals("file")) {
      throw new IOException("Unsupported resource protocol: " + seedCorpusUrl);
    }
    return Stream.concat(Stream.of(new byte[] {}),
        Files
            .find(Paths.get(seedCorpusUrl.getPath()), Integer.MAX_VALUE,
                (path, basicFileAttributes)
                    -> !basicFileAttributes.isDirectory(),
                FileVisitOption.FOLLOW_LINKS)
            .map(FuzzTestArgumentProvider::readAllBytesUnchecked));
  }

  private static String defaultSeedCorpusPath(Class<?> testClass) {
    return "/" + testClass.getName().replace('.', '/') + "SeedCorpus";
  }

  private static byte[] readAllBytesUnchecked(Path path) {
    try {
      return Files.readAllBytes(path);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
