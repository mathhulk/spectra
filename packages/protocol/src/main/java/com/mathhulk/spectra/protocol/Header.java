package com.mathhulk.spectra.protocol;

import java.util.List;
import java.util.Locale;

/** One HTTP header. Names are lower-cased on construction. */
public record Header(String name, String value) {
  public Header {
    name = name.toLowerCase(Locale.ROOT);
  }

  public static Header read(FrameReader reader) {
    return new Header(reader.readString(Limits.MAX_HEADER_NAME), reader.readString(Limits.MAX_HEADER_VALUE));
  }

  public void write(FrameWriter writer) {
    writer.writeString(name, Limits.MAX_HEADER_NAME).writeString(value, Limits.MAX_HEADER_VALUE);
  }

  public static List<Header> readList(FrameReader reader) {
    return reader.readList(Limits.MAX_HEADERS, Header::read);
  }

  public static void writeList(FrameWriter writer, List<Header> headers) {
    writer.writeList(headers, Limits.MAX_HEADERS, (w, h) -> h.write(w));
  }
}
