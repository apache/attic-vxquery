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
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.vxquery.compiler.algebricks.VXQueryConstantValue;
import org.apache.vxquery.compiler.rewriter.rules.util.ExpressionToolbox;
import org.apache.vxquery.datamodel.values.XDMConstants;
import org.apache.vxquery.functions.BuiltinOperators;
import org.apache.vxquery.types.BuiltinTypeRegistry;
import org.apache.vxquery.types.Quantifier;
import org.apache.vxquery.types.SequenceType;

import org.apache.hyracks.algebricks.common.exceptions.AlgebricksException;
import org.apache.hyracks.algebricks.core.algebra.base.ILogicalExpression;
import org.apache.hyracks.algebricks.core.algebra.base.ILogicalOperator;
import org.apache.hyracks.algebricks.core.algebra.base.IOptimizationContext;
import org.apache.hyracks.algebricks.core.algebra.base.LogicalExpressionTag;
import org.apache.hyracks.algebricks.core.algebra.base.LogicalOperatorTag;
import org.apache.hyracks.algebricks.core.algebra.base.LogicalVariable;
import org.apache.hyracks.algebricks.core.algebra.expressions.AbstractFunctionCallExpression;
import org.apache.hyracks.algebricks.core.algebra.expressions.ConstantExpression;
import org.apache.hyracks.algebricks.core.algebra.expressions.VariableReferenceExpression;
import org.apache.hyracks.algebricks.core.algebra.functions.IFunctionInfo;
import org.apache.hyracks.algebricks.core.algebra.operators.logical.AbstractLogicalOperator;
import org.apache.hyracks.algebricks.core.algebra.operators.logical.AggregateOperator;
import org.apache.hyracks.algebricks.core.algebra.operators.logical.AssignOperator;
import org.apache.hyracks.algebricks.core.algebra.operators.logical.SubplanOperator;
import org.apache.hyracks.data.std.api.IPointable;
import org.apache.hyracks.data.std.primitive.BooleanPointable;

/**
 * The rule searches for assign operator with an aggregate function expression
 * immediately following an aggregate operator with a sequence expression.
 * XQuery aggregate functions are implemented in both scalar (one XDM Instance
 * input as a sequence) and iterative (a stream of XDM Instances each one is
 * single object).
 * 
 * <pre>
 * Before 
 * 
 *   plan__parent
 *   ASSIGN( $v2 : sf1( $v1 ) )
 *   SUBPLAN{
 *     AGGREGATE( $v1 : sequence( $v0 ) )
 *     plan__nested
 *     NESTEDTUPLESOURCE
 *   }
 *   plan__child
 *   
 *   Where sf1 is a XQuery aggregate function expression (count, max, min, 
 *   average, sum) with any supporting functions like treat, promote or data. 
 *   Also plan__parent does not use $v1
 *   
 * After
 * 
 *   plan__parent
 *   SUBPLAN{
 *     AGGREGATE( $v2 : af1( $v0 ) )
 *     plan__nested
 *     NESTEDTUPLESOURCE
 *   }
 *   plan__child
 *   
 *   Where af1 is a XQuery aggregate function expression (count, max, min, 
 *   average, sum) that has been implemented in an iterative approach and works 
 *   on individual tuples instead of one value in given as a sequence.
 * </pre>
 * 
 * @author prestoncarman
 */
