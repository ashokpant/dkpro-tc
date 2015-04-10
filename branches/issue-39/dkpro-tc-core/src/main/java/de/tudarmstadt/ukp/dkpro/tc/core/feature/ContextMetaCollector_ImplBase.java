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
package de.tudarmstadt.ukp.dkpro.tc.core.feature;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.tc.api.type.TextClassificationUnit;

/**
 * Extract the context of each unit (in a sequence) and write it to a special file.
 *
 */public abstract class ContextMetaCollector_ImplBase
	extends JCasAnnotator_ImplBase
{
	
    public static final String PARAM_CONTEXT_FILE = "contextFile";
    @ConfigurationParameter(name = PARAM_CONTEXT_FILE, mandatory = true)
    private File contextFile;
    
	public static final String CONTEXT_KEY = "id2context.txt";
	
	public static final int CONTEXT_WIDTH = 30;
		
	protected StringBuilder sb;



	@Override
	public void initialize(UimaContext context)
			throws ResourceInitializationException
	{
		super.initialize(context);
		
		sb = new StringBuilder();
	}

	protected void addContext(JCas jcas, TextClassificationUnit unit, String id, StringBuilder sb) {
        sb.append(id);
        sb.append("\t");
        sb.append(getLeftContext(jcas, unit));
        sb.append("\t");
    	sb.append("[[");
    	sb.append(unit.getCoveredText());
    	sb.append("]]");
        sb.append("\t");
    	sb.append(getRightContext(jcas, unit));
        sb.append("\n");
	}

	@Override
	public void collectionProcessComplete()
			throws AnalysisEngineProcessException
	{
		super.collectionProcessComplete();
		
		try {
			FileUtils.writeStringToFile(contextFile, sb.toString());
		} catch (IOException e) {
			throw new AnalysisEngineProcessException(e);
		}
	}
	
	private String getLeftContext(JCas jcas, TextClassificationUnit unit) {
		int leftOffset = unit.getBegin() - CONTEXT_WIDTH;
		
		if (leftOffset < 0) {
			leftOffset = 0;
		}
		
		String context = jcas.getDocumentText().substring(leftOffset, unit.getBegin());
		context = context.replaceAll("\n", " ");
		
		return context;
	}
	
	private String getRightContext(JCas jcas, TextClassificationUnit unit) {
		int rightOffset = unit.getEnd() + CONTEXT_WIDTH;
		
		if (rightOffset > jcas.getDocumentText().length()) {
			rightOffset = jcas.getDocumentText().length();
		}
		
		String context = jcas.getDocumentText().substring(unit.getEnd(), rightOffset);
		context = context.replaceAll("\n", " ");
		
		return context;
	}
}
