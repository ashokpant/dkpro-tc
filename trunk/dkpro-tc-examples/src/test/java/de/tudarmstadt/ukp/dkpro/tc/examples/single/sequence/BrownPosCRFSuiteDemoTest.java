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
package de.tudarmstadt.ukp.dkpro.tc.examples.single.sequence;

import java.io.File;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import de.tudarmstadt.ukp.dkpro.lab.task.ParameterSpace;
import de.tudarmstadt.ukp.dkpro.tc.core.Constants;
import de.tudarmstadt.ukp.dkpro.tc.examples.utils.JavaDemosTest_Base;

/**
 * This test just ensures that the experiment runs without throwing
 * any exception.
 * 
 * @author Oliver Ferschke, Emily Jamison
 * 
 */
@Ignore
public class BrownPosCRFSuiteDemoTest extends JavaDemosTest_Base
{
    BrownPosCRFSuiteDemo javaExperiment;
    ParameterSpace pSpace;

    @Before
    public void setup()
        throws Exception
    {
        super.setup();
        
        javaExperiment = new BrownPosCRFSuiteDemo();
        pSpace = BrownPosCRFSuiteDemo.getParameterSpace(Constants.FM_SEQUENCE, Constants.LM_SINGLE_LABEL);
    }

    @Test
    public void testJavaCrossValidation()
        throws Exception
    {
//        Assume.assumeTrue(Runtime.getRuntime().maxMemory() >= 2000000000);
        javaExperiment.runCrossValidation(pSpace);
    }
}
