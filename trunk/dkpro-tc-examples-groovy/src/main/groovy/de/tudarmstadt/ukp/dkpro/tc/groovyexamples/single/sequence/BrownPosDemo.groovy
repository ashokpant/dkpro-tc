/**
 * Copyright 2014
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package de.tudarmstadt.ukp.dkpro.tc.groovyexamples.single.sequence

import static de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase.INCLUDE_PREFIX
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription

import org.apache.uima.analysis_engine.AnalysisEngineDescription
import org.apache.uima.fit.component.NoOpAnnotator
import org.apache.uima.resource.ResourceInitializationException

import de.tudarmstadt.ukp.dkpro.lab.Lab
import de.tudarmstadt.ukp.dkpro.lab.task.Dimension
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask.ExecutionPolicy
import de.tudarmstadt.ukp.dkpro.tc.core.Constants
import de.tudarmstadt.ukp.dkpro.tc.crfsuite.CRFSuiteAdapter
import de.tudarmstadt.ukp.dkpro.tc.crfsuite.CRFSuiteBatchCrossValidationReport
import de.tudarmstadt.ukp.dkpro.tc.crfsuite.CRFSuiteClassificationReport
import de.tudarmstadt.ukp.dkpro.tc.examples.io.BrownCorpusReader
import de.tudarmstadt.ukp.dkpro.tc.examples.util.DemoUtils
import de.tudarmstadt.ukp.dkpro.tc.features.length.NrOfTokensUFE
import de.tudarmstadt.ukp.dkpro.tc.ml.ExperimentCrossValidation

/**
 * This a Groovy experiment setup of POS tagging as sequence tagging.
 */
class BrownPosDemo
implements Constants {

    def String LANGUAGE_CODE = "en"
    def NUM_FOLDS = 2
    def String corpusFilePathTrain = "src/main/resources/data/brown_tei/"
    def experimentName = "BrownPosDemo"

    def dimReaders = Dimension.createBundle("readers", [
        readerTrain: BrownCorpusReader,
        readerTrainParams: [
            BrownCorpusReader.PARAM_LANGUAGE,
            LANGUAGE_CODE,
            BrownCorpusReader.PARAM_SOURCE_LOCATION,
            corpusFilePathTrain,
            BrownCorpusReader.PARAM_PATTERNS,
            [
                INCLUDE_PREFIX + "*.xml",
                INCLUDE_PREFIX + "*.xml.gz"
            ]
        ]])
    def dimLearningMode = Dimension.create(DIM_LEARNING_MODE, LM_SINGLE_LABEL)
    def dimFeatureMode = Dimension.create(DIM_FEATURE_MODE, FM_SEQUENCE)
    def dimFeatureSets = Dimension.create(
    DIM_FEATURE_SET, [
        NrOfTokensUFE.name
    ])

    // ##### CV #####
    protected void runCrossValidation()
    throws Exception
    {
        ExperimentCrossValidation batchTask = [
            experimentName: experimentName + "-CV-Groovy",
            // we need to explicitly set the name of the batch task, as the constructor of the groovy setup must be zero-arg
            type: "Evaluation-"+ experimentName +"-CV-Groovy",
            preprocessing:  getPreprocessing(),
            machineLearningAdapter: CRFSuiteAdapter,
            innerReports: [CRFSuiteClassificationReport],
            parameterSpace : [
                dimReaders,
                dimFeatureMode,
                dimLearningMode,
                dimFeatureSets
            ],
            executionPolicy: ExecutionPolicy.RUN_AGAIN,
            reports:         [
                CRFSuiteBatchCrossValidationReport
            ],
            numFolds: NUM_FOLDS]

        // Run
        Lab.getInstance().run(batchTask)
    }

    protected AnalysisEngineDescription getPreprocessing()
    throws ResourceInitializationException
    {
        return createEngineDescription(NoOpAnnotator)
    }

    public static void main(String[] args)
    {
		DemoUtils.setDkproHome(BrownPosDemo.getSimpleName());
        new BrownPosDemo().runCrossValidation()
    }
}
