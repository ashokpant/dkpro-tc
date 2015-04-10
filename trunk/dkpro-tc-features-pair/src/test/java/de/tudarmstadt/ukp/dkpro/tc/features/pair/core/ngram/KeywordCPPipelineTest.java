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
package de.tudarmstadt.ukp.dkpro.tc.features.pair.core.ngram;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import de.tudarmstadt.ukp.dkpro.tc.fstore.simple.DenseFeatureStore;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.tc.api.features.Feature;
import de.tudarmstadt.ukp.dkpro.tc.core.Constants;
import de.tudarmstadt.ukp.dkpro.tc.core.io.JsonDataWriter;
import de.tudarmstadt.ukp.dkpro.tc.core.util.TaskUtils;
import de.tudarmstadt.ukp.dkpro.tc.features.ngram.base.KeywordNGramFeatureExtractorBase;
import de.tudarmstadt.ukp.dkpro.tc.features.pair.core.ngram.meta.LuceneKeywordCPMetaCollector;

public class KeywordCPPipelineTest
    extends PPipelineTestBase
{
    @Test
    public void testComboFeatures_defaults()
        throws Exception
    {
        KeywordCPPipelineTest test = new KeywordCPPipelineTest();
        test.initialize();
        test.parameters = new Object[] {
                LuceneKeywordCPFE.PARAM_USE_VIEW1_KEYWORD_NGRAMS_AS_FEATURES, false,
                LuceneKeywordCPFE.PARAM_USE_VIEW2_KEYWORD_NGRAMS_AS_FEATURES, false,
                LuceneKeywordCPFE.PARAM_USE_VIEWBLIND_KEYWORD_NGRAMS_AS_FEATURES,
                false, KeywordNGramFeatureExtractorBase.PARAM_NGRAM_KEYWORDS_FILE,
                "src/test/resources/data/keywordlist.txt",
                LuceneKeywordCPFE.PARAM_LUCENE_DIR, test.lucenePath };
        test.runPipeline();
        assertTrue(test.featureNames.first().startsWith("comboKNG"));
        assertEquals(test.featureNames.size(), 116); //this number changed historically when ComboUtils.JOINT changed.
        assertTrue(test.featureNames.contains("comboKNG_apricot_ANDapricot"));
        assertTrue(test.featureNames.contains("comboKNG_apricot_peach_ANDapricot"));
        assertTrue(test.featureNames.contains("comboKNG_apricot_peach_ANDapricot_peach"));
        assertTrue(!test.featureNames.contains("comboKNG_nectarine_ANDtrees"));

        for (String f : test.featureNames) {
            int size = f.length() - f.replace("_", "").length();
            assertTrue(size >= 2);
            assertTrue(size <= 4);
        }
    }

    @Test
    public void testComboFeatures_size1()
        throws Exception
    {
        KeywordCPPipelineTest test = new KeywordCPPipelineTest();
        test.initialize();
        test.parameters = new Object[] {
        		LuceneKeywordCPFE.PARAM_USE_VIEW1_KEYWORD_NGRAMS_AS_FEATURES, false,
        		LuceneKeywordCPFE.PARAM_USE_VIEW2_KEYWORD_NGRAMS_AS_FEATURES, false,
        		LuceneKeywordCPFE.PARAM_USE_VIEWBLIND_KEYWORD_NGRAMS_AS_FEATURES,
                false, KeywordNGramFeatureExtractorBase.PARAM_NGRAM_KEYWORDS_FILE,
                "src/test/resources/data/keywordlist.txt",
                LuceneKeywordCPFE.PARAM_LUCENE_DIR, test.lucenePath,
                LuceneKeywordCPFE.PARAM_KEYWORD_NGRAM_MAX_N_COMBO, 2 };
        test.runPipeline();
        assertTrue(test.featureNames.first().startsWith("comboKNG"));
        assertEquals(test.featureNames.size(), 24);
        assertTrue(test.featureNames.contains("comboKNG_apricot_ANDapricot"));
        assertTrue(!test.featureNames.contains("comboKNG_nectarine_ANDtrees"));
    }

    @Test
    public void testComboFeatures_size3()
        throws Exception
    {
        KeywordCPPipelineTest test = new KeywordCPPipelineTest();
        test.initialize();
        test.parameters = new Object[] {
        		LuceneKeywordCPFE.PARAM_USE_VIEW1_KEYWORD_NGRAMS_AS_FEATURES, false,
        		LuceneKeywordCPFE.PARAM_USE_VIEW2_KEYWORD_NGRAMS_AS_FEATURES, false,
        		LuceneKeywordCPFE.PARAM_USE_VIEWBLIND_KEYWORD_NGRAMS_AS_FEATURES,
                false, KeywordNGramFeatureExtractorBase.PARAM_NGRAM_KEYWORDS_FILE,
                "src/test/resources/data/keywordlist.txt",
                LuceneKeywordCPFE.PARAM_LUCENE_DIR, test.lucenePath,
                LuceneKeywordCPFE.PARAM_KEYWORD_NGRAM_MIN_N_COMBO, 6,
                LuceneKeywordCPFE.PARAM_KEYWORD_NGRAM_MAX_N_COMBO, 6 };
        test.runPipeline();
        assertTrue(test.featureNames.first().startsWith("comboKNG"));
        assertEquals(test.featureNames.size(), 10);
        assertTrue(test.featureNames
                .contains("comboKNG_apricot_peach_nectarine_ANDapricot_peach_nectarine"));
    }

    // TODO: Write a symmetry test. Note that features will be the same. Needs different dataset.

    @Test
    public void testNonBinaryFeatureValues()
        throws Exception
    {
        KeywordCPPipelineTest test = new KeywordCPPipelineTest();
        test.initialize();
        test.parameters = new Object[] {
        		LuceneKeywordCPFE.PARAM_USE_VIEW1_KEYWORD_NGRAMS_AS_FEATURES, false,
        		LuceneKeywordCPFE.PARAM_USE_VIEW2_KEYWORD_NGRAMS_AS_FEATURES, false,
        		LuceneKeywordCPFE.PARAM_USE_VIEWBLIND_KEYWORD_NGRAMS_AS_FEATURES,
                false, KeywordNGramFeatureExtractorBase.PARAM_NGRAM_KEYWORDS_FILE,
                "src/test/resources/data/keywordlist.txt",
                LuceneKeywordCPFE.PARAM_LUCENE_DIR, test.lucenePath,
                LuceneKeywordCPFE.PARAM_NGRAM_BINARY_FEATURE_VALUES_COMBO,
                false, LuceneKeywordCPFE.PARAM_KEYWORD_NGRAM_MAX_N_COMBO, 2,
                LuceneKeywordCPFE.PARAM_KEYWORD_NGRAM_SYMMETRY_COMBO, true };
        test.runPipeline();
        int four = 0;
        int three = 0;
        int two = 0;
        int one = 0;
        int zero = 0;
        for (Feature feature : test.instanceList.get(0).getFeatures()) {
            Integer value = ((Double) feature.getValue()).intValue();
            if (new Integer(value) == 4) {
                four++;
            }
            if (new Integer(value) == 3) {
                three++;
            }
            if (new Integer(value) == 2) {
                two++;
            }
            if (new Integer(value) == 1) {
                one++;
            }
            if (new Integer(value) == 0) {
                zero++;
            }
        }
        assertEquals(four, 1);
        assertEquals(three, 6);
        assertEquals(two, 9);
        assertEquals(one, 8);
        assertEquals(zero, 0);
    }

    @Override
    protected String setTestPairsLocation()
    {
        return "src/test/resources/data/keywordNgramsData.txt";
    }

    @Override
    protected void getFeatureExtractorCollector(List<Object> parameterList)
        throws ResourceInitializationException
    {
        featExtractorConnector = TaskUtils.getFeatureExtractorConnector(parameterList,
                outputPath.getAbsolutePath(), JsonDataWriter.class.getName(),
                Constants.LM_SINGLE_LABEL, Constants.FM_PAIR, DenseFeatureStore.class.getName(),
                false, false, false, false,
                LuceneKeywordCPFE.class.getName());
    }

    @Override
    protected void getMetaCollector(List<Object> parameterList)
        throws ResourceInitializationException
    {
        metaCollector = AnalysisEngineFactory.createEngineDescription(
                LuceneKeywordCPMetaCollector.class, 
                parameterList.toArray());
    }

}
