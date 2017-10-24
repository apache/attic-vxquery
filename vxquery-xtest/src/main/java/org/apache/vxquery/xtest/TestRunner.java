/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.vxquery.xtest;

import static org.apache.vxquery.rest.Constants.HttpHeaderValues.CONTENT_TYPE_JSON;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
//<<<<<<< HEAD
import java.nio.charset.StandardCharsets;
//=======
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.List;
//>>>>>>> tillw/hyracks0.3.2
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//<<<<<<< HEAD
import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.vxquery.app.util.RestUtils;
//=======
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hyracks.api.client.IHyracksClientConnection;
import org.apache.hyracks.api.client.NodeControllerInfo;
import org.apache.hyracks.api.comm.IFrame;
import org.apache.hyracks.api.comm.IFrameTupleAccessor;
import org.apache.hyracks.api.comm.VSizeFrame;
import org.apache.hyracks.api.dataset.DatasetJobRecord;
import org.apache.hyracks.api.dataset.IHyracksDataset;
import org.apache.hyracks.api.dataset.IHyracksDatasetReader;
import org.apache.hyracks.api.dataset.ResultSetId;
import org.apache.hyracks.api.exceptions.HyracksException;
import org.apache.hyracks.api.job.JobFlag;
import org.apache.hyracks.api.job.JobId;
import org.apache.hyracks.api.job.JobSpecification;
import org.apache.hyracks.control.nc.resources.memory.FrameManager;
import org.apache.hyracks.dataflow.common.comm.io.ResultFrameTupleAccessor;
import org.apache.vxquery.compiler.CompilerControlBlock;
import org.apache.vxquery.compiler.algebricks.VXQueryGlobalDataFactory;
import org.apache.vxquery.context.DynamicContext;
import org.apache.vxquery.context.DynamicContextImpl;
import org.apache.vxquery.context.RootStaticContextImpl;
import org.apache.vxquery.context.StaticContextImpl;
import org.apache.vxquery.datamodel.accessors.atomic.XSDateTimePointable;
//>>>>>>> tillw/hyracks0.3.2
import org.apache.vxquery.exceptions.ErrorCode;
import org.apache.vxquery.exceptions.SystemException;
import org.apache.vxquery.rest.request.QueryRequest;
import org.apache.vxquery.rest.response.APIResponse;
import org.apache.vxquery.rest.response.ErrorResponse;
import org.apache.vxquery.rest.response.SyncQueryResponse;
import org.codehaus.jackson.map.ObjectMapper;

public class TestRunner {

    private static final Pattern EMBEDDED_SYSERROR_PATTERN = Pattern.compile("(\\p{javaUpperCase}{4}\\d{4})");

    private XTestOptions opts;

    public TestRunner(XTestOptions opts) throws UnknownHostException {
        this.opts = opts;
    }

    public void open() throws Exception {
    }

    public TestCaseResult run(final TestCase testCase) {
        TestCaseResult res = new TestCaseResult(testCase);
        runQueries(testCase, res);
        return res;
    }

