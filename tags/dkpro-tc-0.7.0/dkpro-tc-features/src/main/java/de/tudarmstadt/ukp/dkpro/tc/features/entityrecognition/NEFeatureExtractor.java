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
package de.tudarmstadt.ukp.dkpro.tc.features.entityrecognition;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.Location;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.Organization;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.Person;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.tc.api.exception.TextClassificationException;
import de.tudarmstadt.ukp.dkpro.tc.api.features.DocumentFeatureExtractor;
import de.tudarmstadt.ukp.dkpro.tc.api.features.Feature;
import de.tudarmstadt.ukp.dkpro.tc.api.features.FeatureExtractorResource_ImplBase;

/**
 * Extracts the ratio of named entities per sentence
 */
public class NEFeatureExtractor
    extends FeatureExtractorResource_ImplBase
    implements DocumentFeatureExtractor
{

    @Override
    public List<Feature> extract(JCas view)
        throws TextClassificationException
    {

        List<Feature> featList = new ArrayList<Feature>();

        int numOrgaNE = JCasUtil.select(view, Organization.class).size();
        int numPersonNE = JCasUtil.select(view, Person.class).size();
        int numLocNE = JCasUtil.select(view, Location.class).size();
        int numSentences = JCasUtil.select(view, Sentence.class).size();

        if (numSentences > 0) {
            featList.add(new Feature("NrOfOrganizationEntities", numOrgaNE));
            featList.add(new Feature("NrOfPersonEntities", numPersonNE));
            featList.add(new Feature("NrOfLocationEntities", numLocNE));

            featList.add(new Feature("NrOfOrganizationEntitiesPerSent", Math
                    .round(((float) numOrgaNE / numSentences) * 100f) / 100f));
            featList.add(new Feature("NrOfPersonEntitiesPerSent", Math
                    .round(((float) numPersonNE / numSentences) * 100f) / 100f));
            featList.add(new Feature("NrOfLocationEntitiesPerSent", Math
                    .round(((float) numLocNE / numSentences) * 100f) / 100f));
        }

        return featList;
    }

}
