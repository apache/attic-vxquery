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
package org.apache.vxquery.runtime.functions.node;

import java.io.IOException;

import org.apache.vxquery.datamodel.accessors.TaggedValuePointable;
import org.apache.vxquery.datamodel.builders.nodes.DictionaryBuilder;
import org.apache.vxquery.datamodel.builders.nodes.PINodeBuilder;
import org.apache.vxquery.exceptions.SystemException;

import org.apache.hyracks.algebricks.runtime.base.IScalarEvaluator;
import org.apache.hyracks.api.context.IHyracksTaskContext;
import org.apache.hyracks.data.std.api.IMutableValueStorage;
import org.apache.hyracks.data.std.primitive.VoidPointable;

public class PINodeConstructorScalarEvaluator extends AbstractNodeConstructorScalarEvaluator {
    private final PINodeBuilder pinb;

    private final VoidPointable vp;

    public PINodeConstructorScalarEvaluator(IHyracksTaskContext ctx, IScalarEvaluator[] args) {
        super(ctx, args);
        pinb = new PINodeBuilder();
        vp = (VoidPointable) VoidPointable.FACTORY.createPointable();
    }

    @Override
    protected void constructNode(DictionaryBuilder db, TaggedValuePointable[] args, IMutableValueStorage mvs)
            throws IOException, SystemException {
        pinb.reset(mvs);
        TaggedValuePointable targetArg = args[0];
        targetArg.getValue(vp);
        pinb.setTarget(vp);
        TaggedValuePointable contentArg = args[1];
        contentArg.getValue(vp);
        pinb.setContent(vp);
        pinb.finish();
    }

    @Override
    protected boolean createsDictionary() {
        return false;
    }
}
