package com.mathhulk.spectra.typegen;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Parameter names and documentation for classes and members, keyed by class name and member key ({@code name(Simple,
 * Types)} for methods and constructors, {@code name} for fields). Stored as gzipped tab-separated lines so it needs
 * no JSON library: {@code C<TAB>class<TAB>doc} and {@code M<TAB>class<TAB>key<TAB>param,names<TAB>doc}.
 */
public final class Docs {
  public record Member(List<String> parameters, String doc) {
  }

  private final Map<String, String> classes = new HashMap<>();
  private final Map<String, Map<String, Member>> members = new HashMap<>();

  public static final Docs EMPTY = new Docs();

  public void putClass(String className, String doc) {
    if (doc != null && !doc.isBlank()) {
      classes.put(className, doc);
    }
  }

  public void putMember(String className, String key, List<String> parameters, String doc) {
    members.computeIfAbsent(className, k -> new HashMap<>()).put(key, new Member(List.copyOf(parameters),
        doc == null ? "" : doc));
  }

  public String classDoc(String className) {
    return classes.get(className);
  }

  public Member member(String className, String key) {
    Map<String, Member> map = members.get(className);
    return map == null ? null : map.get(key);
  }

  public int size() {
    return classes.size() + members.values().stream().mapToInt(Map::size).sum();
  }

  private static String escape(String text) {
    return text.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "");
  }

  private static String unescape(String text) {
    StringBuilder out = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '\\' && i + 1 < text.length()) {
        char next = text.charAt(++i);
        out.append(next == 'n' ? '\n' : next == 't' ? '\t' : next);
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }

  public void write(OutputStream out) throws IOException {
    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(out),
        StandardCharsets.UTF_8))) {
      for (Map.Entry<String, String> entry : classes.entrySet()) {
        writer.write("C\t" + entry.getKey() + "\t" + escape(entry.getValue()) + "\n");
      }
      for (Map.Entry<String, Map<String, Member>> type : members.entrySet()) {
        for (Map.Entry<String, Member> member : type.getValue().entrySet()) {
          writer.write("M\t" + type.getKey() + "\t" + member.getKey() + "\t"
              + String.join(",", member.getValue().parameters()) + "\t" + escape(member.getValue().doc()) + "\n");
        }
      }
    }
  }

  public static Docs read(InputStream in) throws IOException {
    Docs docs = new Docs();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(in),
        StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String[] parts = line.split("\t", -1);
        if (parts[0].equals("C") && parts.length >= 3) {
          docs.putClass(parts[1], unescape(parts[2]));
        } else if (parts[0].equals("M") && parts.length >= 5) {
          docs.putMember(parts[1], parts[2], parts[3].isEmpty() ? List.of() : List.of(parts[3].split(",")),
              unescape(parts[4]));
        }
      }
    }
    return docs;
  }
}
