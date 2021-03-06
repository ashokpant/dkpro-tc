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
package de.tudarmstadt.ukp.dkpro.tc.features.style;

import static de.tudarmstadt.ukp.dkpro.tc.testing.FeatureTestUtil.assertFeature;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.tc.api.features.Feature;

public class AdjectiveEndingFeatureExtractorTest
{
    @Test
    public void adjectiveEndingFeatureExtractorTest()
        throws Exception
    {
        AnalysisEngineDescription desc = createEngineDescription(
                createEngineDescription(BreakIteratorSegmenter.class),
                createEngineDescription(OpenNlpPosTagger.class, OpenNlpPosTagger.PARAM_LANGUAGE,
                        "en"));
        AnalysisEngine engine = createEngine(desc);

        JCas jcas = engine.newJCas();
        jcas.setDocumentLanguage("en");
        jcas.setDocumentText("Lovable phenomenal beautiful incredible fantastic gorgeous positive nice good mainly harmless.");
        engine.process(jcas);

        AdjectiveEndingFeatureExtractor extractor = new AdjectiveEndingFeatureExtractor();
        List<Feature> features = extractor.extract(jcas);

        Assert.assertEquals(9, features.size());

        Iterator<Feature> iter = features.iterator();
        assertFeature(AdjectiveEndingFeatureExtractor.FN_ENDING1, 10.0, iter.next());
        assertFeature(AdjectiveEndingFeatureExtractor.FN_ENDING2, 10.0, iter.next());
        assertFeature(AdjectiveEndingFeatureExtractor.FN_ENDING3, 10.0, iter.next());
        assertFeature(AdjectiveEndingFeatureExtractor.FN_ENDING4, 10.0, iter.next());
        assertFeature(AdjectiveEndingFeatureExtractor.FN_ENDING5, 10.0, iter.next());
        assertFeature(AdjectiveEndingFeatureExtractor.FN_ENDING6, 10.0, iter.next());
        assertFeature(AdjectiveEndingFeatureExtractor.FN_ENDING7, 10.0, iter.next());
        assertFeature(AdjectiveEndingFeatureExtractor.FN_ENDING8, 10.0, iter.next()); // 10% of
                                                                                      // adjectives
                                                                                      // has this
                                                                                      // ending
        assertFeature(AdjectiveEndingFeatureExtractor.FN_ENDING9, 100.0, iter.next()); // 100% of
                                                                                       // adverbs
                                                                                       // has this
                                                                                       // ending
    }
}