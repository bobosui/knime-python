# automatically generated by the FlatBuffers compiler, do not modify

# namespace: flatc

import flatbuffers

class DoubleCollectionColumn(object):
    __slots__ = ['_tab']

    @classmethod
    def GetRootAsDoubleCollectionColumn(cls, buf, offset):
        n = flatbuffers.encode.Get(flatbuffers.packer.uoffset, buf, offset)
        x = DoubleCollectionColumn()
        x.Init(buf, n + offset)
        return x

    # DoubleCollectionColumn
    def Init(self, buf, pos):
        self._tab = flatbuffers.table.Table(buf, pos)

    # DoubleCollectionColumn
    def Values(self, j):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(4))
        if o != 0:
            x = self._tab.Vector(o)
            x += flatbuffers.number_types.UOffsetTFlags.py_type(j) * 4
            x = self._tab.Indirect(x)
            from .DoubleCollectionCell import DoubleCollectionCell
            obj = DoubleCollectionCell()
            obj.Init(self._tab.Bytes, x)
            return obj
        return None

    # DoubleCollectionColumn
    def ValuesLength(self):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(4))
        if o != 0:
            return self._tab.VectorLen(o)
        return 0

def DoubleCollectionColumnStart(builder): builder.StartObject(1)
def DoubleCollectionColumnAddValues(builder, values): builder.PrependUOffsetTRelativeSlot(0, flatbuffers.number_types.UOffsetTFlags.py_type(values), 0)
def DoubleCollectionColumnStartValuesVector(builder, numElems): return builder.StartVector(4, numElems, 4)
def DoubleCollectionColumnEnd(builder): return builder.EndObject()
