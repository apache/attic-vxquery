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
package org.apache.vxquery.runtime.functions.arithmetic;

import java.io.DataOutput;

import org.apache.vxquery.datamodel.accessors.TaggedValuePointable;
import org.apache.vxquery.datamodel.accessors.atomic.XSDatePointable;
import org.apache.vxquery.datamodel.accessors.atomic.XSDateTimePointable;
import org.apache.vxquery.datamodel.accessors.atomic.XSDecimalPointable;
import org.apache.vxquery.datamodel.accessors.atomic.XSDurationPointable;
import org.apache.vxquery.datamodel.accessors.atomic.XSTimePointable;
import org.apache.vxquery.exceptions.ErrorCode;
import org.apache.vxquery.exceptions.SystemException;
import org.apache.vxquery.runtime.functions.base.AbstractTaggedValueArgumentScalarEvaluator;
import org.apache.vxquery.runtime.functions.base.AbstractTaggedValueArgumentScalarEvaluatorFactory;
import org.apache.vxquery.types.BuiltinTypeConstants;
import org.apache.vxquery.types.BuiltinTypeRegistry;

import edu.uci.ics.hyracks.algebricks.common.exceptions.AlgebricksException;
import edu.uci.ics.hyracks.algebricks.runtime.base.IScalarEvaluator;
import edu.uci.ics.hyracks.algebricks.runtime.base.IScalarEvaluatorFactory;
import edu.uci.ics.hyracks.data.std.api.IPointable;
import edu.uci.ics.hyracks.data.std.primitive.BooleanPointable;
import edu.uci.ics.hyracks.data.std.primitive.BytePointable;
import edu.uci.ics.hyracks.data.std.primitive.DoublePointable;
import edu.uci.ics.hyracks.data.std.primitive.FloatPointable;
import edu.uci.ics.hyracks.data.std.primitive.IntegerPointable;
import edu.uci.ics.hyracks.data.std.primitive.LongPointable;
import edu.uci.ics.hyracks.data.std.primitive.ShortPointable;
import edu.uci.ics.hyracks.data.std.primitive.UTF8StringPointable;
import edu.uci.ics.hyracks.dataflow.common.data.accessors.ArrayBackedValueStorage;

