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
package org.apache.vxquery.compiler.rewriter.rules;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.vxquery.functions.BuiltinOperators;
import org.apache.vxquery.functions.Function;

import edu.uci.ics.hyracks.algebricks.common.exceptions.AlgebricksException;
import edu.uci.ics.hyracks.algebricks.core.algebra.base.ILogicalExpression;
import edu.uci.ics.hyracks.algebricks.core.algebra.base.ILogicalOperator;
import edu.uci.ics.hyracks.algebricks.core.algebra.base.IOptimizationContext;
import edu.uci.ics.hyracks.algebricks.core.algebra.base.LogicalExpressionTag;
import edu.uci.ics.hyracks.algebricks.core.algebra.base.LogicalOperatorTag;
import edu.uci.ics.hyracks.algebricks.core.algebra.expressions.AbstractFunctionCallExpression;
import edu.uci.ics.hyracks.algebricks.core.algebra.expressions.UnnestingFunctionCallExpression;
import edu.uci.ics.hyracks.algebricks.core.algebra.operators.logical.AbstractLogicalOperator;
import edu.uci.ics.hyracks.algebricks.core.algebra.operators.logical.AssignOperator;
import edu.uci.ics.hyracks.algebricks.core.algebra.operators.logical.UnnestOperator;

/**
 * The rule searches for unnest(iterate) operator followed by an assign(child)
 * operator and merges the assign into the unnest operator.
 *
 * <pre>
 * Before
 * 
 *   plan__parent
 *   UNNEST( $v2 : iterate( $v1 ) )
 *   ASSIGN( $v1 : sf1( $v0 ) )
 *   plan__child
 * 
 *   where plan__parent does not use $v1 and $v0 is defined in plan__child.
 *   sf1 is a scalar function that has a unnesting implementation.
 * 
 * After
 * 
 *   plan__parent
 *   UNNEST( $v2 : uf1( $v0 ) )
 *   plan__child
 * </pre>
 *
 * @author prestonc
 */
public class ConvertAssignToUnnestRule extends AbstractUsedVariablesProcessingRule {
    @Override
    protected boolean processOperator(Mutable<ILogicalOperator> opRef, IOptimizationContext context)
            throws AlgebricksException {
        AbstractLogicalOperator op = (AbstractLogicalOperator) opRef.getValue();
        if (op.getOperatorTag() != LogicalOperatorTag.UNNEST) {
            return false;
        }
        UnnestOperator unnest = (UnnestOperator) op;

        // Check to see if the expression is the iterate operator.
        ILogicalExpression logicalExpression = (ILogicalExpression) unnest.getExpressionRef().getValue();
        if (logicalExpression.getExpressionTag() != LogicalExpressionTag.FUNCTION_CALL) {
            return false;
        }
        AbstractFunctionCallExpression functionCall = (AbstractFunctionCallExpression) logicalExpression;
        if (!functionCall.getFunctionIdentifier().equals(BuiltinOperators.ITERATE.getFunctionIdentifier())) {
            return false;
        }

        AbstractLogicalOperator op2 = (AbstractLogicalOperator) unnest.getInputs().get(0).getValue();
        if (op2.getOperatorTag() != LogicalOperatorTag.ASSIGN) {
            return false;
        }
        AssignOperator assign = (AssignOperator) op2;

        if (usedVariables.contains(assign.getVariables())) {
            return false;
        }

        // Check to see if the expression has an unnesting implementation.
        ILogicalExpression logicalExpression2 = (ILogicalExpression) assign.getExpressions().get(0).getValue();
        if (logicalExpression2.getExpressionTag() != LogicalExpressionTag.FUNCTION_CALL) {
            return false;
        }
        AbstractFunctionCallExpression functionCall2 = (AbstractFunctionCallExpression) logicalExpression2;
        Function functionInfo2 = (Function) functionCall2.getFunctionInfo();
        if (!functionInfo2.hasUnnestingEvaluatorFactory()) {
            return false;
        }

        // Update the unnest parameters.
        unnest.getInputs().clear();
        unnest.getInputs().addAll(assign.getInputs());

        UnnestingFunctionCallExpression child = new UnnestingFunctionCallExpression(functionInfo2,
                functionCall2.getArguments());
        unnest.getExpressionRef().setValue(child);

        return true;
    }

}
