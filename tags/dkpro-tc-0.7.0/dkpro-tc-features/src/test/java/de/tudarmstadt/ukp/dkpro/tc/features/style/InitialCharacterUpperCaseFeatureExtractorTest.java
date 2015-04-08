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

import java.util.List;

import junit.framework.Assert;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.tc.api.features.Feature;
import de.tudarmstadt.ukp.dkpro.tc.api.features.util.FeatureUtil;
import de.tudarmstadt.ukp.dkpro.tc.api.type.TextClassificationUnit;

public class InitialCharacterUpperCaseFeatureExtractorTest
{

    @Test
    public void initialCharacterUpperCaseTest()
        throws Exception
    {

        AnalysisEngineDescription desc = createEngineDescription(BreakIteratorSegmenter.class);

        AnalysisEngine engine = createEngine(desc);
        JCas jcas = engine.newJCas();
        jcas.setDocumentLanguage("en");
        jcas.setDocumentText("It is a very unusual test");

        engine.process(jcas);
        
        InitialCharacterUpperCaseUFE extractor = FeatureUtil.createResource(
        		InitialCharacterUpperCaseUFE.class);


        TextClassificationUnit unit1 = new TextClassificationUnit(jcas);
        unit1.setBegin(0);
        unit1.setEnd(2);

        TextClassificationUnit unit2 = new TextClassificationUnit(jcas);
        unit2.setBegin(3);
        unit2.setEnd(5);

    	List<Feature> features1 = extractor.extract(jcas, unit1);

        Assert.assertEquals(1, features1.size());
        for (Feature feature : features1) {
            assertFeature(InitialCharacterUpperCaseUFE.INITIAL_CH_UPPER_CASE, true, feature);
        }

        List<Feature> features2 = extractor.extract(jcas, unit2);
        Assert.assertEquals(1, features2.size());
        for (Feature feature : features2) {
            assertFeature(InitialCharacterUpperCaseUFE.INITIAL_CH_UPPER_CASE, false, feature);
        }
    }
}