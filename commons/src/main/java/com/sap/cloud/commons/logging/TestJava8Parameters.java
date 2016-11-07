/**
 * Copyright 2016 SAP SE
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.sap.cloud.commons.logging;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

public class TestJava8Parameters {

  public static void test() {
    try {
      final ObjectMapper mapper = new ObjectMapper().registerModule(new ParameterNamesModule(Mode.PROPERTIES));
      final TestJava8ParametersDummy readValue =
          mapper.readValue("{\"a\" : \"test\", \"b\": 10 }", TestJava8ParametersDummy.class);
      assert readValue.getA().equals("test");
      assert (readValue.getB() == 10);
    } catch (final IOException e) {
      final String ls = System.lineSeparator();
      final String msg = new StringBuilder("Dear ").append(System.getProperty("user.name")).append(",").append(ls)
          .append("All classes must be compiled with --parameter option. ")
          .append("This is already done with Maven.").append(ls)
          .append("You must configure Your IDE do use that option. For Eclipse:").append(ls)
          .append(
              "Window -> Prefrerences -> Java -> Compiler -> Store information about method parameters (usable via reflection)")
          .toString();
      throw new RuntimeException(msg);
    }
  }

  class TestJava8ParametersDummy {

    TestJava8ParametersDummy(final String a, final int b) {
      this.a = a;
      this.b = b;
    }

    private final String a;
    private final int b;

    public String getA() {
      return a;
    }

    public int getB() {
      return b;
    }
  }

}
