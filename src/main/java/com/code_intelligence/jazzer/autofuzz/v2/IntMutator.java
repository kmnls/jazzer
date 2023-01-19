package com.code_intelligence.jazzer.autofuzz.v2;

import static com.code_intelligence.jazzer.autofuzz.v2.MutatorSupport.checkArgument;
import static com.code_intelligence.jazzer.autofuzz.v2.MutatorSupport.expectedClass;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.function.Function;
import java.util.function.Supplier;

public class IntMutator implements Mutator {
  private static final class IntMutatable implements Mutatable<Integer> {

    private final int min;
    private final int max;
    private final int[] specialValues;
    private int value;

    private IntMutatable(Annotation[] annotations) {
      int min = Integer.MIN_VALUE;
      int max = Integer.MAX_VALUE;
      for (Annotation annotation : annotations) {
        if (annotation instanceof Positive) {
          min = Math.max(min, 1);
        }
      }
      checkArgument(min <= max);
      this.min = min;
      this.max = max;
      this.specialValues = new int[] {min, max};
    }


    @Override
    public void readFrom(DataInputStream data, DataInputStream control) throws IOException {
      value = control.readInt();
    }

    @Override
    public void writeTo(DataOutputStream data, DataOutputStream control) throws IOException {
      control.writeInt(value);
    }

    @Override
    public Mutatable<Integer> init(SplittableRandom prng) {
      value = prng.nextInt(min, max + 2);
      if (value == max + 1) {
        value = specialValues[prng.nextInt(specialValues.length)];
      }
      return this;
    }

    @Override
    public void mutate(SplittableRandom prng) {
      value += 1;
    }

    @Override
    public Integer get() {
      return value;
    }
  }

  @Override
  public Optional<Supplier<Mutatable<?>>> tryCreate(AnnotatedType type,
      Function<AnnotatedType, Optional<Supplier<Mutatable<?>>>> childFactory) {
    if (!expectedClass(type, int.class, Integer.class)) {
      return Optional.empty();
    }
    return Optional.of(() -> new IntMutatable(type.getAnnotations()));
  }
}
