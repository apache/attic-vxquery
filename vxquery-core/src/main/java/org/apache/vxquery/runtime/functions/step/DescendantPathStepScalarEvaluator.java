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

import org.apache.vxquery.datamodel.accessors.TaggedValuePointable;
import org.apache.vxquery.datamodel.values.ValueTag;
import org.apache.vxquery.exceptions.ErrorCode;
import org.apache.vxquery.exceptions.SystemException;

import edu.uci.ics.hyracks.algebricks.runtime.base.IScalarEvaluator;
import edu.uci.ics.hyracks.api.context.IHyracksTaskContext;
import edu.uci.ics.hyracks.data.std.api.IPointable;
import edu.uci.ics.hyracks.data.std.util.ArrayBackedValueStorage;

public class DescendantPathStepScalarEvaluator extends AbstractDescendantPathStepScalarEvaluator {
    private final TaggedValuePointable rootTVP;

    private final ArrayBackedValueStorage seqAbvs;

    public DescendantPathStepScalarEvaluator(IScalarEvaluator[] args, IHyracksTaskContext ctx) {
        super(args, ctx);
        rootTVP = (TaggedValuePointable) TaggedValuePointable.FACTORY.createPointable();
        seqAbvs = new ArrayBackedValueStorage();
    }

    @Override
    protected final void evaluate(TaggedValuePointable[] args, IPointable result) throws SystemException {
        try {
            if (args[0].getTag() != ValueTag.NODE_TREE_TAG) {
                throw new SystemException(ErrorCode.SYSE0001);
            }
            args[0].getValue(ntp);

            // Set up the result sequence and get the root node.
            seqAbvs.reset();
            seqb.reset(seqAbvs);
            ntp.getRootNode(rootTVP);

            // Solve for descendants.
            searchSubtree(rootTVP);

            seqb.finish();
            result.set(seqAbvs);
        } catch (IOException e) {
            throw new SystemException(ErrorCode.SYSE0001, e);
        }
    }

}