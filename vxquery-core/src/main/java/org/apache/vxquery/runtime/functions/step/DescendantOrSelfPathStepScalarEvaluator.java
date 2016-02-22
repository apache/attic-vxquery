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
package org.apache.vxquery.runtime.functions.step;

import java.io.IOException;

import org.apache.vxquery.datamodel.accessors.SequencePointable;
import org.apache.vxquery.datamodel.accessors.TaggedValuePointable;
import org.apache.vxquery.datamodel.values.ValueTag;
import org.apache.vxquery.exceptions.ErrorCode;
import org.apache.vxquery.exceptions.SystemException;
import org.apache.vxquery.types.DocumentType;
import org.apache.vxquery.types.ElementType;
import org.apache.vxquery.types.Quantifier;
import org.apache.vxquery.types.SequenceType;

import edu.uci.ics.hyracks.algebricks.runtime.base.IScalarEvaluator;
import edu.uci.ics.hyracks.api.context.IHyracksTaskContext;
import edu.uci.ics.hyracks.data.std.api.IPointable;
import edu.uci.ics.hyracks.data.std.primitive.VoidPointable;
import edu.uci.ics.hyracks.data.std.util.ArrayBackedValueStorage;

public class DescendantOrSelfPathStepScalarEvaluator extends AbstractDescendantPathStepScalarEvaluator {
    private final TaggedValuePointable rootTVP;
    private final ArrayBackedValueStorage seqAbvs;
    private final SequencePointable seqp = (SequencePointable) SequencePointable.FACTORY.createPointable();
    private final VoidPointable p = (VoidPointable) VoidPointable.FACTORY.createPointable();
    private final TaggedValuePointable tvp = (TaggedValuePointable) TaggedValuePointable.FACTORY.createPointable();

    public DescendantOrSelfPathStepScalarEvaluator(IScalarEvaluator[] args, IHyracksTaskContext ctx) {
        super(args, ctx);
        rootTVP = (TaggedValuePointable) TaggedValuePointable.FACTORY.createPointable();
        seqAbvs = new ArrayBackedValueStorage();
    }

    @Override
    protected final void evaluate(TaggedValuePointable[] args, IPointable result) throws SystemException {
        try {
            // Set up the result sequence.
            seqAbvs.reset();
            seqb.reset(seqAbvs);

            // Check the argument passed in as sequence or node tree.
            if (args[0].getTag() == ValueTag.SEQUENCE_TAG) {
                args[0].getValue(seqp);
                for (int j = 0; j < seqp.getEntryCount(); ++j) {
                    seqp.getEntry(j, p);
                    tvp.set(p.getByteArray(), p.getStartOffset(), p.getLength());
                    if (tvp.getTag() != ValueTag.NODE_TREE_TAG) {
                        throw new SystemException(ErrorCode.SYSE0001);
                    }
                    tvp.getValue(ntp);
                    processNodeTree(rootTVP);
                }
            } else if (args[0].getTag() == ValueTag.NODE_TREE_TAG) {
                args[0].getValue(ntp);
                processNodeTree(rootTVP);
            } else {
                throw new SystemException(ErrorCode.SYSE0001);
            }

            seqb.finish();
            result.set(seqAbvs);
        } catch (IOException e) {
            throw new SystemException(ErrorCode.SYSE0001, e);
        }
    }

    private void processNodeTree(TaggedValuePointable rootTVP) throws SystemException {
        ntp.getRootNode(rootTVP);

        // Solve for self.
        switch (rootTVP.getTag()) {
            case ValueTag.DOCUMENT_NODE_TAG:
                setNodeTest(SequenceType.create(DocumentType.ANYDOCUMENT, Quantifier.QUANT_ONE));
                break;
            case ValueTag.ELEMENT_NODE_TAG:
                setNodeTest(SequenceType.create(ElementType.ANYELEMENT, Quantifier.QUANT_ONE));
                break;
            default:
                throw new SystemException(ErrorCode.SYSE0001);
        }
        itemTvp.set(rootTVP);
        try {
            appendNodeToResult();
        } catch (IOException e) {
            throw new SystemException(ErrorCode.SYSE0001, e);
        }

        // Solve for descendants.
        searchSubtree(rootTVP);
    }

}