public abstract class AbstractArithmeticScalarEvaluatorFactory extends
        AbstractTaggedValueArgumentScalarEvaluatorFactory {
    private static final long serialVersionUID = 1L;

    public AbstractArithmeticScalarEvaluatorFactory(IScalarEvaluatorFactory[] args) {
        super(args);
    }

    @Override
    protected IScalarEvaluator createEvaluator(IScalarEvaluator[] args) throws AlgebricksException {
        final AbstractArithmeticOperation aOp = createArithmeticOperation();
        final ArrayBackedValueStorage abvs = new ArrayBackedValueStorage();
        final DataOutput dOut = abvs.getDataOutput();
        final TypedPointables tp1 = new TypedPointables();
        final TypedPointables tp2 = new TypedPointables();
        return new AbstractTaggedValueArgumentScalarEvaluator(args) {
            @Override
            protected void evaluate(TaggedValuePointable[] args, IPointable result) throws SystemException {
                TaggedValuePointable tvp1 = args[0];
                TaggedValuePointable tvp2 = args[1];
                int tid1 = getBaseTypeForArithmetics(tvp1.getTag());
                int tid2 = getBaseTypeForArithmetics(tvp2.getTag());
                if (tid1 == BuiltinTypeConstants.XS_UNTYPED_ATOMIC_TYPE_ID) {
                    // TODO Convert to double
                    tid1 = BuiltinTypeConstants.XS_DOUBLE_TYPE_ID;
                    throw new UnsupportedOperationException();
                }
                if (tid2 == BuiltinTypeConstants.XS_UNTYPED_ATOMIC_TYPE_ID) {
                    // TODO Convert to double
                    tid2 = BuiltinTypeConstants.XS_DOUBLE_TYPE_ID;
                    throw new UnsupportedOperationException();
                }
                abvs.reset();
                try {
                    switch (tid1) {
                        case BuiltinTypeConstants.XS_DECIMAL_TYPE_ID:
                            tvp1.getValue(tp1.decp);
                            switch (tid2) {
                                case BuiltinTypeConstants.XS_DECIMAL_TYPE_ID:
                                    tvp2.getValue(tp2.decp);
                                    aOp.operateDecimalDecimal(tp1.decp, tp2.decp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_INTEGER_TYPE_ID:
                                    tvp2.getValue(tp2.longp);
                                    aOp.operateDecimalInteger(tp1.decp, tp2.longp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_FLOAT_TYPE_ID:
                                    tvp2.getValue(tp2.floatp);
                                    aOp.operateDecimalFloat(tp1.decp, tp2.floatp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_DOUBLE_TYPE_ID:
                                    tvp2.getValue(tp2.doublep);
                                    aOp.operateDecimalDouble(tp1.decp, tp2.doublep, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_DAY_TIME_DURATION_TYPE_ID:
                                    tvp2.getValue(tp2.intp);
                                    aOp.operateDecimalDTDuration(tp1.decp, tp2.intp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_YEAR_MONTH_DURATION_TYPE_ID:
                                    tvp2.getValue(tp2.intp);
                                    aOp.operateDecimalYMDuration(tp1.decp, tp2.intp, dOut);
                                    result.set(abvs);
                                    return;
                            }
                            break;

                        case BuiltinTypeConstants.XS_INTEGER_TYPE_ID:
                            tvp1.getValue(tp1.longp);
                            switch (tid2) {
                                case BuiltinTypeConstants.XS_DECIMAL_TYPE_ID:
                                    tvp2.getValue(tp2.decp);
                                    aOp.operateIntegerDecimal(tp1.longp, tp2.decp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_INTEGER_TYPE_ID:
                                    tvp2.getValue(tp2.longp);
                                    aOp.operateIntegerInteger(tp1.longp, tp2.longp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_FLOAT_TYPE_ID:
                                    tvp2.getValue(tp2.floatp);
                                    aOp.operateIntegerFloat(tp1.longp, tp2.floatp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_DOUBLE_TYPE_ID:
                                    tvp2.getValue(tp2.doublep);
                                    aOp.operateIntegerDouble(tp1.longp, tp2.doublep, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_DAY_TIME_DURATION_TYPE_ID:
                                    tvp2.getValue(tp2.intp);
                                    aOp.operateIntegerDTDuration(tp1.longp, tp2.intp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_YEAR_MONTH_DURATION_TYPE_ID:
                                    tvp2.getValue(tp2.intp);
                                    aOp.operateIntegerYMDuration(tp1.longp, tp2.intp, dOut);
                                    result.set(abvs);
                                    return;
                            }
                            break;

                        case BuiltinTypeConstants.XS_FLOAT_TYPE_ID:
                            tvp1.getValue(tp1.floatp);
                            switch (tid2) {
                                case BuiltinTypeConstants.XS_DECIMAL_TYPE_ID:
                                    tvp2.getValue(tp2.decp);
                                    aOp.operateFloatDecimal(tp1.floatp, tp2.decp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_INTEGER_TYPE_ID:
                                    tvp2.getValue(tp2.longp);
                                    aOp.operateFloatInteger(tp1.floatp, tp2.longp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_FLOAT_TYPE_ID:
                                    tvp2.getValue(tp2.floatp);
                                    aOp.operateFloatFloat(tp1.floatp, tp2.floatp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_DOUBLE_TYPE_ID:
                                    tvp2.getValue(tp2.doublep);
                                    aOp.operateFloatDouble(tp1.floatp, tp2.doublep, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_DAY_TIME_DURATION_TYPE_ID:
                                    tvp2.getValue(tp2.intp);
                                    aOp.operateFloatDTDuration(tp1.floatp, tp2.intp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_YEAR_MONTH_DURATION_TYPE_ID:
                                    tvp2.getValue(tp2.intp);
                                    aOp.operateFloatYMDuration(tp1.floatp, tp2.intp, dOut);
                                    result.set(abvs);
                                    return;
                            }
                            break;

                        case BuiltinTypeConstants.XS_DOUBLE_TYPE_ID:
                            tvp1.getValue(tp1.doublep);
                            switch (tid2) {
                                case BuiltinTypeConstants.XS_DECIMAL_TYPE_ID:
                                    tvp2.getValue(tp2.decp);
                                    aOp.operateDoubleDecimal(tp1.doublep, tp2.decp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_INTEGER_TYPE_ID:
                                    tvp2.getValue(tp2.longp);
                                    aOp.operateDoubleInteger(tp1.doublep, tp2.longp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_FLOAT_TYPE_ID:
                                    tvp2.getValue(tp2.floatp);
                                    aOp.operateDoubleFloat(tp1.doublep, tp2.floatp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_DOUBLE_TYPE_ID:
                                    tvp2.getValue(tp2.doublep);
                                    aOp.operateDoubleDouble(tp1.doublep, tp2.doublep, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_DAY_TIME_DURATION_TYPE_ID:
                                    tvp2.getValue(tp2.intp);
                                    aOp.operateDoubleDTDuration(tp1.doublep, tp2.intp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_YEAR_MONTH_DURATION_TYPE_ID:
                                    tvp2.getValue(tp2.intp);
                                    aOp.operateDoubleYMDuration(tp1.doublep, tp2.intp, dOut);
                                    result.set(abvs);
                                    return;
                            }
                            break;

                        case BuiltinTypeConstants.XS_DATE_TYPE_ID:
                            tvp1.getValue(tp1.datep);
                            switch (tid2) {
                                case BuiltinTypeConstants.XS_DATE_TYPE_ID:
                                    tvp2.getValue(tp2.datep);
                                    aOp.operateDateDate(tp1.datep, tp2.datep, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_DAY_TIME_DURATION_TYPE_ID:
                                    tvp2.getValue(tp2.intp);
                                    aOp.operateDateDTDuration(tp1.datep, tp2.intp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_YEAR_MONTH_DURATION_TYPE_ID:
                                    tvp2.getValue(tp2.intp);
                                    aOp.operateDateYMDuration(tp1.datep, tp2.intp, dOut);
                                    result.set(abvs);
                                    return;
                            }
                            break;

                        case BuiltinTypeConstants.XS_DATETIME_TYPE_ID:
                            tvp1.getValue(tp1.datetimep);
                            switch (tid2) {
                                case BuiltinTypeConstants.XS_DATETIME_TYPE_ID:
                                    tvp2.getValue(tp2.datetimep);
                                    aOp.operateDatetimeDatetime(tp1.datetimep, tp2.datetimep, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_DAY_TIME_DURATION_TYPE_ID:
                                    tvp2.getValue(tp2.intp);
                                    aOp.operateDatetimeDTDuration(tp1.datetimep, tp2.intp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_YEAR_MONTH_DURATION_TYPE_ID:
                                    tvp2.getValue(tp2.intp);
                                    aOp.operateDatetimeYMDuration(tp1.datetimep, tp2.intp, dOut);
                                    result.set(abvs);
                                    return;
                            }
                            break;

                        case BuiltinTypeConstants.XS_TIME_TYPE_ID:
                            tvp1.getValue(tp1.timep);
                            switch (tid2) {
                                case BuiltinTypeConstants.XS_TIME_TYPE_ID:
                                    tvp2.getValue(tp2.timep);
                                    aOp.operateTimeTime(tp1.timep, tp2.timep, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_DAY_TIME_DURATION_TYPE_ID:
                                    tvp2.getValue(tp2.intp);
                                    aOp.operateTimeDTDuration(tp1.timep, tp2.intp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_YEAR_MONTH_DURATION_TYPE_ID:
                                    tvp2.getValue(tp2.intp);
                                    aOp.operateTimeYMDuration(tp1.timep, tp2.intp, dOut);
                                    result.set(abvs);
                                    return;
                            }
                            break;

                        case BuiltinTypeConstants.XS_DAY_TIME_DURATION_TYPE_ID:
                            tvp1.getValue(tp1.intp);
                            switch (tid2) {
                                case BuiltinTypeConstants.XS_DECIMAL_TYPE_ID:
                                    tvp2.getValue(tp2.decp);
                                    aOp.operateDTDurationDecimal(tp1.intp, tp2.decp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_INTEGER_TYPE_ID:
                                    tvp2.getValue(tp2.longp);
                                    aOp.operateDTDurationInteger(tp1.intp, tp2.longp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_FLOAT_TYPE_ID:
                                    tvp2.getValue(tp2.floatp);
                                    aOp.operateDTDurationFloat(tp1.intp, tp2.floatp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_DOUBLE_TYPE_ID:
                                    tvp2.getValue(tp2.doublep);
                                    aOp.operateDTDurationDouble(tp1.intp, tp2.doublep, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_DATE_TYPE_ID:
                                    tvp2.getValue(tp2.datep);
                                    aOp.operateDTDurationDate(tp1.intp, tp2.datep, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_TIME_TYPE_ID:
                                    tvp2.getValue(tp2.timep);
                                    aOp.operateDTDurationTime(tp1.intp, tp2.timep, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_DATETIME_TYPE_ID:
                                    tvp2.getValue(tp2.datetimep);
                                    aOp.operateDTDurationDatetime(tp1.intp, tp2.datetimep, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_DAY_TIME_DURATION_TYPE_ID:
                                    tvp2.getValue(tp2.intp);
                                    aOp.operateDTDurationDTDuration(tp1.intp, tp2.intp, dOut);
                                    result.set(abvs);
                                    return;
                            }
                            break;

                        case BuiltinTypeConstants.XS_YEAR_MONTH_DURATION_TYPE_ID:
                            tvp1.getValue(tp1.intp);
                            switch (tid2) {
                                case BuiltinTypeConstants.XS_DECIMAL_TYPE_ID:
                                    tvp2.getValue(tp2.decp);
                                    aOp.operateYMDurationDecimal(tp1.intp, tp2.decp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_INTEGER_TYPE_ID:
                                    tvp2.getValue(tp2.longp);
                                    aOp.operateYMDurationInteger(tp1.intp, tp2.longp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_FLOAT_TYPE_ID:
                                    tvp2.getValue(tp2.floatp);
                                    aOp.operateYMDurationFloat(tp1.intp, tp2.floatp, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_DOUBLE_TYPE_ID:
                                    tvp2.getValue(tp2.doublep);
                                    aOp.operateYMDurationDouble(tp1.intp, tp2.doublep, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_DATE_TYPE_ID:
                                    tvp2.getValue(tp2.datep);
                                    aOp.operateYMDurationDate(tp1.intp, tp2.datep, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_TIME_TYPE_ID:
                                    tvp2.getValue(tp2.timep);
                                    aOp.operateYMDurationTime(tp1.intp, tp2.timep, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_DATETIME_TYPE_ID:
                                    tvp2.getValue(tp2.datetimep);
                                    aOp.operateYMDurationDatetime(tp1.intp, tp2.datetimep, dOut);
                                    result.set(abvs);
                                    return;

                                case BuiltinTypeConstants.XS_YEAR_MONTH_DURATION_TYPE_ID:
                                    tvp2.getValue(tp2.intp);
                                    aOp.operateYMDurationYMDuration(tp1.intp, tp2.intp, dOut);
                                    result.set(abvs);
                                    return;
                            }
                            break;
                    }
                } catch (SystemException se) {
                    throw se;
                } catch (Exception e) {
                    throw new SystemException(ErrorCode.SYSE0001, e);
                }
                throw new SystemException(ErrorCode.XPTY0004);
            }

            private int getBaseTypeForArithmetics(int tid) throws SystemException {
                while (true) {
                    switch (tid) {
                        case BuiltinTypeConstants.XS_STRING_TYPE_ID:
                        case BuiltinTypeConstants.XS_DECIMAL_TYPE_ID:
                        case BuiltinTypeConstants.XS_INTEGER_TYPE_ID:
                        case BuiltinTypeConstants.XS_FLOAT_TYPE_ID:
                        case BuiltinTypeConstants.XS_DOUBLE_TYPE_ID:
                        case BuiltinTypeConstants.XS_ANY_URI_TYPE_ID:
                        case BuiltinTypeConstants.XS_BOOLEAN_TYPE_ID:
                        case BuiltinTypeConstants.XS_DATE_TYPE_ID:
                        case BuiltinTypeConstants.XS_DATETIME_TYPE_ID:
                        case BuiltinTypeConstants.XS_TIME_TYPE_ID:
                        case BuiltinTypeConstants.XS_DAY_TIME_DURATION_TYPE_ID:
                        case BuiltinTypeConstants.XS_YEAR_MONTH_DURATION_TYPE_ID:
                        case BuiltinTypeConstants.XS_BASE64_BINARY_TYPE_ID:
                        case BuiltinTypeConstants.XS_HEX_BINARY_TYPE_ID:
                        case BuiltinTypeConstants.XS_QNAME_TYPE_ID:
                        case BuiltinTypeConstants.XS_G_DAY_TYPE_ID:
                        case BuiltinTypeConstants.XS_G_MONTH_DAY_TYPE_ID:
                        case BuiltinTypeConstants.XS_G_MONTH_TYPE_ID:
                        case BuiltinTypeConstants.XS_G_YEAR_MONTH_TYPE_ID:
                        case BuiltinTypeConstants.XS_G_YEAR_TYPE_ID:
                        case BuiltinTypeConstants.XS_UNTYPED_ATOMIC_TYPE_ID:
                            return tid;

                        case BuiltinTypeConstants.XS_ANY_ATOMIC_TYPE_ID:
                            throw new SystemException(ErrorCode.XPTY0004);

                        default:
                            tid = BuiltinTypeRegistry.INSTANCE.getSchemaTypeById(tid).getBaseType().getTypeId();
                    }
                }
            }
        };
    }

    private static class TypedPointables {
        BooleanPointable boolp = (BooleanPointable) BooleanPointable.FACTORY.createPointable();
        BytePointable bytep = (BytePointable) BytePointable.FACTORY.createPointable();
        ShortPointable shortp = (ShortPointable) ShortPointable.FACTORY.createPointable();
        IntegerPointable intp = (IntegerPointable) IntegerPointable.FACTORY.createPointable();
        LongPointable longp = (LongPointable) LongPointable.FACTORY.createPointable();
        FloatPointable floatp = (FloatPointable) FloatPointable.FACTORY.createPointable();
        DoublePointable doublep = (DoublePointable) DoublePointable.FACTORY.createPointable();
        UTF8StringPointable utf8sp = (UTF8StringPointable) UTF8StringPointable.FACTORY.createPointable();
        XSDecimalPointable decp = new XSDecimalPointable();
        XSDateTimePointable datetimep = new XSDateTimePointable();
        XSDatePointable datep = new XSDatePointable();
        XSTimePointable timep = new XSTimePointable();
        XSDurationPointable durationp = new XSDurationPointable();
    }

    protected abstract AbstractArithmeticOperation createArithmeticOperation();
}