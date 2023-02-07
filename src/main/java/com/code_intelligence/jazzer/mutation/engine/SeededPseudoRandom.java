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

package com.code_intelligence.jazzer.mutation.engine;

import com.code_intelligence.jazzer.mutation.api.PseudoRandom;
import java.util.SplittableRandom;

public final class SeededPseudoRandom implements PseudoRandom {
  private final SplittableRandom random;

  public SeededPseudoRandom(long seed) {
    this.random = new SplittableRandom(seed);
  }

  @Override
  public boolean nextBoolean() {
    return random.nextBoolean();
  }

  @Override
  public int nextInt() {
    return random.nextInt();
  }

  @Override
  public int nextInt(int upperExclusive) {
    return random.nextInt(upperExclusive);
  }

  @Override
  public int nextInt(int lowerInclusive, int upperExclusive) {
    return random.nextInt(lowerInclusive, upperExclusive);
  }
}
