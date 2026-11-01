package com.mathhulk.spectra.typegen;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Extracts parameter names and Javadoc from sources jars with javac's parser (no compilation). */
public final class SourceDocs {
  private SourceDocs() {
  }

  /** Simple erased type name as {@link Generator#memberKey} uses it: {@code List<String>} -> {@code List}. */
  static String simpleType(String type) {
    String erased = type.replaceAll("<.*>", "").replace("...", "[]").replaceAll("@\\S+\\s+", "").trim();
    int dot = erased.lastIndexOf('.', erased.indexOf('[') < 0 ? erased.length() : erased.indexOf('['));
    return dot < 0 ? erased : erased.substring(dot + 1);
  }

  /** Turns raw Javadoc into readable text for TSDoc: inline tags become backticks, HTML is stripped. */
  static String clean(String doc) {
    if (doc == null) {
      return null;
    }
    return doc.replaceAll("\\{@(?:code|literal) ([^}]*)}", "`$1`")
        .replaceAll("\\{@(?:link|linkplain) ([^} ]+)(?: ([^}]*))?}", "`$1`")
        .replaceAll("</?(p|b|i|em|strong|code|pre|ul|ol|li|br|a[^>]*)/?>", "")
        .replace("*/", "*\\/")
        .strip();
  }

  public static Docs read(List<Path> sourceJars) throws IOException {
    Docs docs = new Docs();
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      return docs; // A JRE without javac: no docs, generation still works.
    }
    for (Path jar : sourceJars) {
      List<JavaFileObject> files = new ArrayList<>();
      try (ZipFile zip = new ZipFile(jar.toFile())) {
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
          ZipEntry entry = entries.nextElement();
          if (!entry.getName().endsWith(".java") || entry.getName().endsWith("module-info.java")) {
            continue;
          }
          String content = new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
          files.add(new SimpleJavaFileObject(URI.create("string:///" + entry.getName()), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
              return content;
            }
          });
        }
      }
      JavacTask task = (JavacTask) compiler.getTask(null, null, diagnostic -> {
      }, List.of("-proc:none"), null, files);
      DocTrees trees = DocTrees.instance(task);
      for (CompilationUnitTree unit : task.parse()) {
        String pkg = unit.getPackageName() == null ? "" : unit.getPackageName().toString() + ".";
        new TreePathScanner<Void, Void>() {
          private final Deque<String> classes = new ArrayDeque<>();

          @Override
          public Void visitClass(ClassTree node, Void unused) {
            String name = classes.isEmpty() ? pkg + node.getSimpleName() : classes.peek() + "$" + node.getSimpleName();
            classes.push(name);
            docs.putClass(name, clean(trees.getDocComment(getCurrentPath())));
            super.visitClass(node, unused);
            classes.pop();
            return null;
          }

          @Override
          public Void visitMethod(MethodTree node, Void unused) {
            if (classes.isEmpty()) {
              return null;
            }
            String name = node.getName().contentEquals("<init>") ? "<init>" : node.getName().toString();
            List<String> types = new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (VariableTree parameter : node.getParameters()) {
              types.add(simpleType(parameter.getType().toString()));
              names.add(parameter.getName().toString());
            }
            docs.putMember(classes.peek(), name + "(" + String.join(",", types) + ")", names,
                clean(trees.getDocComment(getCurrentPath())));
            return null; // Do not descend into method bodies.
          }

          @Override
          public Void visitVariable(VariableTree node, Void unused) {
            if (!classes.isEmpty() && getCurrentPath().getParentPath().getLeaf().getKind() != Tree.Kind.METHOD) {
              docs.putMember(classes.peek(), node.getName().toString(), List.of(),
                  clean(trees.getDocComment(getCurrentPath())));
            }
            return null;
          }
        }.scan(unit, null);
      }
    }
    return docs;
  }
}
