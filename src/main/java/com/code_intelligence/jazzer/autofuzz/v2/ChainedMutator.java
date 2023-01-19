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

import static com.code_intelligence.jazzer.autofuzz.v2.MutatorSupport.findFirstPresent;
import static java.util.stream.Collectors.toList;

import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class ChainedMutator implements Mutator {
  private final List<Mutator> mutators;

  public ChainedMutator(Mutator... mutators) {
    this.mutators = Collections.unmodifiableList(Arrays.stream(mutators).collect(toList()));
  }

  public Optional<Supplier<Mutatable<?>>> tryCreate(AnnotatedType type) {
    return tryCreate(type, null);
  }

  @Override
  public Optional<Supplier<Mutatable<?>>> tryCreate(
      AnnotatedType type, Function<AnnotatedType, Optional<Supplier<Mutatable<?>>>> childFactory) {
    return findFirstPresent(mutators.stream().map(
        mutator -> mutator.tryCreate(type, childType -> tryCreate(childType, null))));
  }
}
