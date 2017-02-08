// automatically generated by the FlatBuffers compiler, do not modify

package org.knime.flatbuffers.flatc;

import java.nio.*;
import java.lang.*;
import java.util.*;
import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class LongColumn extends Table {
  public static LongColumn getRootAsLongColumn(ByteBuffer _bb) { return getRootAsLongColumn(_bb, new LongColumn()); }
  public static LongColumn getRootAsLongColumn(ByteBuffer _bb, LongColumn obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { bb_pos = _i; bb = _bb; }
  public LongColumn __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public long values(int j) { int o = __offset(4); return o != 0 ? bb.getLong(__vector(o) + j * 8) : 0; }
  public int valuesLength() { int o = __offset(4); return o != 0 ? __vector_len(o) : 0; }
  public ByteBuffer valuesAsByteBuffer() { return __vector_as_bytebuffer(4, 8); }

  public static int createLongColumn(FlatBufferBuilder builder,
      int valuesOffset) {
    builder.startObject(1);
    LongColumn.addValues(builder, valuesOffset);
    return LongColumn.endLongColumn(builder);
  }

  public static void startLongColumn(FlatBufferBuilder builder) { builder.startObject(1); }
  public static void addValues(FlatBufferBuilder builder, int valuesOffset) { builder.addOffset(0, valuesOffset, 0); }
  public static int createValuesVector(FlatBufferBuilder builder, long[] data) { builder.startVector(8, data.length, 8); for (int i = data.length - 1; i >= 0; i--) builder.addLong(data[i]); return builder.endVector(); }
  public static void startValuesVector(FlatBufferBuilder builder, int numElems) { builder.startVector(8, numElems, 8); }
  public static int endLongColumn(FlatBufferBuilder builder) {
    int o = builder.endObject();
    return o;
  }
}

