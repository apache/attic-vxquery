/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.vxquery.datamodel.builders.nodes;

import java.io.DataOutput;
import java.io.IOException;

import org.apache.vxquery.datamodel.accessors.nodes.ElementNodePointable;
import org.apache.vxquery.datamodel.values.ValueTag;
import org.apache.vxquery.util.GrowableIntArray;

import edu.uci.ics.hyracks.data.std.api.IMutableValueStorage;
import edu.uci.ics.hyracks.data.std.api.IValueReference;
import edu.uci.ics.hyracks.data.std.primitive.BytePointable;
import edu.uci.ics.hyracks.data.std.primitive.IntegerPointable;
import edu.uci.ics.hyracks.data.std.util.ArrayBackedValueStorage;

public class ElementNodeBuilder extends AbstractNodeBuilder {
    private final GrowableIntArray attrSlots;

    private final IMutableValueStorage attrDataArea;

    private final GrowableIntArray childrenSlots;

    private final IMutableValueStorage childrenDataArea;

    private IMutableValueStorage mvs;

    private DataOutput out;

    private int headerOffset;

    private int nsChunkStart;

    private int nsCount;

    private int attrCount;

    private int childrenCount;

    public ElementNodeBuilder() {
        attrSlots = new GrowableIntArray();
        attrDataArea = new ArrayBackedValueStorage();
        childrenSlots = new GrowableIntArray();
        childrenDataArea = new ArrayBackedValueStorage();
    }

    @Override
    public void reset(IMutableValueStorage mvs) throws IOException {
        this.mvs = mvs;
        out = mvs.getDataOutput();
        out.write(ValueTag.ELEMENT_NODE_TAG);
        headerOffset = mvs.getLength();
        out.write(0);
    }

    @Override
    public void finish() throws IOException {
        byte header = 0;
        if (nsCount > 0) {
            header |= ElementNodePointable.NS_CHUNK_EXISTS_MASK;
        }
        if (attrCount > 0) {
            header |= ElementNodePointable.ATTRIBUTES_CHUNK_EXISTS_MASK;
        }
        if (childrenCount > 0) {
            header |= ElementNodePointable.CHILDREN_CHUNK_EXISTS_MASK;
        }
        BytePointable.setByte(mvs.getByteArray(), headerOffset, header);
    }

    public void setName(int uriCode, int localNameCode, int prefixCode) throws IOException {
        out.writeInt(prefixCode);
        out.writeInt(uriCode);
        out.writeInt(localNameCode);
    }

    public void setType(int uriCode, int localNameCode, int prefixCode) throws IOException {
        out.writeInt(prefixCode);
        out.writeInt(uriCode);
        out.writeInt(localNameCode);
    }

    public void setLocalNodeId(int localNodeId) throws IOException {
        out.writeInt(localNodeId);
    }

    public void startNamespaceChunk() {
        nsChunkStart = mvs.getLength();
        nsCount = 0;
    }

    public void addNamespace(int prefixCode, int uriCode) throws IOException {
        if (nsCount == 0) {
            out.writeInt(0);
        }
        out.writeInt(prefixCode);
        out.writeInt(uriCode);
        ++nsCount;
    }

    public void endNamespaceChunk() {
        byte[] bytes = mvs.getByteArray();
        IntegerPointable.setInteger(bytes, nsChunkStart, nsCount);
    }

    public void startAttributeChunk() {
        attrSlots.clear();
        attrDataArea.reset();
    }

    public void startAttribute(AttributeNodeBuilder attrb) throws IOException {
        attrb.reset(attrDataArea);
    }

    public void endAttribute(AttributeNodeBuilder attrb) throws IOException {
        attrb.finish();
        attrSlots.append(attrDataArea.getLength());
    }

    public void endAttributeChunk() throws IOException {
        attrCount = attrSlots.getSize();
        if (attrCount > 0) {
            out.writeInt(attrCount);
            int[] slotArray = attrSlots.getArray();
            for (int i = 0; i < attrCount; ++i) {
                int slot = slotArray[i];
                out.writeInt(slot);
            }
            out.write(attrDataArea.getByteArray(), attrDataArea.getStartOffset(), attrDataArea.getLength());
        }
    }

    public void startChildrenChunk() {
        childrenSlots.clear();
        childrenDataArea.reset();
    }

    public void startChild(AbstractNodeBuilder nb) throws IOException {
        nb.reset(childrenDataArea);
    }

    public void endChild(AbstractNodeBuilder nb) throws IOException {
        nb.finish();
        childrenSlots.append(childrenDataArea.getLength());
    }

    public void addChild(IValueReference value) throws IOException {
        childrenDataArea.getDataOutput().write(value.getByteArray(), value.getStartOffset(), value.getLength());
        childrenSlots.append(childrenDataArea.getLength());
    }

    public void endChildrenChunk() throws IOException {
        childrenCount = childrenSlots.getSize();
        if (childrenCount > 0) {
            out.writeInt(childrenCount);
            int[] slotArray = childrenSlots.getArray();
            for (int i = 0; i < childrenCount; ++i) {
                int slot = slotArray[i];
                out.writeInt(slot);
            }
            out.write(childrenDataArea.getByteArray(), childrenDataArea.getStartOffset(), childrenDataArea.getLength());
        }
    }
}