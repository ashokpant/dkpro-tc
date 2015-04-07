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
package de.tudarmstadt.ukp.dkpro.tc.evaluation.measures.label;

import java.util.HashMap;
import java.util.Map;

import de.tudarmstadt.ukp.dkpro.tc.evaluation.confusion.matrix.CombinedSmallContingencyTable;


/**
 * @author Andriy Nadolskyy
 * 
 */
public class MicroRecall
{

	public static Map<String, Double> calculate(CombinedSmallContingencyTable cSCTable, 
			boolean softEvaluation) 
	{
		double tp = cSCTable.getTruePositives();
		double fn = cSCTable.getFalseNegatives();
		
		Double recall = 0.0;
		double denominator = tp + fn;
		if (denominator != 0.0) {
			recall = (Double) tp / denominator;
		}
		else if (! softEvaluation) {
			recall = Double.NaN;
		}		
		Map<String, Double> results = new HashMap<String, Double>();
		results.put(MicroRecall.class.getSimpleName(), recall);
		return results;	 
	}	
}