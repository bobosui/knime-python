# automatically generated by the FlatBuffers compiler, do not modify

# namespace: flatc

import flatbuffers

class Column(object):
    __slots__ = ['_tab']

    @classmethod
    def GetRootAsColumn(cls, buf, offset):
        n = flatbuffers.encode.Get(flatbuffers.packer.uoffset, buf, offset)
        x = Column()
        x.Init(buf, n + offset)
        return x

    # Column
    def Init(self, buf, pos):
        self._tab = flatbuffers.table.Table(buf, pos)

    # Column
    def Type(self):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(4))
        if o != 0:
            return self._tab.Get(flatbuffers.number_types.Int32Flags, o + self._tab.Pos)
        return 0

    # Column
    def ByteColumn(self):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(6))
        if o != 0:
            x = self._tab.Indirect(o + self._tab.Pos)
            from .ByteColumn import ByteColumn
            obj = ByteColumn()
            obj.Init(self._tab.Bytes, x)
            return obj
        return None

    # Column
    def ByteListColumn(self):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(8))
        if o != 0:
            x = self._tab.Indirect(o + self._tab.Pos)
            from .ByteCollectionColumn import ByteCollectionColumn
            obj = ByteCollectionColumn()
            obj.Init(self._tab.Bytes, x)
            return obj
        return None

    # Column
    def ByteColColumn(self):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(10))
        if o != 0:
            x = self._tab.Indirect(o + self._tab.Pos)
            from .ByteCollectionColumn import ByteCollectionColumn
            obj = ByteCollectionColumn()
            obj.Init(self._tab.Bytes, x)
            return obj
        return None

    # Column
    def BooleanColumn(self):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(12))
        if o != 0:
            x = self._tab.Indirect(o + self._tab.Pos)
            from .BooleanColumn import BooleanColumn
            obj = BooleanColumn()
            obj.Init(self._tab.Bytes, x)
            return obj
        return None

    # Column
    def BooleanListColumn(self):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(14))
        if o != 0:
            x = self._tab.Indirect(o + self._tab.Pos)
            from .BooleanCollectionColumn import BooleanCollectionColumn
            obj = BooleanCollectionColumn()
            obj.Init(self._tab.Bytes, x)
            return obj
        return None

    # Column
    def BooleanSetColumn(self):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(16))
        if o != 0:
            x = self._tab.Indirect(o + self._tab.Pos)
            from .BooleanCollectionColumn import BooleanCollectionColumn
            obj = BooleanCollectionColumn()
            obj.Init(self._tab.Bytes, x)
            return obj
        return None

    # Column
    def DoubleColumn(self):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(18))
        if o != 0:
            x = self._tab.Indirect(o + self._tab.Pos)
            from .DoubleColumn import DoubleColumn
            obj = DoubleColumn()
            obj.Init(self._tab.Bytes, x)
            return obj
        return None

    # Column
    def DoubleListColumn(self):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(20))
        if o != 0:
            x = self._tab.Indirect(o + self._tab.Pos)
            from .DoubleCollectionColumn import DoubleCollectionColumn
            obj = DoubleCollectionColumn()
            obj.Init(self._tab.Bytes, x)
            return obj
        return None

    # Column
    def DoubleSetColumn(self):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(22))
        if o != 0:
            x = self._tab.Indirect(o + self._tab.Pos)
            from .DoubleCollectionColumn import DoubleCollectionColumn
            obj = DoubleCollectionColumn()
            obj.Init(self._tab.Bytes, x)
            return obj
        return None

    # Column
    def IntColumn(self):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(24))
        if o != 0:
            x = self._tab.Indirect(o + self._tab.Pos)
            from .IntColumn import IntColumn
            obj = IntColumn()
            obj.Init(self._tab.Bytes, x)
            return obj
        return None

    # Column
    def IntListColumn(self):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(26))
        if o != 0:
            x = self._tab.Indirect(o + self._tab.Pos)
            from .IntCollectionColumn import IntCollectionColumn
            obj = IntCollectionColumn()
            obj.Init(self._tab.Bytes, x)
            return obj
        return None

    # Column
    def IntSetColumn(self):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(28))
        if o != 0:
            x = self._tab.Indirect(o + self._tab.Pos)
            from .IntCollectionColumn import IntCollectionColumn
            obj = IntCollectionColumn()
            obj.Init(self._tab.Bytes, x)
            return obj
        return None

    # Column
    def LongColumn(self):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(30))
        if o != 0:
            x = self._tab.Indirect(o + self._tab.Pos)
            from .LongColumn import LongColumn
            obj = LongColumn()
            obj.Init(self._tab.Bytes, x)
            return obj
        return None

    # Column
    def LongListColumn(self):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(32))
        if o != 0:
            x = self._tab.Indirect(o + self._tab.Pos)
            from .LongCollectionColumn import LongCollectionColumn
            obj = LongCollectionColumn()
            obj.Init(self._tab.Bytes, x)
            return obj
        return None

    # Column
    def LongSetColumn(self):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(34))
        if o != 0:
            x = self._tab.Indirect(o + self._tab.Pos)
            from .LongCollectionColumn import LongCollectionColumn
            obj = LongCollectionColumn()
            obj.Init(self._tab.Bytes, x)
            return obj
        return None

    # Column
    def StringColumn(self):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(36))
        if o != 0:
            x = self._tab.Indirect(o + self._tab.Pos)
            from .StringColumn import StringColumn
            obj = StringColumn()
            obj.Init(self._tab.Bytes, x)
            return obj
        return None

    # Column
    def StringListColumn(self):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(38))
        if o != 0:
            x = self._tab.Indirect(o + self._tab.Pos)
            from .StringCollectionColumn import StringCollectionColumn
            obj = StringCollectionColumn()
            obj.Init(self._tab.Bytes, x)
            return obj
        return None

    # Column
    def StringSetColumn(self):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(40))
        if o != 0:
            x = self._tab.Indirect(o + self._tab.Pos)
            from .StringCollectionColumn import StringCollectionColumn
            obj = StringCollectionColumn()
            obj.Init(self._tab.Bytes, x)
            return obj
        return None

