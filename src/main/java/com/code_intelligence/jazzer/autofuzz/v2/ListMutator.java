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

import static com.code_intelligence.jazzer.autofuzz.v2.MutatorSupport.DEFAULT_MAX_SIZE;
import static com.code_intelligence.jazzer.autofuzz.v2.MutatorSupport.DEFAULT_MIN_SIZE;
import static com.code_intelligence.jazzer.autofuzz.v2.MutatorSupport.expectedParameterizedType;
import static java.lang.Math.min;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ListMutator implements Mutator {
  private static final class ListMutatable<T>
      extends AbstractList<T> implements Mutatable<List<T>> {
    private final ArrayList<Mutatable<T>> list = new ArrayList<>();
    private final Supplier<Mutatable<T>> makeElement;
    private final int minSize;
    private final int maxSize;

    private ListMutatable(Annotation[] annotations, Supplier<Mutatable<T>> makeElement) {
      this.makeElement = makeElement;
      int minSize = DEFAULT_MIN_SIZE;
      int maxSize = DEFAULT_MAX_SIZE;
      for (Annotation annotation : annotations) {
        if (annotation instanceof WithSize) {
          WithSize withSize = (WithSize) annotation;
          minSize = withSize.min();
          maxSize = withSize.max();
        }
      }
      this.minSize = minSize;
      this.maxSize = maxSize;
    }

    @Override
    public void readFrom(DataInputStream data, DataInputStream control) throws IOException {
      list.clear();
      int size = control.readInt();
      list.ensureCapacity(size);
      for (int i = 0; i < size; i++) {
        Mutatable<T> element = makeElement.get();
        try {
          element.readFrom(data, control);
        } catch (IOException e) {
          for (int j = i; j < size; j++) {
            list.add(null);
          }
          throw e;
        }
        list.add(element);
      }
    }

    @Override
    public void writeTo(DataOutputStream data, DataOutputStream control) throws IOException {
      control.writeInt(list.size());
      for (Mutatable<T> element : list) {
        element.writeTo(data, control);
      }
    }

    @Override
    public ListMutatable<T> init(SplittableRandom prng) {
      int targetSize = prng.nextInt(minSize, min(maxSize, minSize + 1) + 1);
      for (int i = 0; i < targetSize; i++) {
        list.add(makeElement.get().init(prng));
      }
      return this;
    }

    @Override
    public void mutate(SplittableRandom prng) {}

    @Override
    public List<T> get() {
      return this;
    }

    @Override
    public T get(int index) {
      return list.get(index).get();
    }

    @Override
    public int size() {
      return list.size();
    }
  }

  @Override
  public Optional<Supplier<Mutatable<?>>> tryCreate(
      AnnotatedType type, Function<AnnotatedType, Optional<Supplier<Mutatable<?>>>> childFactory) {
    return expectedParameterizedType(type, List.class)
        .flatMap(childFactory)
        .map(makeElement -> () -> new ListMutatable(type.getAnnotations(), makeElement));
  }
}
