# automatically generated by the FlatBuffers compiler, do not modify

# namespace: flatc

import flatbuffers

class StringCollectionColumn(object):
    __slots__ = ['_tab']

    @classmethod
    def GetRootAsStringCollectionColumn(cls, buf, offset):
        n = flatbuffers.encode.Get(flatbuffers.packer.uoffset, buf, offset)
        x = StringCollectionColumn()
        x.Init(buf, n + offset)
        return x

    # StringCollectionColumn
    def Init(self, buf, pos):
        self._tab = flatbuffers.table.Table(buf, pos)

    # StringCollectionColumn
    def Values(self, j):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(4))
        if o != 0:
            x = self._tab.Vector(o)
            x += flatbuffers.number_types.UOffsetTFlags.py_type(j) * 4
            x = self._tab.Indirect(x)
            from .StringCollectionCell import StringCollectionCell
            obj = StringCollectionCell()
            obj.Init(self._tab.Bytes, x)
            return obj
        return None

    # StringCollectionColumn
    def ValuesLength(self):
        o = flatbuffers.number_types.UOffsetTFlags.py_type(self._tab.Offset(4))
        if o != 0:
            return self._tab.VectorLen(o)
        return 0

def StringCollectionColumnStart(builder): builder.StartObject(1)
def StringCollectionColumnAddValues(builder, values): builder.PrependUOffsetTRelativeSlot(0, flatbuffers.number_types.UOffsetTFlags.py_type(values), 0)
def StringCollectionColumnStartValuesVector(builder, numElems): return builder.StartVector(4, numElems, 4)
def StringCollectionColumnEnd(builder): return builder.EndObject()