def ColumnStart(builder): builder.StartObject(19)
def ColumnAddType(builder, type): builder.PrependInt32Slot(0, type, 0)
def ColumnAddByteColumn(builder, byteColumn): builder.PrependUOffsetTRelativeSlot(1, flatbuffers.number_types.UOffsetTFlags.py_type(byteColumn), 0)
def ColumnAddByteListColumn(builder, byteListColumn): builder.PrependUOffsetTRelativeSlot(2, flatbuffers.number_types.UOffsetTFlags.py_type(byteListColumn), 0)
def ColumnAddByteColColumn(builder, byteColColumn): builder.PrependUOffsetTRelativeSlot(3, flatbuffers.number_types.UOffsetTFlags.py_type(byteColColumn), 0)
def ColumnAddBooleanColumn(builder, booleanColumn): builder.PrependUOffsetTRelativeSlot(4, flatbuffers.number_types.UOffsetTFlags.py_type(booleanColumn), 0)
def ColumnAddBooleanListColumn(builder, booleanListColumn): builder.PrependUOffsetTRelativeSlot(5, flatbuffers.number_types.UOffsetTFlags.py_type(booleanListColumn), 0)
def ColumnAddBooleanSetColumn(builder, booleanSetColumn): builder.PrependUOffsetTRelativeSlot(6, flatbuffers.number_types.UOffsetTFlags.py_type(booleanSetColumn), 0)
def ColumnAddDoubleColumn(builder, doubleColumn): builder.PrependUOffsetTRelativeSlot(7, flatbuffers.number_types.UOffsetTFlags.py_type(doubleColumn), 0)
def ColumnAddDoubleListColumn(builder, doubleListColumn): builder.PrependUOffsetTRelativeSlot(8, flatbuffers.number_types.UOffsetTFlags.py_type(doubleListColumn), 0)
def ColumnAddDoubleSetColumn(builder, doubleSetColumn): builder.PrependUOffsetTRelativeSlot(9, flatbuffers.number_types.UOffsetTFlags.py_type(doubleSetColumn), 0)
def ColumnAddIntColumn(builder, intColumn): builder.PrependUOffsetTRelativeSlot(10, flatbuffers.number_types.UOffsetTFlags.py_type(intColumn), 0)
def ColumnAddIntListColumn(builder, intListColumn): builder.PrependUOffsetTRelativeSlot(11, flatbuffers.number_types.UOffsetTFlags.py_type(intListColumn), 0)
def ColumnAddIntSetColumn(builder, intSetColumn): builder.PrependUOffsetTRelativeSlot(12, flatbuffers.number_types.UOffsetTFlags.py_type(intSetColumn), 0)
def ColumnAddLongColumn(builder, longColumn): builder.PrependUOffsetTRelativeSlot(13, flatbuffers.number_types.UOffsetTFlags.py_type(longColumn), 0)
def ColumnAddLongListColumn(builder, longListColumn): builder.PrependUOffsetTRelativeSlot(14, flatbuffers.number_types.UOffsetTFlags.py_type(longListColumn), 0)
def ColumnAddLongSetColumn(builder, longSetColumn): builder.PrependUOffsetTRelativeSlot(15, flatbuffers.number_types.UOffsetTFlags.py_type(longSetColumn), 0)
def ColumnAddStringColumn(builder, stringColumn): builder.PrependUOffsetTRelativeSlot(16, flatbuffers.number_types.UOffsetTFlags.py_type(stringColumn), 0)
def ColumnAddStringListColumn(builder, stringListColumn): builder.PrependUOffsetTRelativeSlot(17, flatbuffers.number_types.UOffsetTFlags.py_type(stringListColumn), 0)
def ColumnAddStringSetColumn(builder, stringSetColumn): builder.PrependUOffsetTRelativeSlot(18, flatbuffers.number_types.UOffsetTFlags.py_type(stringSetColumn), 0)
def ColumnEnd(builder): return builder.EndObject()
