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
package org.apache.vxquery.metadata;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.hyracks.api.client.NodeControllerInfo;
import org.apache.hyracks.api.comm.IFrame;
import org.apache.hyracks.api.comm.IFrameFieldAppender;
import org.apache.hyracks.api.comm.VSizeFrame;
import org.apache.hyracks.api.context.IHyracksTaskContext;
import org.apache.hyracks.api.dataflow.IOperatorNodePushable;
import org.apache.hyracks.api.dataflow.value.IRecordDescriptorProvider;
import org.apache.hyracks.api.dataflow.value.RecordDescriptor;
import org.apache.hyracks.api.exceptions.HyracksDataException;
import org.apache.hyracks.api.job.IOperatorDescriptorRegistry;
import org.apache.hyracks.data.std.api.IPointable;
import org.apache.hyracks.data.std.primitive.UTF8StringPointable;
import org.apache.hyracks.data.std.util.ArrayBackedValueStorage;
import org.apache.hyracks.dataflow.common.comm.io.FrameFixedFieldTupleAppender;
import org.apache.hyracks.dataflow.common.comm.io.FrameTupleAccessor;
import org.apache.hyracks.dataflow.common.comm.util.ByteBufferInputStream;
import org.apache.hyracks.dataflow.std.base.AbstractSingleActivityOperatorDescriptor;
import org.apache.hyracks.dataflow.std.base.AbstractUnaryInputUnaryOutputOperatorNodePushable;
import org.apache.vxquery.compiler.rewriter.rules.AbstractCollectionRule;
import org.apache.vxquery.context.DynamicContext;
import org.apache.vxquery.datamodel.accessors.TaggedValuePointable;
import org.apache.vxquery.datamodel.builders.sequence.SequenceBuilder;
import org.apache.vxquery.datamodel.values.XDMConstants;
import org.apache.vxquery.exceptions.ErrorCode;
import org.apache.vxquery.exceptions.SystemException;
import org.apache.vxquery.functions.BuiltinFunctions;
import org.apache.vxquery.hdfs2.HDFSFunctions;
import org.apache.vxquery.runtime.functions.index.IndexConstructorUtil;
import org.apache.vxquery.runtime.functions.index.updateIndex.IndexUpdater;
import org.apache.vxquery.xmlparser.ITreeNodeIdProvider;
import org.apache.vxquery.xmlparser.TreeNodeIdProvider;
import org.apache.vxquery.xmlparser.XMLParser;

import javax.xml.bind.JAXBException;

public class VXQueryIndexingOperatorDescriptor extends AbstractSingleActivityOperatorDescriptor {
    private static final long serialVersionUID = 1L;
    private short dataSourceId;
    private short totalDataSources;
    private String[] collectionPartitions;
    private String[] indexPartitions;
    private List<Integer> childSeq;
    protected static final Logger LOGGER = Logger.getLogger(VXQueryCollectionOperatorDescriptor.class.getName());
    private HDFSFunctions hdfs;
    private String tag;
    private final String START_TAG = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
    private final String hdfsConf;
    private final Map<String, NodeControllerInfo> nodeControllerInfos;

    public VXQueryIndexingOperatorDescriptor(IOperatorDescriptorRegistry spec, VXQueryIndexingDataSource ds,
            RecordDescriptor rDesc, String hdfsConf, Map<String, NodeControllerInfo> nodeControllerInfos) {
        super(spec, 1, 1);
        collectionPartitions = ds.getCollectionPartitions();
        indexPartitions = ds.getIndexPartitions();
        dataSourceId = (short) ds.getDataSourceId();
        totalDataSources = (short) ds.getTotalDataSources();
        recordDescriptors[0] = rDesc;
        this.childSeq = ds.getChildSeq();
        this.tag = ds.getTag();
        this.hdfsConf = hdfsConf;
        this.nodeControllerInfos = nodeControllerInfos;
    }