    public void runQuery(TestCase testCase, TestCaseResult res) {
        if (opts.verbose) {
            System.err.println("Starting " + testCase.getXQueryDisplayName());
        }

        long start = System.currentTimeMillis();

        try {
            String query = FileUtils.readFileToString(testCase.getXQueryFile(), "UTF-8");

            if (opts.showQuery) {
                System.err.println("***Query for " + testCase.getXQueryDisplayName() + ": ");
                System.err.println(query);
            }

//<<<<<<< HEAD
            QueryRequest request = createQueryRequest(opts, query);
            APIResponse response = sendQueryRequest(request, testCase.getSourceFileMap());
            if (response instanceof SyncQueryResponse) {
                res.result = ((SyncQueryResponse) response).getResults();
            } else {
                System.err.println("Error response: Failure when running the query");
                ErrorResponse errorResponse = (ErrorResponse) response;
                Matcher m = EMBEDDED_SYSERROR_PATTERN.matcher(errorResponse.getError().getMessage());

                Exception e = new RuntimeException("Failed to run the query");
                if (m.find()) {
                    String eCode = m.group(1);
                    throw new SystemException(ErrorCode.valueOf(eCode), e);
                } else {
                    throw e;
//=======
                XMLQueryCompiler compiler = new XMLQueryCompiler(listener, nodeControllerInfos, opts.frameSize,
                        opts.hdfsConf);
                Reader in = new InputStreamReader(new FileInputStream(testCase.getXQueryFile()), "UTF-8");
                CompilerControlBlock ccb = new CompilerControlBlock(
                        new StaticContextImpl(RootStaticContextImpl.INSTANCE),
                        new ResultSetId(testCase.getXQueryDisplayName().hashCode()), testCase.getSourceFileMap());
                compiler.compile(testCase.getXQueryDisplayName(), in, ccb, opts.optimizationLevel, collectionList);
                JobSpecification spec = compiler.getModule().getHyracksJobSpecification();
                in.close();

                DynamicContext dCtx = new DynamicContextImpl(compiler.getModule().getModuleContext());

                if (opts.timezone != null) {
                    final int dtLen = XSDateTimePointable.TYPE_TRAITS.getFixedLength();
                    byte[] currentDateTime = new byte[dtLen];
                    XSDateTimePointable datetimep = new XSDateTimePointable();
                    datetimep.set(currentDateTime, 0, dtLen);
                    datetimep.setCurrentDateTime(Calendar.getInstance(TimeZone.getTimeZone(opts.timezone)));
                    dCtx.setCurrentDateTime(datetimep);
                }

                spec.setGlobalJobDataFactory(new VXQueryGlobalDataFactory(dCtx.createFactory()));

                spec.setMaxReattempts(0);
                JobId jobId = hcc.startJob(spec, EnumSet.of(JobFlag.PROFILE_RUNTIME));

                FrameManager resultDisplayFrameMgr = new FrameManager(spec.getFrameSize());
                IFrame frame = new VSizeFrame(resultDisplayFrameMgr);
                IHyracksDatasetReader reader = hds.createReader(jobId, ccb.getResultSetId());
                // TODO(tillw) remove this loop once the IHyracksDatasetReader reliably returns the correct exception
                while (reader.getResultStatus().getState() == DatasetJobRecord.State.RUNNING) {
                    Thread.sleep(1);
                }
                IFrameTupleAccessor frameTupleAccessor = new ResultFrameTupleAccessor();
                res.result = "";
                while (reader.read(frame) > 0) {
                    res.result += ResultUtils.getStringFromBuffer(frame.getBuffer(), frameTupleAccessor);
                    frame.getBuffer().clear();
                }
                res.result.trim();
                hcc.waitForCompletion(jobId);
            } catch (HyracksException e) {
                Throwable t = e;
                while (t.getCause() != null) {
                    t = t.getCause();
                }
                final String message = t.getMessage();
                if (message != null) {
                    Matcher m = EMBEDDED_SYSERROR_PATTERN.matcher(message);
                    if (m.find()) {
                        String eCode = m.group(1);
                        throw new SystemException(ErrorCode.valueOf(eCode), e);
                    }
//>>>>>>> tillw/hyracks0.3.2
                }
            }
        } catch (Throwable e) {
            res.error = e;
        } finally {
            try {
                res.compare();
            } catch (Exception e) {
                System.err.println("Framework error");
                e.printStackTrace();
            }
            long end = System.currentTimeMillis();
            res.time = end - start;
        }

        if (opts.showResult) {
            if (res.result == null) {
                System.err.println("***Error: ");
                System.err.println("Message: " + res.error.getMessage());
                res.error.printStackTrace();
            } else {
                System.err.println("***Result: ");
                System.err.println(res.result);
            }
        }
    }

    private static QueryRequest createQueryRequest(XTestOptions opts, String query) {
        QueryRequest request = new QueryRequest(query);
        request.setCompileOnly(opts.compileOnly);
        request.setOptimization(opts.optimizationLevel);
        request.setFrameSize(opts.frameSize);
        request.setShowAbstractSyntaxTree(opts.showAST);
        request.setShowTranslatedExpressionTree(opts.showTET);
        request.setShowOptimizedExpressionTree(opts.showOET);
        request.setShowRuntimePlan(opts.showRP);
        request.setAsync(false);

        return request;
    }

    private static APIResponse sendQueryRequest(QueryRequest request, Map<String, File> sourceFileMap)
            throws IOException, URISyntaxException {

        URI uri = RestUtils.buildQueryURI(request, TestClusterUtil.localClusterUtil.getIpAddress(),
                TestClusterUtil.localClusterUtil.getRestPort());
        CloseableHttpClient httpClient = HttpClients.custom().build();

        try {
            HttpPost httpRequest = new HttpPost(uri);
            httpRequest.setHeader(HttpHeaders.ACCEPT, CONTENT_TYPE_JSON);

            ObjectMapper mapper = new ObjectMapper();
            String fileMap = mapper.writeValueAsString(sourceFileMap);
            httpRequest.setEntity(new StringEntity(fileMap, StandardCharsets.UTF_8));

            try (CloseableHttpResponse httpResponse = httpClient.execute(httpRequest)) {
                HttpEntity entity = httpResponse.getEntity();
                String response = RestUtils.readEntity(entity);
                if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    return RestUtils.mapEntity(response, SyncQueryResponse.class, CONTENT_TYPE_JSON);
                } else {
                    return RestUtils.mapEntity(response, ErrorResponse.class, CONTENT_TYPE_JSON);
                }
            } catch (IOException e) {
                System.err.println("Error occurred when reading entity: " + e.getMessage());
            } catch (JAXBException e) {
                System.err.println("Error occurred when mapping query response: " + e.getMessage());
            }
        } finally {
            HttpClientUtils.closeQuietly(httpClient);
        }

        return null;
    }

    public void runQueries(TestCase testCase, TestCaseResult res) {
        runQuery(testCase, res);
    }

    public void close() throws Exception {
        // TODO add a close statement for the hyracks connection.
    }
}
