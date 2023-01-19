package com.code_intelligence.jazzer.autofuzz.v2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.SplittableRandom;

public class TupleMutatable implements Mutatable<Object[]> {

  private final Mutatable<?>[] mutatables;

  public TupleMutatable(Mutatable<?>[] mutatables) {
    this.mutatables = Arrays.copyOf(mutatables, mutatables.length);
  }

  @Override
  public void readFrom(DataInputStream data, DataInputStream control) throws IOException {

  }

  @Override
  public void writeTo(DataOutputStream data, DataOutputStream control) throws IOException {

  }

  @Override
  public Mutatable<Object[]> init(SplittableRandom prng) {
    for (Mutatable<?> element : mutatables) {
      element.init(prng);
    }
    return this;
  }

  @Override
  public void mutate(SplittableRandom prng) {

  }

  @Override
  public Object[] get() {
    return new
  }
}