    @Override
    public IOperatorNodePushable createPushRuntime(IHyracksTaskContext ctx,
            IRecordDescriptorProvider recordDescProvider, int partition, int nPartitions) throws HyracksDataException {
        final FrameTupleAccessor fta = new FrameTupleAccessor(
                recordDescProvider.getInputRecordDescriptor(getActivityId(), 0));
        final int fieldOutputCount = recordDescProvider.getOutputRecordDescriptor(getActivityId(), 0).getFieldCount();
        final IFrame frame = new VSizeFrame(ctx);
        final IFrameFieldAppender appender = new FrameFixedFieldTupleAppender(fieldOutputCount);
        final short partitionId = (short) ctx.getTaskAttemptId().getTaskId().getPartition();
        final ITreeNodeIdProvider nodeIdProvider = new TreeNodeIdProvider(partitionId, dataSourceId, totalDataSources);
        final String nodeId = ctx.getJobletContext().getApplicationContext().getNodeId();
        final DynamicContext dCtx = (DynamicContext) ctx.getJobletContext().getGlobalJobData();

        final String collectionName = collectionPartitions[partition % collectionPartitions.length];
        final String indexName = indexPartitions[partition % indexPartitions.length];
        final XMLParser parser = new XMLParser(false, nodeIdProvider, nodeId, appender, childSeq,
                dCtx.getStaticContext());

        return new AbstractUnaryInputUnaryOutputOperatorNodePushable() {
            @Override
            public void open() throws HyracksDataException {
                appender.reset(frame, true);
                writer.open();
                hdfs = new HDFSFunctions(nodeControllerInfos, hdfsConf);
            }

            @Override
            public void nextFrame(ByteBuffer buffer) throws HyracksDataException {
                fta.reset(buffer);
                String collectionModifiedName = collectionName.replace("${nodeId}", nodeId);
                String indexModifiedName = indexName.replace("${nodeId}", nodeId);
                if (!collectionModifiedName.contains("hdfs:/")) {
                    File collectionDirectory = new File(collectionModifiedName);
                    //check if directory is in the local file system
                    if (collectionDirectory.exists()) {
                        // Go through each tuple.
                        if (collectionDirectory.isDirectory()) {
                            for (int tupleIndex = 0; tupleIndex < fta.getTupleCount(); ++tupleIndex) {
                                Iterator<File> it = FileUtils.iterateFiles(collectionDirectory,
                                        new VXQueryIOFileFilter(), TrueFileFilter.INSTANCE);
                                while (it.hasNext()) {
                                    File xmlDocument = it.next();
                                    if (LOGGER.isLoggable(Level.FINE)) {
                                        LOGGER.fine("Starting to read XML document: " + xmlDocument.getAbsolutePath());
                                    }
                                    IPointable result = new TaggedValuePointable();

                                    final UTF8StringPointable stringp =
                                            (UTF8StringPointable) UTF8StringPointable.FACTORY.createPointable();
                                    final TaggedValuePointable nodep =
                                            (TaggedValuePointable) TaggedValuePointable.FACTORY.createPointable();

                                    final ByteBufferInputStream bbis = new ByteBufferInputStream();
                                    final DataInputStream di = new DataInputStream(bbis);
                                    final SequenceBuilder sb = new SequenceBuilder();
                                    final ArrayBackedValueStorage abvs = new ArrayBackedValueStorage();
                                    final ArrayBackedValueStorage abvsFileNode = new ArrayBackedValueStorage();

                                    try {

                                        if (AbstractCollectionRule.functionCall.getFunctionIdentifier().equals
                                                (BuiltinFunctions.FN_BUILD_INDEX_ON_COLLECTION_2.getFunctionIdentifier())) {
                                            IndexConstructorUtil.evaluate(collectionModifiedName, indexModifiedName, result,
                                                    stringp, bbis, di, sb, abvs, nodeIdProvider,
                                                    abvsFileNode, nodep, false, nodeId);
                                        } else if (AbstractCollectionRule.functionCall.getFunctionIdentifier().equals
                                                (BuiltinFunctions.FN_UPDATE_INDEX_1.getFunctionIdentifier())){
                                            IndexUpdater updater = new IndexUpdater(indexModifiedName, result, stringp, bbis, di, sb,
                                                    abvs, nodeIdProvider,
                                                    abvsFileNode, nodep, nodeId);
                                            updater.setup();
                                            updater.updateIndex();
                                            updater.updateMetadataFile();
                                            updater.exit();
                                            XDMConstants.setTrue(result);
                                        } else if (AbstractCollectionRule.functionCall.getFunctionIdentifier().equals
                                                (BuiltinFunctions.FN_DELETE_INDEX_1.getFunctionIdentifier())) {
                                            IndexUpdater updater = new IndexUpdater(indexModifiedName, result, stringp, bbis, di, sb,
                                                    abvs, nodeIdProvider,
                                                    abvsFileNode, nodep, nodeId);
                                            updater.setup();
                                            updater.deleteAllIndexes();
                                        } else {}

                                    } catch (SystemException | JAXBException | NoSuchAlgorithmException | IOException e) {
                                        try {
                                            throw new SystemException(ErrorCode.SYSE0001, e);
                                        } catch (SystemException e1) {
                                            e1.printStackTrace();
                                        }
                                    }
                                    parser.parseElements(xmlDocument, writer, tupleIndex);
                                }
                            }
                        } else {
                            throw new HyracksDataException(
                                    "Invalid directory parameter (" + nodeId + ":" + collectionDirectory.getAbsolutePath() + ") passed to collection.");
                        }
                    }
                }
            }

            @Override
            public void fail() throws HyracksDataException {
                writer.fail();
            }

            @Override
            public void close() throws HyracksDataException {
                // Check if needed?
                if (appender.getTupleCount() > 0) {
                    appender.flush(writer, true);
                }
                writer.close();
            }
        };
    }
}
