/*
 *  Copyright 2014 The Kuali Foundation Licensed under the
 *	Educational Community License, Version 2.0 (the "License"); you may
 *	not use this file except in compliance with the License. You may
 *	obtain a copy of the License at
 *
 *	http://www.osedu.org/licenses/ECL-2.0
 *
 *	Unless required by applicable law or agreed to in writing,
 *	software distributed under the License is distributed on an "AS IS"
 *	BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *	or implied. See the License for the specific language governing
 *	permissions and limitations under the License.
 */
package org.kuali.student.git.model;

import java.io.IOException;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.kuali.student.git.model.tree.GitTreeNodeData;
import org.kuali.student.git.model.tree.GitTreeNodeInitializer;
import org.kuali.student.git.model.tree.exceptions.GitTreeNodeInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ocleirig
 *
 */
public class DummyGitTreeNodeInitializer implements GitTreeNodeInitializer {

	private static final Logger log = LoggerFactory.getLogger(DummyGitTreeNodeInitializer.class);
	/**
	 * 
	 */
	public DummyGitTreeNodeInitializer() {
	}

	/* (non-Javadoc)
	 * @see org.kuali.student.git.model.tree.GitTreeNodeInitializer#initialize(org.kuali.student.git.model.tree.GitTreeData.GitTreeNodeData)
	 */
	@Override
	public void initialize(GitTreeNodeData node) throws GitTreeNodeInitializationException {
		
		log.info("initialize called for node = " + node);
		node.setInitialized(true);

	}

}
