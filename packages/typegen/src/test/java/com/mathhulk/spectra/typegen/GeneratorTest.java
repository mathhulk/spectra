package com.mathhulk.spectra.typegen;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorTest {
  /** A small API shaped like Bukkit's: generics, overloads, statics, enums, nested types, reserved names. */
  public static class Box<T> implements Comparable<Box<T>> {
    public static final int LIMIT = 3;
    public final String label;

    public Box(String label) {
      this.label = label;
    }

    public T get() {
      return null;
    }

    public void put(T value, int count) {
    }

    public void put(T value) {
    }

    public <R> List<R> map(java.util.function.Function<T, R> mapper) {
      return List.of();
    }

    public void delete(String function, int... default_) {
    }

    public static Box<String> of(String label) {
      return new Box<>(label);
    }

    @Override
    public int compareTo(Box<T> other) {
      return 0;
    }

    public enum Kind {
      SMALL, LARGE
    }
  }

  @Test
  void generatesInstanceAndStaticSides() {
    Docs docs = new Docs();
    docs.putMember(Box.class.getName(), "put(Object,int)", List.of("value", "count"), "Puts `count` values.");
    String out = new Generator(docs).generate(List.of(Box.class, Box.Kind.class));

    assertTrue(out.contains("namespace com.mathhulk.spectra.typegen {"), out);
    assertTrue(out.contains("interface GeneratorTest$Box<T> extends java.lang.Comparable<com.mathhulk.spectra.typegen.GeneratorTest$Box<T>> {"), out);
    assertTrue(out.contains("get(): T;"), out);
    assertTrue(out.contains("put(value: T, count: number): void;"), out);
    assertTrue(out.contains("* Puts `count` values."), out);
    assertTrue(out.contains("map<R>(arg0: java.util.function.Function<T, R>): java.util.List<R>;"), out);
    assertTrue(out.contains("delete(function_: string, ...default_: number[]): void;")
        || out.contains("delete(arg0: string, ...arg1: number[]): void;"), out);
    assertTrue(out.contains("readonly label: string;"), out);
    assertTrue(out.contains("const GeneratorTest$Box: JavaClass<GeneratorTest$Box<any>> & {"), out);
    assertTrue(out.contains("new<T>(arg0: string): GeneratorTest$Box<T>;"), out);
    assertTrue(out.contains("of(arg0: string): com.mathhulk.spectra.typegen.GeneratorTest$Box<string>;"), out);
    assertTrue(out.contains("readonly LIMIT: number;"), out);
    assertTrue(out.contains("readonly SMALL: com.mathhulk.spectra.typegen.GeneratorTest$Box$Kind;"), out);
    // Directly referenced JDK types are declared too.
    assertTrue(out.contains("namespace java.util {"), out);
    assertTrue(out.contains("interface List<E>"), out);
  }

  @Test
  void sourceTypeNamesMatchReflectionKeys() {
    assertEquals("List", SourceDocs.simpleType("java.util.List<String>"));
    assertEquals("String[]", SourceDocs.simpleType("String..."));
    assertEquals("Material", SourceDocs.simpleType("@NotNull Material"));
    assertEquals("int[][]", SourceDocs.simpleType("int[][]"));
  }
}
