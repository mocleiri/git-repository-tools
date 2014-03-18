/*
 * Copyright 2014 The Kuali Foundation
 * 
 * Licensed under the Educational Community License, Version 1.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.kuali.student.git.tools.merge;

import org.junit.Assert;
import org.junit.Test;
import org.kuali.student.git.model.AbstractKualiStudentBranchDetectorTest;
import org.kuali.student.git.model.branch.BranchDetectorImpl;
import org.kuali.student.git.model.exceptions.VetoBranchException;
import org.kuali.student.git.utils.GitBranchUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kuali Student Team
 *
 */

public class TestGitSubTreePath extends AbstractKualiStudentBranchDetectorTest {

	private static final Logger log = LoggerFactory.getLogger(TestGitSubTreePath.class);
	
	
	
	/**
	 * 
	 */
	public TestGitSubTreePath() {
		// TODO Auto-generated constructor stub
	}
	
	@Test
	public void testCreateSubTreeBlobPath() throws VetoBranchException {
		
		String directoryPath = "poc/brms/brms-ri/branches/brms-ri-dev/inspect-rule-failure/src/main/java/org/kuali/student/rules/runtime/ast";
		
		String copyFromDirectoryPath = "poc/brms/brms-ri/branches/brms-ri-dev/inspect-rule-failure/src/main/java/org/kuali/student/rules/ast";

		String copyFromBlobPath = "inspect-rule-failure/src/main/java/org/kuali/student/rules/ast/BinaryTree.java";
		
		
		/*
		 * We want to end up with a path rooted in the path specified by the directoryPath (path from the svn dump)
		 * 
		 * poc/brms/brms-ri/branches/brms-ri-dev/inspect-rule-failure/src/main/java/org/kuali/student/rules/runtime/ast/BinaryTree.java
		 * 
		 */
		String alteredBlobPath = GitBranchUtils.convertToTargetPath(directoryPath, 0L, copyFromDirectoryPath, copyFromBlobPath, branchDetector);
	   
		Assert.assertEquals("poc/brms/brms-ri/branches/brms-ri-dev/inspect-rule-failure/src/main/java/org/kuali/student/rules/runtime/ast/BinaryTree.java", alteredBlobPath);
				
		
	}
	
	@Test
	public void testRevision12075() throws VetoBranchException {
		
		String targetPath = "deploymentlab/tags/impex-parent-1.0.0";
		
		String copyFromPath = "deploymentlab/trunk/impex";
		
		long copyFromRevision = 12074;
		
		String alteredPath = GitBranchUtils.convertToTargetPath(targetPath, copyFromRevision, copyFromPath, "impex/pom.xml", branchDetector);
		
		Assert.assertEquals("deploymentlab/tags/impex-parent-1.0.0/pom.xml", alteredPath);
		
	}
	
	@Test
	public void testKsRevision100() throws VetoBranchException {
		
		String path = "poc/brms/brms-ri/branches/brms-ri-dev/inspect-rule-failure/src/main/java/org/kuali/student/rules/runtime/ast";
		
		String brachSubPath = "inspect-rule-failure/src/main/java/org/kuali/student/rules/runtime/ast";
		
		String copyFromPath = "poc/brms/brms-ri/branches/brms-ri-dev/inspect-rule-failure/src/main/java/org/kuali/student/rules/ast";
		
		String copyFromBranchSubPath = "inspect-rule-failure/src/main/java/org/kuali/student/rules/ast";
		
		String blobPath = "inspect-rule-failure/src/main/java/org/kuali/student/rules/ast/BinaryTree.java";
		
		String alteredPath = GitBranchUtils.convertToTargetPath(path, 100L, copyFromPath, blobPath, branchDetector);
		
		Assert.assertEquals("poc/brms/brms-ri/branches/brms-ri-dev/inspect-rule-failure/src/main/java/org/kuali/student/rules/runtime/ast/BinaryTree.java", alteredPath);
	}
	
	@Test
	public void testRevision7563 () throws VetoBranchException {
		
		String targetPath = "deploymentlab/branches/1.0.0-m3/performance";
		
		String copyFromPath = "deploymentlab/performance/tsung/trunk";
		
		long copyFromRevision = 7562;
		
		String alteredPath = GitBranchUtils.convertToTargetPath(targetPath, copyFromRevision, copyFromPath, ".project", branchDetector);
		
		Assert.assertEquals("deploymentlab/branches/1.0.0-m3/performance/.project", alteredPath);
	}

}
