/*
 * Copyright 2023 Code Intelligence GmbH
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

package com.code_intelligence.jazzer.autofuzz.v2;

import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.util.Optional;
import java.util.stream.Stream;

public final class MutatorSupport {
  public static final int DEFAULT_MIN_SIZE = 0;
  public static final int DEFAULT_MAX_SIZE = 1000;

  public static void checkArgument(boolean precondition) {
    if (!precondition) {
      throw new IllegalArgumentException();
    }
  }

  public static Optional<AnnotatedType> expectedParameterizedType(
      AnnotatedType type, Class<?> expectedParent) {
    if (!(type instanceof AnnotatedParameterizedType)) {
      return Optional.empty();
    }
    Class<?> clazz = (Class<?>) ((ParameterizedType) type.getType()).getRawType();
    if (!expectedParent.isAssignableFrom(clazz)) {
      return Optional.empty();
    }

    AnnotatedType[] typeArguments =
        ((AnnotatedParameterizedType) type).getAnnotatedActualTypeArguments();
    if (typeArguments.length != 1) {
      return Optional.empty();
    }
    AnnotatedType elementType = typeArguments[0];
    if (!(elementType.getType() instanceof ParameterizedType)
        && !(elementType.getType() instanceof Class)) {
      return Optional.empty();
    }

    return Optional.of(elementType);
  }

  public static boolean expectedClass(AnnotatedType type, Class<?>... expectedParents) {
    if (!(type.getType() instanceof Class<?>) ) {
      return false;
    }
    Class<?> clazz = (Class<?>) type.getType();
    return Stream.of(expectedParents).anyMatch(clazz::isAssignableFrom);
  }

  public static <T> Optional<T> findFirstPresent(Stream<Optional<T>> stream) {
    return stream.filter(Optional::isPresent).map(Optional::get).findFirst();
  }

  private MutatorSupport() {}
}
