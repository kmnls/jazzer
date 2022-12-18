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

import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

// Based on
// https://github.com/spring-projects/spring-boot/blob/b132b5c31778b2bd453f5711705bdf2f679940ce/spring-boot-project/spring-boot-tools/spring-boot-test-support/src/main/java/org/springframework/boot/testsupport/classpath/ModifiedClassPathExtension.java
// License: Apache-2.0
public class IsolatingInvocationInterceptor implements InvocationInterceptor {
  static {
    IsolatedClassLoader.setPreservedClassNamePrefixes(
        Arrays.asList("org.junit.", "com.code_intelligence.jazzer.", "jaz.", "org.hamcrest."));
  }

  private static final Launcher launcher = createJupiterLauncher();

  private static Launcher createJupiterLauncher() {
    LauncherConfig config = LauncherConfig.builder()
                                .enableLauncherDiscoveryListenerAutoRegistration(false)
                                .enableLauncherSessionListenerAutoRegistration(false)
                                .enablePostDiscoveryFilterAutoRegistration(false)
                                .enableTestExecutionListenerAutoRegistration(false)
                                .build();
    return LauncherFactory.create(config);
  }

  @Override
  public void interceptTestTemplateMethod(Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
      throws Throwable {
    if (IsolatedClassLoader.isInstalled()) {
      invocation.proceed();
      return;
    }
    invocation.skip();
    try (AutoCloseable ignored = IsolatedClassLoader.install()) {
      runTest(extensionContext.getParent().get().getUniqueId());
    }
  }

  @Override
  public void interceptBeforeAllMethod(Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
      throws Throwable {
    proceedIfIsolated(invocation);
  }

  @Override
  public void interceptBeforeEachMethod(Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
      throws Throwable {
    proceedIfIsolated(invocation);
  }

  @Override
  public void interceptTestMethod(Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
      throws Throwable {
    proceedIfIsolated(invocation);
  }

  @Override
  public void interceptAfterEachMethod(Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
      throws Throwable {
    proceedIfIsolated(invocation);
  }

  @Override
  public void interceptAfterAllMethod(Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
      throws Throwable {
    proceedIfIsolated(invocation);
  }

  private void proceedIfIsolated(Invocation<Void> invocation) throws Throwable {
    if (IsolatedClassLoader.isInstalled()) {
      invocation.proceed();
    } else {
      invocation.skip();
    }
  }

  private void runTest(String testId) throws Throwable {
    LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                                           .selectors(DiscoverySelectors.selectUniqueId(testId))
                                           .build();
    TestPlan testPlan = launcher.discover(request);
    SummaryGeneratingListener listener = new SummaryGeneratingListener();
    launcher.registerTestExecutionListeners(listener);
    launcher.execute(testPlan);
    TestExecutionSummary summary = listener.getSummary();
    if (!summary.getFailures().isEmpty()) {
      throw summary.getFailures().get(0).getException();
    }
  }
}