public class ConsolidateAssignAggregateRule extends AbstractVXQueryAggregateRule {
    @Override
    public boolean rewritePre(Mutable<ILogicalOperator> opRef, IOptimizationContext context) throws AlgebricksException {
        IFunctionInfo aggregateInfo;
        AbstractFunctionCallExpression finalFunctionCall;
        Mutable<ILogicalExpression> mutableVariableExpresion;

        // Check if assign is for aggregate function.
        AbstractLogicalOperator op = (AbstractLogicalOperator) opRef.getValue();
        if (op.getOperatorTag() != LogicalOperatorTag.ASSIGN) {
            return false;
        }
        AssignOperator assign = (AssignOperator) op;

        Mutable<ILogicalExpression> mutableLogicalExpression = assign.getExpressions().get(0);
        ILogicalExpression logicalExpression = mutableLogicalExpression.getValue();
        if (logicalExpression.getExpressionTag() != LogicalExpressionTag.FUNCTION_CALL) {
            return false;
        }
        AbstractFunctionCallExpression functionCall = (AbstractFunctionCallExpression) logicalExpression;
        // TODO Use the function definition
        aggregateInfo = getAggregateFunction(functionCall);
        if (aggregateInfo == null) {
            return false;
        }
        mutableVariableExpresion = ExpressionToolbox.findVariableExpression(mutableLogicalExpression);
        if (mutableVariableExpresion == null) {
            return false;
        }
        Mutable<ILogicalExpression> finalFunctionCallM = ExpressionToolbox
                .findLastFunctionExpression(mutableLogicalExpression);
        finalFunctionCall = (AbstractFunctionCallExpression) finalFunctionCallM.getValue();

        // Variable details.
        VariableReferenceExpression variableReference = (VariableReferenceExpression) mutableVariableExpresion
                .getValue();
        int variableId = variableReference.getVariableReference().getId();

        // Search for variable see if it is a aggregate sequence.
        AbstractLogicalOperator opSearch = (AbstractLogicalOperator) op.getInputs().get(0).getValue();
        opSearch = findSequenceAggregateOperator(opSearch, variableId);
        if (opSearch == null) {
            return false;
        }

        AggregateOperator aggregate = (AggregateOperator) opSearch;

        // Check to see if the expression is a function and sort-distinct-nodes-asc-or-atomics.
        ILogicalExpression logicalExpressionSearch = (ILogicalExpression) aggregate.getExpressions().get(0).getValue();
        if (logicalExpressionSearch.getExpressionTag() != LogicalExpressionTag.FUNCTION_CALL) {
            return false;
        }
        AbstractFunctionCallExpression functionCallSearch = (AbstractFunctionCallExpression) logicalExpressionSearch;
        if (!functionCallSearch.getFunctionIdentifier().equals(BuiltinOperators.SEQUENCE.getFunctionIdentifier())) {
            return false;
        }

        // Set the aggregate function to use new aggregate option.
        functionCallSearch.setFunctionInfo(aggregateInfo);

        // Alter arguments to include the aggregate arguments.
        finalFunctionCall.getArguments().get(0).setValue(functionCallSearch.getArguments().get(0).getValue());

        // Move the arguments for the assign function into aggregate. 
        functionCallSearch.getArguments().get(0).setValue(functionCall.getArguments().get(0).getValue());

        // Remove the aggregate assign, by creating a no op.
        assign.getExpressions().get(0).setValue(variableReference);

        // Add an assign operator to set up partitioning variable.
        // Create a new assign for a TRUE variable.
        LogicalVariable trueVar = context.newVar();
        IPointable p = (BooleanPointable) BooleanPointable.FACTORY.createPointable();
        XDMConstants.setTrue(p);
        VXQueryConstantValue cv = new VXQueryConstantValue(SequenceType.create(BuiltinTypeRegistry.XS_BOOLEAN,
                Quantifier.QUANT_ONE), p.getByteArray());
        AssignOperator trueAssignOp = new AssignOperator(trueVar, new MutableObject<ILogicalExpression>(
                new ConstantExpression(cv)));

        ILogicalOperator aggInput = aggregate.getInputs().get(0).getValue();
        aggregate.getInputs().get(0).setValue(trueAssignOp);
        trueAssignOp.getInputs().add(new MutableObject<ILogicalOperator>(aggInput));

        // Set partitioning variable.
        // TODO Review why this is not valid in 0.2.6
        //        aggregate.setPartitioningVariable(trueVar);

        return true;
    }

    private AbstractLogicalOperator findSequenceAggregateOperator(AbstractLogicalOperator opSearch, int variableId) {
        while (true) {
            if (opSearch.getOperatorTag() == LogicalOperatorTag.AGGREGATE) {
                // Check for variable assignment and sequence.
                AggregateOperator aggregate = (AggregateOperator) opSearch;

                // Check to see if the expression is a function and sort-distinct-nodes-asc-or-atomics.
                ILogicalExpression logicalExpressionSearch = (ILogicalExpression) aggregate.getExpressions().get(0)
                        .getValue();
                if (logicalExpressionSearch.getExpressionTag() != LogicalExpressionTag.FUNCTION_CALL) {
                    opSearch = (AbstractLogicalOperator) opSearch.getInputs().get(0).getValue();
                    continue;
                }
                AbstractFunctionCallExpression functionCallSearch = (AbstractFunctionCallExpression) logicalExpressionSearch;
                if (!functionCallSearch.getFunctionIdentifier().equals(
                        BuiltinOperators.SEQUENCE.getFunctionIdentifier())) {
                    opSearch = (AbstractLogicalOperator) opSearch.getInputs().get(0).getValue();
                    continue;
                }

                // Only find operator for the following variable ID.
                if (variableId != aggregate.getVariables().get(0).getId()) {
                    opSearch = (AbstractLogicalOperator) opSearch.getInputs().get(0).getValue();
                    continue;
                }

                // Found the aggregate operator!!!
                return opSearch;
            } else if (opSearch.getOperatorTag() == LogicalOperatorTag.SUBPLAN) {
                // Run through subplan.
                SubplanOperator subplan = (SubplanOperator) opSearch;
                AbstractLogicalOperator opSubplan = (AbstractLogicalOperator) subplan.getNestedPlans().get(0)
                        .getRoots().get(0).getValue();
                AbstractLogicalOperator search = findSequenceAggregateOperator(opSubplan, variableId);
                if (search != null) {
                    return search;
                }
            }
            if (opSearch.getOperatorTag() != LogicalOperatorTag.EMPTYTUPLESOURCE
                    && opSearch.getOperatorTag() != LogicalOperatorTag.NESTEDTUPLESOURCE) {
                opSearch = (AbstractLogicalOperator) opSearch.getInputs().get(0).getValue();
            } else {
                break;
            }
        }
        if (opSearch.getOperatorTag() == LogicalOperatorTag.EMPTYTUPLESOURCE
                || opSearch.getOperatorTag() == LogicalOperatorTag.NESTEDTUPLESOURCE) {
            return null;
        }
        return opSearch;
    }

    @Override
    public boolean rewritePost(Mutable<ILogicalOperator> opRef, IOptimizationContext context) {
        return false;
    }
}
