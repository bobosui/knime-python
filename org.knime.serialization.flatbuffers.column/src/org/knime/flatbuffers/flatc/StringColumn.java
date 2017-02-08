// automatically generated by the FlatBuffers compiler, do not modify

package org.knime.flatbuffers.flatc;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class StringColumn extends Table {
  public static StringColumn getRootAsStringColumn(ByteBuffer _bb) { return getRootAsStringColumn(_bb, new StringColumn()); }
  public static StringColumn getRootAsStringColumn(ByteBuffer _bb, StringColumn obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; }
  public StringColumn __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public String values(int j) { int o = __offset(4); return o != 0 ? __string(__vector(o) + j * 4) : null; }
  public int valuesLength() { int o = __offset(4); return o != 0 ? __vector_len(o) : 0; }

  public static int createStringColumn(FlatBufferBuilder builder,
      int valuesOffset) {
    builder.startObject(1);
    StringColumn.addValues(builder, valuesOffset);
    return StringColumn.endStringColumn(builder);
  }

  public static void startStringColumn(FlatBufferBuilder builder) { builder.startObject(1); }
  public static void addValues(FlatBufferBuilder builder, int valuesOffset) { builder.addOffset(0, valuesOffset, 0); }
  public static int createValuesVector(FlatBufferBuilder builder, int[] data) { builder.startVector(4, data.length, 4); for (int i = data.length - 1; i >= 0; i--) builder.addOffset(data[i]); return builder.endVector(); }
  public static void startValuesVector(FlatBufferBuilder builder, int numElems) { builder.startVector(4, numElems, 4); }
  public static int endStringColumn(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
}

