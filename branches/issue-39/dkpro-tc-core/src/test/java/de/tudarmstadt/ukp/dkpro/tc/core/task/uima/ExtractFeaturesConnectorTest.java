/*******************************************************************************
 * Copyright 2014
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.dkpro.tc.core.task.uima;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createExternalResourceDescription;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.resource.ExternalResourceDescription;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.Gson;

import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.tc.api.features.FeatureStore;
import de.tudarmstadt.ukp.dkpro.tc.core.Constants;
import de.tudarmstadt.ukp.dkpro.tc.core.feature.NoopFeatureExtractor;
import de.tudarmstadt.ukp.dkpro.tc.core.io.JsonDataWriter;
import de.tudarmstadt.ukp.dkpro.tc.core.io.TestReaderMultiLabel;
import de.tudarmstadt.ukp.dkpro.tc.core.io.TestReaderRegression;
import de.tudarmstadt.ukp.dkpro.tc.core.io.TestReaderSingleLabel;
import de.tudarmstadt.ukp.dkpro.tc.core.util.TaskUtils;
import de.tudarmstadt.ukp.dkpro.tc.fstore.simple.DenseFeatureStore;
import de.tudarmstadt.ukp.dkpro.tc.fstore.simple.SparseFeatureStore;

@RunWith(Parameterized.class)
public class ExtractFeaturesConnectorTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { DenseFeatureStore.class }, {SparseFeatureStore.class}
        });
    }
    
    private Class<? extends FeatureStore> featureStoreClass;

	private final List<ExternalResourceDescription> NOOP = asList(createExternalResourceDescription(NoopFeatureExtractor.class));
    
    public ExtractFeaturesConnectorTest(Class<? extends FeatureStore> featureStoreClass)
    {
        this.featureStoreClass = featureStoreClass;
    }

    @Test
    public void extractFeaturesConnectorSingleLabelTest()
            throws Exception
    {

        File outputPath = folder.newFolder();

//        // we do not need parameters here, but in case we do :)
//        Object[] parameters = new Object[] {
//                // "NAME", "VALUE"
//        };
//        List<Object> parameterList = new ArrayList<Object>(Arrays.asList(parameters));

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                TestReaderSingleLabel.class, TestReaderSingleLabel.PARAM_SOURCE_LOCATION,
                "src/test/resources/data/*.txt");

        AnalysisEngineDescription segmenter = AnalysisEngineFactory
                .createEngineDescription(BreakIteratorSegmenter.class);

        

        
        AnalysisEngineDescription featExtractorConnector = TaskUtils.getFeatureExtractorConnector(
        		outputPath.getAbsolutePath(), JsonDataWriter.class.getName(),
                Constants.LM_SINGLE_LABEL, Constants.FM_DOCUMENT,
                featureStoreClass.getName(), true, false, false, NOOP);

        SimplePipeline.runPipeline(reader, segmenter, featExtractorConnector);

        Gson gson = new Gson();
        FeatureStore fs = gson.fromJson(
                FileUtils.readFileToString(new File(outputPath, JsonDataWriter.JSON_FILE_NAME)),
                featureStoreClass);
        assertEquals(2, fs.getNumberOfInstances());
        assertEquals(1, fs.getUniqueOutcomes().size());

        System.out.println(FileUtils.readFileToString(new File(outputPath,
                JsonDataWriter.JSON_FILE_NAME)));
    }

    @Test
    public void extractFeaturesConnectorMultiLabelTest()
            throws Exception
    {

        File outputPath = folder.newFolder();

        // we do not need parameters here, but in case we do :)
//        Object[] parameters = new Object[] {
//                // "NAME", "VALUE"
//        };
//        List<Object> parameterList = new ArrayList<Object>(Arrays.asList(parameters));

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                TestReaderMultiLabel.class, TestReaderMultiLabel.PARAM_SOURCE_LOCATION,
                "src/test/resources/data/*.txt");

        AnalysisEngineDescription segmenter = AnalysisEngineFactory
                .createEngineDescription(BreakIteratorSegmenter.class);

        AnalysisEngineDescription featExtractorConnector = TaskUtils.getFeatureExtractorConnector(
                outputPath.getAbsolutePath(), JsonDataWriter.class.getName(),
                Constants.LM_MULTI_LABEL, Constants.FM_DOCUMENT, featureStoreClass.getName(),
                true, false, false, NOOP);

        SimplePipeline.runPipeline(reader, segmenter, featExtractorConnector);

        Gson gson = new Gson();
        FeatureStore fs = gson.fromJson(
                FileUtils.readFileToString(new File(outputPath, JsonDataWriter.JSON_FILE_NAME)),
                featureStoreClass);
        assertEquals(2, fs.getNumberOfInstances());
        assertEquals(3, fs.getUniqueOutcomes().size());

        System.out.println(FileUtils.readFileToString(new File(outputPath,
                JsonDataWriter.JSON_FILE_NAME)));
    }

    @Test
    public void extractFeaturesConnectorRegressionTest()
            throws Exception
    {

        File outputPath = folder.newFolder();

//        // we do not need parameters here, but in case we do :)
//        Object[] parameters = new Object[] {
//                // "NAME", "VALUE"
//        };
//        List<Object> parameterList = new ArrayList<Object>(Arrays.asList(parameters));

        CollectionReaderDescription reader = CollectionReaderFactory.createReaderDescription(
                TestReaderRegression.class, TestReaderRegression.PARAM_SOURCE_LOCATION,
                "src/test/resources/data/*.txt");

        AnalysisEngineDescription segmenter = AnalysisEngineFactory
                .createEngineDescription(BreakIteratorSegmenter.class);

        AnalysisEngineDescription featExtractorConnector = TaskUtils.getFeatureExtractorConnector(
                outputPath.getAbsolutePath(), JsonDataWriter.class.getName(),
                Constants.LM_REGRESSION, Constants.FM_DOCUMENT, featureStoreClass.getName(),
                true, false, false, NOOP);

        SimplePipeline.runPipeline(reader, segmenter, featExtractorConnector);

        Gson gson = new Gson();
        FeatureStore fs = gson.fromJson(
                FileUtils.readFileToString(new File(outputPath, JsonDataWriter.JSON_FILE_NAME)),
                featureStoreClass);
        assertEquals(2, fs.getNumberOfInstances());
        assertEquals(1, fs.getUniqueOutcomes().size());
        assertEquals("0.45", fs.getUniqueOutcomes().first());

        System.out.println(FileUtils.readFileToString(new File(outputPath,
                JsonDataWriter.JSON_FILE_NAME)));
    }
}
