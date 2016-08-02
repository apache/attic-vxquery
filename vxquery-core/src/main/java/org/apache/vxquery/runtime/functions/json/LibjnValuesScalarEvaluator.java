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
package org.apache.vxquery.runtime.functions.json;

import org.apache.hyracks.algebricks.runtime.base.IScalarEvaluator;
import org.apache.hyracks.api.context.IHyracksTaskContext;
import org.apache.hyracks.data.std.api.IPointable;
import org.apache.hyracks.data.std.primitive.UTF8StringPointable;
import org.apache.hyracks.data.std.util.ArrayBackedValueStorage;
import org.apache.vxquery.datamodel.accessors.SequencePointable;
import org.apache.vxquery.datamodel.accessors.TaggedValuePointable;
import org.apache.vxquery.datamodel.accessors.jsonitem.ObjectPointable;
import org.apache.vxquery.datamodel.builders.sequence.SequenceBuilder;
import org.apache.vxquery.datamodel.values.ValueTag;
import org.apache.vxquery.exceptions.ErrorCode;
import org.apache.vxquery.exceptions.SystemException;
import org.apache.vxquery.runtime.functions.base.AbstractTaggedValueArgumentScalarEvaluator;

import java.io.IOException;

public class LibjnValuesScalarEvaluator extends AbstractTaggedValueArgumentScalarEvaluator {
    protected final IHyracksTaskContext ctx;
    private final ObjectPointable op;
    private final UTF8StringPointable stringKey;
    private final ArrayBackedValueStorage abvs1;
    private final SequenceBuilder sb;

    public LibjnValuesScalarEvaluator(IHyracksTaskContext ctx, IScalarEvaluator[] args) {
        super(args);
        this.ctx = ctx;
        stringKey = (UTF8StringPointable) UTF8StringPointable.FACTORY.createPointable();
        abvs1 = new ArrayBackedValueStorage();
        sb = new SequenceBuilder();
        op = (ObjectPointable) ObjectPointable.FACTORY.createPointable();
    }

    @Override
    protected void evaluate(TaggedValuePointable[] args, IPointable result) throws SystemException {
        TaggedValuePointable sequence = args[0];

        TaggedValuePointable tempTvp = ppool.takeOne(TaggedValuePointable.class);
        SequencePointable sp = ppool.takeOne(SequencePointable.class);
        try {
            abvs1.reset();
            sb.reset(abvs1);
            if (sequence.getTag() == ValueTag.SEQUENCE_TAG) {
                sequence.getValue(sp);
                for (int i = 0; i < sp.getEntryCount(); ++i) {
                    sp.getEntry(i, tempTvp);
                    if (tempTvp.getTag() == ValueTag.OBJECT_TAG) {
                        tempTvp.getValue(op);
                        addValues(tempTvp);
                    }
                }
            } else if (sequence.getTag() == ValueTag.OBJECT_TAG) {
                sequence.getValue(op);
                addValues(tempTvp);
            }
            sb.finish();
            result.set(abvs1);
        } catch (IOException e) {
            throw new SystemException(ErrorCode.SYSE0001, e);
        } finally {
            ppool.giveBack(tempTvp);
            ppool.giveBack(sp);
        }
    }

    private void addValues(TaggedValuePointable tempTvp) throws IOException, SystemException {
        TaggedValuePointable tempValue = ppool.takeOne(TaggedValuePointable.class);
        SequencePointable sp1 = ppool.takeOne(SequencePointable.class);
        try {
            op.getKeys(tempTvp);
            if (tempTvp.getTag() == ValueTag.XS_STRING_TAG) {
                tempTvp.getValue(stringKey);
                op.getValue(stringKey, tempValue);
                sb.addItem(tempValue);
            } else if (tempTvp.getTag() == ValueTag.SEQUENCE_TAG) {
                tempTvp.getValue(sp1);
                for (int j = 0; j < sp1.getEntryCount(); ++j) {
                    sp1.getEntry(j, tempTvp);
                    tempTvp.getValue(stringKey);
                    op.getValue(stringKey, tempValue);
                    sb.addItem(tempValue);
                }
            }
        } finally {
            ppool.giveBack(tempValue);
            ppool.giveBack(sp1);
        }
    }

}
