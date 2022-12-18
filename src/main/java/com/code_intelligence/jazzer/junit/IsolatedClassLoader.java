/*
 * Copyright 2022 Code Intelligence GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.code_intelligence.jazzer.junit;

import com.code_intelligence.jazzer.utils.UnsafeProvider;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import sun.misc.Unsafe;

public class IsolatedClassLoader extends URLClassLoader {
  private static final Map<ClassLoader, IsolatedClassLoader> cache =
      Collections.synchronizedMap(new WeakHashMap<>());
  private static List<String> preservedClassNamePrefixes = new ArrayList<>();

  private final ClassLoader originalClassLoader;

  private IsolatedClassLoader(ClassLoader original) {
    super(getClassLoaderUrls(original), original.getParent());
    this.originalClassLoader = original;
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    for (String prefix : preservedClassNamePrefixes) {
      if (name.startsWith(prefix)) {
        return Class.forName(name, false, originalClassLoader);
      }
    }
    return super.loadClass(name);
  }

  public static void setPreservedClassNamePrefixes(List<String> prefixes) {
    preservedClassNamePrefixes = Collections.unmodifiableList(new ArrayList<>(prefixes));
  }

  public static AutoCloseable install() {
    ClassLoader original = Thread.currentThread().getContextClassLoader();
    IsolatedClassLoader isolated = cache.computeIfAbsent(original, IsolatedClassLoader::create);
    Thread.currentThread().setContextClassLoader(isolated);
    return () -> Thread.currentThread().setContextClassLoader(original);
  }

  public static boolean isInstalled() {
    return Thread.currentThread().getContextClassLoader().getClass().getName().equals(
        IsolatedClassLoader.class.getName());
  }

  private static IsolatedClassLoader create(ClassLoader original) {
    if (original instanceof IsolatedClassLoader) {
      throw new IllegalStateException("IsolatedClassLoader is already installed");
    }
    return new IsolatedClassLoader(original);
  }

  // Based on
  // https://github.com/bazelbuild/bazel/blob/658ba15d9f37969edfaae507d267ee3aaba8b44e/src/java_tools/junitrunner/java/com/google/testing/coverage/JacocoCoverageRunner.java#L389
  // License: Apache-2.0
  private static URL[] getClassLoaderUrls(ClassLoader classLoader) {
    // Java 8
    if (classLoader instanceof URLClassLoader) {
      return ((URLClassLoader) classLoader).getURLs();
    }

    // Java 9 and later
    if (!classLoader.getClass().getName().startsWith("jdk.internal.loader.ClassLoaders$")) {
      throw new IllegalStateException(
          "Unsupported ClassLoader: " + classLoader.getClass().getName());
    }
    try {
      Unsafe unsafe = UnsafeProvider.getUnsafe();
      Field ucpField;
      try {
        // Java 9-15:
        // jdk.internal.loader.ClassLoaders.AppClassLoader.ucp
        ucpField = classLoader.getClass().getDeclaredField("ucp");
      } catch (NoSuchFieldException e) {
        // Java 16+:
        // jdk.internal.loader.BuiltinClassLoader.ucp
        // https://github.com/openjdk/jdk/commit/03a4df0acd103702e52dcd01c3f03fda4d7b04f5#diff-32cc12c0e3172fe5f2da1f65a75fa1cb920c39040d06323c83ad2c4d84e095aaL147
        ucpField = classLoader.getClass().getSuperclass().getDeclaredField("ucp");
      }
      long ucpFieldOffset = unsafe.objectFieldOffset(ucpField);
      Object ucpObject = unsafe.getObject(classLoader, ucpFieldOffset);

      // jdk.internal.loader.URLClassPath.path
      Field pathField = ucpField.getType().getDeclaredField("path");
      long pathFieldOffset = unsafe.objectFieldOffset(pathField);
      ArrayList<URL> path = (ArrayList<URL>) unsafe.getObject(ucpObject, pathFieldOffset);

      return path.toArray(new URL[0]);
    } catch (Exception ignored) {
      throw new IllegalStateException(
          "Failed to get URLs from ClassLoader: " + classLoader.getClass().getName());
    }
  }
}
