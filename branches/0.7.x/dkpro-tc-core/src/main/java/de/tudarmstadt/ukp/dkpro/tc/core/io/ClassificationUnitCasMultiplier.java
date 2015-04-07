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
package de.tudarmstadt.ukp.dkpro.tc.core.io;

import java.util.Collection;
import java.util.Iterator;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.AbstractCas;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.component.JCasMultiplier_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasCopier;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.tc.api.type.TextClassificationFocus;
import de.tudarmstadt.ukp.dkpro.tc.api.type.TextClassificationSequence;
import de.tudarmstadt.ukp.dkpro.tc.api.type.TextClassificationUnit;

/**
 * This JCasMultiplier creates a new JCas for each {@link TextClassificationUnit} annotation in the
 * original JCas. The newly created JCas contains one {@link TextClassificationFocus} annotation
 * that shows with TextClassificationUnit should be classified. All annotations in the original JCas
 * are copied to the new one.
 * 
 * @author Artem Vovk
 * @author zesch
 * 
 */
public class ClassificationUnitCasMultiplier
    extends JCasMultiplier_ImplBase
{

    /**
     * If true, the multiplier create a new CAS for each sequence in the current CAS. Otherwise, it
     * creates a new CAS for each text classification unit.
     */
    public static final String PARAM_USE_SEQUENCES = "useSequences";
    @ConfigurationParameter(name = PARAM_USE_SEQUENCES, mandatory = true, defaultValue = "false")
    private boolean useSequences;

    // For each TextClassificationUnit stored in this collection one corresponding JCas is created.
    private Collection<? extends AnnotationFS> annotations;
    private Iterator<? extends AnnotationFS> iterator;

    private JCas jCas;

    private int subCASCounter;
    private Integer sequenceCounter;
    private Integer unitCounter;

    @Override
    public void process(JCas aJCas)
        throws AnalysisEngineProcessException
    {
        this.jCas = aJCas;
        this.subCASCounter = 0;
        this.sequenceCounter = 0;
        this.unitCounter = 0;

        if (useSequences) {
            this.annotations = JCasUtil.select(aJCas, TextClassificationSequence.class);
        }
        else {
            this.annotations = JCasUtil.select(aJCas, TextClassificationUnit.class);
        }
        this.iterator = annotations.iterator();
    }

    @Override
    public boolean hasNext()
        throws AnalysisEngineProcessException
    {
        if (!iterator.hasNext()) {
            this.jCas = null;
            this.iterator = null;
            this.annotations = null;
            return false;
        }
        return true;
    }

    @Override
    public AbstractCas next()
        throws AnalysisEngineProcessException
    {
        // Create an empty CAS as a destination for a copy.
        JCas emptyJCas = this.getEmptyJCas();
        DocumentMetaData.create(emptyJCas);
        emptyJCas.setDocumentText(this.jCas.getDocumentText());
        CAS emptyCas = emptyJCas.getCas();

        // Copy current CAS to the empty CAS.
        CasCopier.copyCas(jCas.getCas(), emptyCas, false);
        JCas copyJCas;
        try {
            copyJCas = emptyCas.getJCas();
        }
        catch (CASException e) {
            throw new AnalysisEngineProcessException("Exception while creating JCas", null, e);
        }

        // Set new ids and URIs for copied cases.
        // The counting variable keeps track of how many new CAS objects are created from the
        // original CAS, a CAS relative counter.
        // NOTE: As it may cause confusion: If in sequence classification several or all CAS
        // contains only a single sequence this counter would be zero in all cases - this is not a
        // bug, but a cosmetic flaw
        DocumentMetaData.get(copyJCas).setDocumentId(
                DocumentMetaData.get(jCas).getDocumentId() + "_" + subCASCounter);
        DocumentMetaData.get(copyJCas).setDocumentUri(
                DocumentMetaData.get(jCas).getDocumentUri() + "_" + subCASCounter);

        // set the focus annotation
        AnnotationFS focusUnit = this.iterator.next();
        TextClassificationFocus focus = new TextClassificationFocus(copyJCas, focusUnit.getBegin(),
                focusUnit.getEnd());
        focus.addToIndexes();
        
        // set sequence and unit ids
        if (useSequences) {
        	TextClassificationSequence sequence = JCasUtil.selectCovered(copyJCas,  TextClassificationSequence.class, focus).get(0);
        	sequence.setId(sequenceCounter);
        	sequenceCounter++;
        	for (TextClassificationUnit unit : JCasUtil.selectCovered(copyJCas, TextClassificationUnit.class, focus)) {
        		unit.setId(unitCounter);
        		unitCounter++;
        	}
        	
        	// reset counter - we want to make the unit id relative to the sequence
        	unitCounter=0;
        }
        else  {
        	TextClassificationUnit unit = JCasUtil.selectCovered(copyJCas,  TextClassificationUnit.class, focus).get(0);
        	unit.setId(unitCounter);
        	unitCounter++;
        }

        subCASCounter++;
        getLogger().debug("Creating CAS " + subCASCounter + " of " + annotations.size());

        return copyJCas;
    }
}