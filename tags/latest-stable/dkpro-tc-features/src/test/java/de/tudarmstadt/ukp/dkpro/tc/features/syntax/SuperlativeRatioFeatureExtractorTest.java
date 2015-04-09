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
package de.tudarmstadt.ukp.dkpro.tc.features.syntax;

import static de.tudarmstadt.ukp.dkpro.tc.features.syntax.SuperlativeRatioFeatureExtractor.FN_SUPERLATIVE_RATIO_ADJ;
import static de.tudarmstadt.ukp.dkpro.tc.features.syntax.SuperlativeRatioFeatureExtractor.FN_SUPERLATIVE_RATIO_ADV;
import static de.tudarmstadt.ukp.dkpro.tc.testing.FeatureTestUtil.assertFeature;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.util.List;

import junit.framework.Assert;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.tc.api.features.Feature;

public class SuperlativeRatioFeatureExtractorTest
{
    @Test
    public void posContextFeatureExtractorTest()
        throws Exception
    {
        AnalysisEngineDescription desc = createEngineDescription(
                createEngineDescription(BreakIteratorSegmenter.class),
                createEngineDescription(OpenNlpPosTagger.class, OpenNlpPosTagger.PARAM_LANGUAGE,
                        "en"));
        AnalysisEngine engine = createEngine(desc);

        JCas jcas = engine.newJCas();
        jcas.setDocumentLanguage("en");
        jcas.setDocumentText("This is a normal test. This is the best, biggest, and greatest test ever.");
        engine.process(jcas);

        SuperlativeRatioFeatureExtractor extractor = new SuperlativeRatioFeatureExtractor();
        List<Feature> features = extractor.extract(jcas);

        Assert.assertEquals(2, features.size());

        for (Feature feature : features) {
            if (feature.getName().equals(FN_SUPERLATIVE_RATIO_ADJ)) {
                assertFeature(FN_SUPERLATIVE_RATIO_ADJ, 0.75, feature);
            }
            else if (feature.getName().equals(FN_SUPERLATIVE_RATIO_ADV)) {
                assertFeature(FN_SUPERLATIVE_RATIO_ADV, 0.0, feature);
            }
        }
    }
}