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

import static org.apache.uima.fit.util.JCasUtil.select;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.ADJ;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.ADV;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.ART;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.N;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.O;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.PP;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.PR;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.V;
import de.tudarmstadt.ukp.dkpro.tc.api.exception.TextClassificationException;
import de.tudarmstadt.ukp.dkpro.tc.api.features.DocumentFeatureExtractor;
import de.tudarmstadt.ukp.dkpro.tc.api.features.Feature;
import de.tudarmstadt.ukp.dkpro.tc.api.features.FeatureExtractorResource_ImplBase;

/**
 * Heylighen & Dewaele (2002): Variation in the contextuality of language
 * The contextuality measure can reach values 0-100
 * The higher value, the more formal (male) style the text is,
 * i.e. contains many nouns, verbs, determiners.
 * The lower value, the more contextual (female) style the text is,
 * i.e. contains many adverbs, pronouns and such.
 * 
 * 
 * Extracts also values for each pos class, as they are calculated anyway
 * 
 */
public class ContextualityMeasureFeatureExtractor
    extends FeatureExtractorResource_ImplBase
    implements DocumentFeatureExtractor
{
    public static final String CONTEXTUALITY_MEASURE_FN = "ContextualityMeasure";

    @Override
    public List<Feature> extract(JCas jcas)
        throws TextClassificationException
    {
        List<Feature> featList = new ArrayList<Feature>();

        double total = JCasUtil.select(jcas, POS.class).size();
        double noun = select(jcas, N.class).size() / total;
        double adj = select(jcas, ADJ.class).size() / total;
        double prep = select(jcas, PP.class).size() / total;
        double art = select(jcas, ART.class).size() / total;// !includes determiners
        double pro = select(jcas, PR.class).size() / total;
        double verb = select(jcas, V.class).size() / total;
        double adv = select(jcas, ADV.class).size() / total;

        int interjCount = 0;
        for (POS tag : JCasUtil.select(jcas, O.class)) {
            // FIXME Issue 123: this is tagset specific
            if (tag.getPosValue().contains("UH")) {
                interjCount++;
            }
        }
        double interj = interjCount / total;

        // noun freq + adj.freq. + prepositions freq. + article freq. - pronoun freq. - verb f. -
        // adverb - interjection + 100
        double contextualityMeasure = 0.5 * (noun + adj + prep + art - pro - verb - adv - interj + 100);

        featList.add(new Feature("NounRate", noun));
        featList.add(new Feature("AdjectiveRate", adj));
        featList.add(new Feature("PrepositionRate", prep));
        featList.add(new Feature("ArticleRate", art));
        featList.add(new Feature("PronounRate", pro));
        featList.add(new Feature("VerbRate", verb));
        featList.add(new Feature("AdverbRate", adv));
        featList.add(new Feature("InterjectionRate", interj));
        featList.add(new Feature(CONTEXTUALITY_MEASURE_FN, contextualityMeasure));

        return featList;
    }

}
