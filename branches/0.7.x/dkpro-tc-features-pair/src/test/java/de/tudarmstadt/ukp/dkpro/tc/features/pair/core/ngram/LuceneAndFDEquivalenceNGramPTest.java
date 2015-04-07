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

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.frequency.util.FrequencyDistribution;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.tc.api.features.Feature;
import de.tudarmstadt.ukp.dkpro.tc.api.features.util.FeatureUtil;
import de.tudarmstadt.ukp.dkpro.tc.core.Constants;

public class LuceneAndFDEquivalenceNGramPTest
{
    LuceneNGramPFE extractor;
    JCas jcas;
    JCas view1;
    JCas view2;

    private void initialize()
        throws Exception
    {
        AnalysisEngineDescription seg = createEngineDescription(BreakIteratorSegmenter.class);
        AnalysisEngine engine = createEngine(seg);

        AggregateBuilder builder = new AggregateBuilder();
        builder.add(seg, Constants.INITIAL_VIEW, Constants.PART_ONE);
        builder.add(seg, Constants.INITIAL_VIEW, Constants.PART_TWO);

        jcas = engine.newJCas();
        view1 = jcas.createView(Constants.PART_ONE);
        view2 = jcas.createView(Constants.PART_TWO);
        view1.setDocumentLanguage("en");
        view2.setDocumentLanguage("en");
        view1.setDocumentText("Cats eat mice.");
        view2.setDocumentText("Birds chase cats.");

        SimplePipeline.runPipeline(jcas, builder.createAggregateDescription());

        extractor = new LuceneNGramPFE();
        extractor.ngramMinN1 = 1;
        extractor.ngramMinN2 = 1;
        // extractor.ngramMinN = 1;
        extractor.ngramMaxN1 = 3;
        extractor.ngramMaxN2 = 3;
        // extractor.ngramMaxN = 3;
        extractor.useView1NgramsAsFeatures = false;
        extractor.useView2NgramsAsFeatures = false;
        extractor.useViewBlindNgramsAsFeatures = false;
        extractor.markViewBlindNgramsWithLocalView = false;
        extractor.ngramUseTopK1 = 500;
        extractor.ngramUseTopK2 = 500;
        // extractor.ngramUseTopK = 500;
        extractor.setLowerCase(true);
        extractor.setStopwords(FeatureUtil.getStopwords(null, false));
        extractor.makeTopKSet(makeSomeNgrams());
        extractor.topKSetView1 = makeSomeNgrams();
        extractor.topKSetView2 = makeSomeNgrams();
    }

    @Test
    public void CompareOldAndNewPairFETest()
        throws Exception
    {
        initialize();
        extractor.ngramMinN1 = 1;
        extractor.ngramMaxN1 = 3;
        extractor.ngramMinN2 = 1;
        extractor.ngramMaxN2 = 3;
        extractor.useView1NgramsAsFeatures = true;
        extractor.useView2NgramsAsFeatures = true;
        extractor.setLowerCase(true);

        List<Feature> newFeatures = extractor.extract(jcas.getView(Constants.PART_ONE),
                jcas.getView(Constants.PART_TWO));
        FrequencyDistribution<String> view1features = new FrequencyDistribution<String>();
        FrequencyDistribution<String> view2features = new FrequencyDistribution<String>();

        for (Feature f : newFeatures) {
            if (f.getName().startsWith("view1NG_")) {
                view1features.addSample(f.getName().replace("view1NG_", ""), 1);
            }
            else {
                view2features.addSample(f.getName().replace("view2NG_", ""), 1);
            }
        }

        FrequencyDistributionNGramPFE oldExtractor = new FrequencyDistributionNGramPFE();
        // oldExtractor.ngramMinN = 1;
        // oldExtractor.ngramMaxN = 3;

        oldExtractor.setStopwords(FeatureUtil.getStopwords(null, false));
        oldExtractor.makeTopKSet(makeSomeNgrams());
        List<Feature> oldFeatures = oldExtractor.extract(view1, view2);

        for (Feature f : oldFeatures) {
            if (f.getName().startsWith("ngrams_PART_ONE_")) {
                view1features.addSample(f.getName().replace("ngrams_PART_ONE_", ""), 1);
            }
            else {
                view2features.addSample(f.getName().replace("ngrams_PART_TWO_", ""), 1);
            }
        }
        assertEquals(view1features.getKeys().size(), 7);
        for (String sample : view1features.getKeys()) {
            assertTrue(view1features.getCount(sample) == 2);
        }
        assertEquals(view2features.getKeys().size(), 7);
        for (String sample : view2features.getKeys()) {
            assertTrue(view1features.getCount(sample) == 2);
        }
    }

    /**
     * Makes a FD of "filtered ngrams from whole corpus." Not really filtered by params in this test
     * suite. Each of these will always be a final feature; but have values according to whether
     * they also occur in a set of ngrams for the view/jcas in question, which <b>has</b> been
     * filtered by params in this test suite.
     * 
     * @return ngrams to represent a set of ngrams from the whole corpus
     */
    private static FrequencyDistribution<String> makeSomeNgrams()
    {
        FrequencyDistribution<String> fd = new FrequencyDistribution<String>();
        fd.addSample("cats", 2);
        fd.addSample("birds", 1);
        fd.addSample("dogs", 4);
        fd.addSample("cats_eat", 5);
        fd.addSample("cats_eat_mice", 1);
        fd.addSample("birds_chase_cats", 2);
        fd.addSample("Birds_chase_cats", 2);
        return fd;
    }

}