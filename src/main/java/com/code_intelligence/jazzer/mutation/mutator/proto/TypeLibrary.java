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

package com.code_intelligence.jazzer.mutation.mutator.proto;

import static com.code_intelligence.jazzer.mutation.support.Preconditions.check;
import static com.code_intelligence.jazzer.mutation.support.StreamSupport.entry;
import static com.code_intelligence.jazzer.mutation.support.TypeSupport.asAnnotatedType;
import static com.code_intelligence.jazzer.mutation.support.TypeSupport.notNull;
import static com.code_intelligence.jazzer.mutation.support.TypeSupport.withTypeArguments;
import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

import com.code_intelligence.jazzer.mutation.annotation.NotNull;
import com.code_intelligence.jazzer.mutation.annotation.proto.DescriptorSource;
import com.code_intelligence.jazzer.mutation.support.TypeHolder;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

final class TypeLibrary {
  private static final AnnotatedType RAW_LIST = new TypeHolder<@NotNull List>() {}.annotatedType();
  private static final AnnotatedType RAW_MAP = new TypeHolder<@NotNull Map>() {}.annotatedType();
  private static final Map<JavaType, AnnotatedType> BASE_TYPE_WITH_PRESENCE =
      Stream
          .of(entry(JavaType.BOOLEAN, Boolean.class), entry(JavaType.BYTE_STRING, ByteString.class),
              entry(JavaType.DOUBLE, Double.class), entry(JavaType.ENUM, EnumValueDescriptor.class),
              entry(JavaType.FLOAT, Float.class), entry(JavaType.INT, Integer.class),
              entry(JavaType.LONG, Long.class), entry(JavaType.MESSAGE, Message.class),
              entry(JavaType.STRING, String.class))
          .collect(collectingAndThen(toMap(Entry::getKey, e -> asAnnotatedType(e.getValue())),
              map -> unmodifiableMap(new EnumMap<>(map))));

  static <T extends Builder> AnnotatedType getTypeToMutate(FieldDescriptor field) {
    if (field.isRequired()) {
      return getBaseType(field);
    } else if (field.isMapField()) {
      // Map fields are represented as repeated message fields, so this check has to come before the
      // one for regular repeated fields.
      AnnotatedType keyType = getBaseType(field.getMessageType().getFields().get(0));
      AnnotatedType valueType = getBaseType(field.getMessageType().getFields().get(1));
      return withTypeArguments(RAW_MAP, keyType, valueType);
    } else if (field.isRepeated()) {
      return withTypeArguments(RAW_LIST, getBaseType(field));
    } else if (field.hasPresence()) {
      return BASE_TYPE_WITH_PRESENCE.get(field.getJavaType());
    } else {
      return getBaseType(field);
    }
  }

  private static <T extends Builder> AnnotatedType getBaseType(FieldDescriptor field) {
    return notNull(BASE_TYPE_WITH_PRESENCE.get(field.getJavaType()));
  }

  private TypeLibrary() {}

  static Message getDefaultInstance(Class<? extends Message> messageClass) {
    Method getDefaultInstance;
    try {
      getDefaultInstance = messageClass.getMethod("getDefaultInstance");
      check(Modifier.isStatic(getDefaultInstance.getModifiers()));
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(
          format("Message class for builder type %s does not have a getDefaultInstance method",
              messageClass.getName()),
          e);
    }
    try {
      return (Message) getDefaultInstance.invoke(null);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException(
          format(getDefaultInstance + " isn't accessible or threw an exception"), e);
    }
  }

  static DynamicMessage getDefaultInstance(DescriptorSource descriptorSource) {
    String[] parts = descriptorSource.value().split("#");
    if (parts.length != 2) {
      throw new IllegalArgumentException(format(
          "Expected @DescriptorSource(\"%s\") to specify a fully-qualified field name (e.g. com.example.MyClass#MY_FIELD)",
          descriptorSource.value()));
    }

    Class<?> clazz;
    try {
      clazz = Class.forName(parts[0]);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(
          format("Failed to find class '%s' specified by @DescriptorSource(\"%s\")", parts[0],
              descriptorSource.value()),
          e);
    }

    Field field;
    try {
      field = clazz.getDeclaredField(parts[1]);
      field.setAccessible(true);
    } catch (NoSuchFieldException e) {
      throw new IllegalArgumentException(
          format("Failed to find field specified by @DescriptorSource(\"%s\")",
              descriptorSource.value()),
          e);
    }
    if (!Modifier.isStatic(field.getModifiers())) {
      throw new IllegalArgumentException(
          format("Expected field specified by @DescriptorSource(\"%s\") to be static",
              descriptorSource.value()));
    }
    if (!Modifier.isFinal(field.getModifiers())) {
      throw new IllegalArgumentException(
          format("Expected field specified by @DescriptorSource(\"%s\") to be final",
              descriptorSource.value()));
    }
    if (field.getType() != Descriptor.class) {
      throw new IllegalArgumentException(
          format("Expected field specified by @DescriptorSource(\"%s\") to have type %s, got %s",
              descriptorSource.value(), Descriptor.class.getName(), field.getType().getName()));
    }

    Descriptor descriptor;
    try {
      descriptor = (Descriptor) field.get(null);
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException(
          format("Failed to access field specified by @DescriptorSource(\"%s\")",
              descriptorSource.value()),
          e);
    }

    return DynamicMessage.getDefaultInstance(descriptor);
  }
}
