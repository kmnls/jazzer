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

import static com.code_intelligence.jazzer.autofuzz.v2.MutatorSupport.expectedParameterizedType;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class MutatorSupportTest {
  private static void parameterized(
      @WithSize(min = 10, max = 20) ArrayList<@Positive Integer> param,
      @Positive Integer elementType) {}

  private static void parameterizedNested(
      @WithSize(min = 10,
          max = 20) ArrayList<@WithSize(min = 15, max = 20) ArrayList<@Positive Integer>> param,
      @WithSize(min = 15, max = 20) ArrayList<@Positive Integer> elementType) {}

  private static void notParameterized(@WithSize(min = 10, max = 20) ArrayList param) {}

  private static void wildcard(@WithSize(min = 10, max = 20) ArrayList<?> param) {}

  private static void classNotMatching(
      @WithSize(min = 10, max = 20) HashSet<@Positive Integer> param,
      @Positive Integer elementType) {}

  @Test
  public void testExpectedParameterizedType_parameterized() throws NoSuchMethodException {
    Method method =
        MutatorSupportTest.class.getDeclaredMethod("parameterized", ArrayList.class, Integer.class);
    AnnotatedType type = method.getAnnotatedParameterTypes()[0];
    AnnotatedType expectedElementType = method.getAnnotatedParameterTypes()[1];
    Optional<AnnotatedType> actualElementType = expectedParameterizedType(type, List.class);

    assertThat(actualElementType).isPresent();
    assertThat(actualElementType.get().getType()).isEqualTo(expectedElementType.getType());
    assertThat(actualElementType.get().getAnnotations())
        .isEqualTo(expectedElementType.getAnnotations());
  }

  @Test
  public void testExpectedParameterizedType_parameterizedNested() throws NoSuchMethodException {
    Method method = MutatorSupportTest.class.getDeclaredMethod(
        "parameterizedNested", ArrayList.class, ArrayList.class);
    AnnotatedType type = method.getAnnotatedParameterTypes()[0];
    AnnotatedType expectedElementType = method.getAnnotatedParameterTypes()[1];
    Optional<AnnotatedType> actualElementType = expectedParameterizedType(type, List.class);

    assertThat(actualElementType).isPresent();
    assertThat(actualElementType.get().getType()).isEqualTo(expectedElementType.getType());
    assertThat(actualElementType.get().getAnnotations())
        .isEqualTo(expectedElementType.getAnnotations());
  }

  @Test
  public void testExpectedParameterizedType_notParameterized() throws NoSuchMethodException {
    Method method = MutatorSupportTest.class.getDeclaredMethod("notParameterized", ArrayList.class);
    assertThat(expectedParameterizedType(method.getAnnotatedParameterTypes()[0], List.class))
        .isEmpty();
  }

  @Test
  public void testExpectedParameterizedType_classNotMatching() throws NoSuchMethodException {
    Method method = MutatorSupportTest.class.getDeclaredMethod(
        "classNotMatching", HashSet.class, Integer.class);
    assertThat(expectedParameterizedType(method.getAnnotatedParameterTypes()[0], List.class))
        .isEmpty();
  }

  @Test
  public void testExpectedParameterizedType_wildcard() throws NoSuchMethodException {
    Method method = MutatorSupportTest.class.getDeclaredMethod("wildcard", ArrayList.class);
    assertThat(expectedParameterizedType(method.getAnnotatedParameterTypes()[0], List.class))
        .isEmpty();
  }
}
