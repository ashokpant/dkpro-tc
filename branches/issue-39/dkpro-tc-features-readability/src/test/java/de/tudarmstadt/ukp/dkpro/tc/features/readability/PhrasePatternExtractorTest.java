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

package de.tudarmstadt.ukp.dkpro.tc.features.readability;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.jcas.JCas;
import org.junit.Assert;
import org.junit.Ignore;

import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpChunker;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpSegmenter;
import de.tudarmstadt.ukp.dkpro.tc.api.features.Feature;

public class PhrasePatternExtractorTest
{
    @Ignore
    public void testPhrasePatternExtractor()
        throws Exception
    {
        String text = FileUtils
                .readFileToString(new File("src/test/resources/test_document_en.txt"));

        AnalysisEngineDescription desc = createEngineDescription(
                createEngineDescription(OpenNlpSegmenter.class),
                createEngineDescription(OpenNlpPosTagger.class),
                createEngineDescription(OpenNlpChunker.class));
        AnalysisEngine engine = createEngine(desc);
        JCas jcas = engine.newJCas();
        jcas.setDocumentLanguage("en");
        jcas.setDocumentText(text);
        engine.process(jcas);

        PhrasePatternExtractor extractor = new PhrasePatternExtractor();
        List<Feature> features = extractor.extract(jcas);

        Assert.assertEquals(6, features.size());
        // System.out.println(features);
        Assert.assertEquals((double) features.get(0).getValue(), 4.2, 0.1);
        Assert.assertEquals((double) features.get(1).getValue(), 1.6, 0.1);
        Assert.assertEquals((double) features.get(2).getValue(), 0.9, 0.1);
        Assert.assertEquals((double) features.get(3).getValue(), 3.7, 0.1);
        Assert.assertEquals((double) features.get(4).getValue(), 0.4, 0.1);
        Assert.assertEquals((double) features.get(5).getValue(), 0.4, 0.1);

    }
}