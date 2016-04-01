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
package org.kuali.student.cleaner;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Assert;
import org.junit.Test;
import org.kuali.student.cleaner.model.ObjectIdTranslationMapImpl;
import org.kuali.student.cleaner.model.bitmap.RevCommitBitMapIndex;
import org.kuali.student.cleaner.model.sort.FusionAwareTopoSortComparator;
import org.kuali.student.git.model.DummyGitTreeNodeInitializer;
import org.kuali.student.git.model.ExternalModuleUtils;
import org.kuali.student.git.model.GitRepositoryUtils;
import org.kuali.student.git.model.ref.utils.GitRefUtils;
import org.kuali.student.git.model.tree.GitTreeData;
import org.kuali.student.git.model.tree.utils.GitTreeProcessor;
import org.kuali.student.svn.model.AbstractGitRespositoryTestCase;
import org.kuali.student.svn.model.ExternalModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test case for developing a Comparator based RevSort that will resemble a topo sort but also handle the fusion lateral branch
 * dependencies.
 * 
 * @author ocleirig
 *
 */
public class SplitMultiModuleRewriterTestCase extends AbstractGitRespositoryTestCase {

	private static final Logger log = LoggerFactory.getLogger(SplitMultiModuleRewriterTestCase.class);

	/**
	 * @param name
	 */
	public SplitMultiModuleRewriterTestCase() {
		super("split-multi-module-rewriter", true);
	}

	/* (non-Javadoc)
	 * @see org.kuali.student.svn.model.AbstractGitRespositoryTestCase#onBefore()
	 */
	@Override
	protected void onBefore() throws Exception {
		/*
		 * Setup base commits
		 * 
		 * base commit
		 * 
		 * aggregate trunk
		 * 
		 * and the different modules
		 * 
		 */
		
		ObjectInserter inserter = repo.newObjectInserter();
		
		// create trunk
		GitTreeData trunk = new GitTreeData(new DummyGitTreeNodeInitializer());
		
		storeFile (inserter, trunk, "src/modules/my-module/example.txt", "test");
		storeFile (inserter, trunk, "readme.txt", "test");
		
		ObjectId commitId = commit (inserter, trunk, "initial trunk commit");
		
		inserter.flush();

        assertSmallBlobContents(trunk, "readme.txt", "test");
		
		GitRefUtils.createOrUpdateBranch(repo, "trunk", commitId);
		
		storeFile (inserter, trunk, "readme.txt", "changes to readme.txt");
		
		commitId = commit (inserter, trunk, "changes only to readme.txt", commitId);

		inserter.flush();
		
		GitRefUtils.createOrUpdateBranch(repo, "trunk", commitId);

        org.eclipse.jgit.lib.ObjectId treeId = GitRepositoryUtils.findInCommit(repo, commitId, "src/modules/my-module");

        GitTreeData release = new GitTreeData(new DummyGitTreeNodeInitializer());

        release.setGitTreeObjectId(treeId);

        commitId = commit (inserter, release, "make release", commitId);

        inserter.flush();

        GitRefUtils.createOrUpdateBranch(repo, "release", commitId);

        inserter.release();

        repo.getRefDatabase().refresh();


    }
	
	@Test
	public void testSplitMultiModuleRewriterOnRepo() throws Exception {

        org.kuali.student.git.cleaner.SplitMultiModuleRewriter rewriter = new org.kuali.student.git.cleaner.SplitMultiModuleRewriter();

        java.util.List<String> args = new java.util.ArrayList<String>();
        // <source git repository meta directory> <path to collapse> <git command path>

        args.add(repo.getDirectory().getAbsolutePath());

        args.add("src/modules/my-module");

        rewriter.validateArgs(args);

        rewriter.execute();

        rewriter.close();
		
	}

	

}
