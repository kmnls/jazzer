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

package com.example;

import com.code_intelligence.jazzer.junit.FuzzTest;
import java.util.regex.Pattern;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HermeticInstrumentationFuzzTest {
  class VulnerableFuzzClass {
    public void vulnerableMethod(String input) {
      Pattern.compile(input);
    }
  }

  class VulnerableUnitClass {
    public void vulnerableMethod(String input) {
      Pattern.compile(input);
    }
  }

  @FuzzTest
  @Order(1)
  void fuzzTest1(byte[] data) {
    new VulnerableFuzzClass().vulnerableMethod("[");
  }

  @Test
  @Order(2)
  void unitTest1() {
    new VulnerableUnitClass().vulnerableMethod("[");
  }

  @FuzzTest
  @Order(3)
  void fuzzTest2(byte[] data) {
    Pattern.compile("[");
  }

  @Test
  @Order(4)
  void unitTest2() {
    Pattern.compile("[");
  }
}
