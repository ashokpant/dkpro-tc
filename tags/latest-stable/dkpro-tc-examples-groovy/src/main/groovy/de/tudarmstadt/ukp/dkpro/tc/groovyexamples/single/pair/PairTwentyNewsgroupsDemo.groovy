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
package de.tudarmstadt.ukp.dkpro.tc.groovyexamples.single.pair

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription

import org.apache.uima.analysis_engine.AnalysisEngineDescription
import org.apache.uima.resource.ResourceInitializationException

import weka.classifiers.functions.SMO
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordNamedEntityRecognizer
import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter
import de.tudarmstadt.ukp.dkpro.lab.Lab
import de.tudarmstadt.ukp.dkpro.lab.task.Dimension
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask.ExecutionPolicy
import de.tudarmstadt.ukp.dkpro.tc.core.Constants
import de.tudarmstadt.ukp.dkpro.tc.examples.io.PairTwentyNewsgroupsReader
import de.tudarmstadt.ukp.dkpro.tc.examples.util.DemoUtils
import de.tudarmstadt.ukp.dkpro.tc.features.pair.core.length.DiffNrOfTokensPairFeatureExtractor
import de.tudarmstadt.ukp.dkpro.tc.ml.ExperimentTrainTest
import de.tudarmstadt.ukp.dkpro.tc.ml.report.BatchOutcomeIDReport
import de.tudarmstadt.ukp.dkpro.tc.ml.report.BatchTrainTestReport
import de.tudarmstadt.ukp.dkpro.tc.weka.WekaClassificationAdapter
import de.tudarmstadt.ukp.dkpro.tc.weka.report.WekaClassificationReport


/**
 * PairTwentyNewsgroupsExperiment, using Groovy
 *
 * The PairTwentyNewsgroupsExperiment takes pairs of news files and trains/tests
 * a binary classifier to learn if the files in the pair are from the same newsgroup.
 * The pairs are listed in a tsv file: see the files in src/main/resources/lists/ as
 * examples.
 * <p>
 * PairTwentyNewsgroupsExperiment uses similar architecture as TwentyNewsgroupsGroovyExperiment
 * ({@link TrainTestExperiment}) to automatically wire the standard tasks for
 * a basic TrainTest setup.  To remind the user to be careful of information leak when
 * training and testing on pairs of data from similar sources, we do not provide
 * a demo Cross Validation setup here.  (Our sample train and test datasets are from separate
 * newsgroups.)  Please see TwentyNewsgroupsGroovyExperiment for a demo implementing a CV experiment.
 *
 *
 * @author Emily Jamison
 */
class PairTwentyNewsgroupsDemo implements Constants {

    // === PARAMETERS===========================================================

    def experimentName = "PairTwentyNewsgroupsExperiment"
    def languageCode = "en"
    def listFilePathTrain = "src/main/resources/data/twentynewsgroups/pairs/pairslist.train"
    def listFilePathTest  ="src/main/resources/data/twentynewsgroups/pairs/pairslist.test"


    // === DIMENSIONS===========================================================

    def dimReaders = Dimension.createBundle("readers", [
        readerTest: PairTwentyNewsgroupsReader,
        readerTestParams: [
            PairTwentyNewsgroupsReader.PARAM_LISTFILE,
            listFilePathTest,
            PairTwentyNewsgroupsReader.PARAM_LANGUAGE_CODE,
            languageCode
        ],
        readerTrain: PairTwentyNewsgroupsReader,
        readerTrainParams: [
            PairTwentyNewsgroupsReader.PARAM_LISTFILE,
            listFilePathTrain,
            PairTwentyNewsgroupsReader.PARAM_LANGUAGE_CODE,
            languageCode
        ]
    ])

    def dimFeatureMode = Dimension.create(DIM_FEATURE_MODE, FM_PAIR)
    def dimLearningMode = Dimension.create(DIM_LEARNING_MODE, LM_SINGLE_LABEL)

    def dimClassificationArgs = Dimension.create(DIM_CLASSIFICATION_ARGS,
    //	[NaiveBayes.name],
    [SMO.name])

    def dimFeatureSets = Dimension.create(
    DIM_FEATURE_SET,
    [
        // This feature is sensible and fast, but gives bad results on the demo data
        DiffNrOfTokensPairFeatureExtractor.name,
        // Please review LuceneNGramPFE's javadoc to understand
        // the parameters before using LuceneNGramPFE.
        //      LuceneNGramPFE.name
    ]
    )

    // === Experiments =========================================================


    /**
     * TrainTest Setting
     *
     * @throws Exception
     */
    protected void runTrainTest() throws Exception
    {

        ExperimentTrainTest batchTask = [
            experimentName: experimentName + "-TrainTest-Groovy",
            // we need to explicitly set the name of the batch task, as the constructor of the groovy setup must be zero-arg
            type: "Evaluation-"+ experimentName +"-TrainTest-Groovy",
            preprocessing:	getPreprocessing(),
            machineLearningAdapter: WekaClassificationAdapter,
            innerReports: [WekaClassificationReport],
            parameterSpace : [
                dimReaders,
                dimFeatureMode,
                dimLearningMode,
                dimClassificationArgs,
                dimFeatureSets
            ],
            executionPolicy: ExecutionPolicy.RUN_AGAIN,
            reports:         [
                BatchTrainTestReport,
                BatchOutcomeIDReport]
        ]

        // Run
        Lab.getInstance().run(batchTask)
    }

    private AnalysisEngineDescription getPreprocessing()
    throws ResourceInitializationException
    {
        return createEngineDescription(
        createEngineDescription(StanfordSegmenter),
        createEngineDescription(StanfordNamedEntityRecognizer,
        StanfordNamedEntityRecognizer.PARAM_VARIANT, "all.3class.distsim.crf")
        )
    }

    public static void main(String[] args)
    {
		DemoUtils.setDkproHome(PairTwentyNewsgroupsDemo.getSimpleName());
        new PairTwentyNewsgroupsDemo().runTrainTest()
    }

}
