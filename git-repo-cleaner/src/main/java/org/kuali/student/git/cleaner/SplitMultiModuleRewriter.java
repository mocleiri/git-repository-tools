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
package org.kuali.student.git.cleaner;

import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.kuali.student.git.model.GitRepositoryUtils;
import org.kuali.student.git.model.tree.GitTreeData;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

/**
 * In the case where a trunk contained many modules and we want to extract just the commits related to a specific module.
 *
 * But we also want the release branches unchanged (but rewritten to update to the rewritten trunk commits.
 *
 * this also fixes up the git-svn-id comment for the path that is collapsed.
 * 
 * @author ocleirig
 * 
 */
public class SplitMultiModuleRewriter extends org.kuali.student.git.cleaner.AbstractRepositoryCleaner {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
			.getLogger(SplitMultiModuleRewriter.class);


	private java.util.Map<ObjectId, String> blobIdToReplacementContentMap = new java.util.HashMap<>();
    private String targetPath;


    /**
	 *
	 */
	public SplitMultiModuleRewriter() {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.kuali.student.git.cleaner.RepositoryCleaner#validateArgs(java.lang
	 * .String[])
	 */
	@Override
	public void validateArgs(java.util.List<String> args) throws Exception {

		if (args.size() != 2 && args.size() != 4) {
			log.error("USAGE: <source git repository meta directory> <path to collapse> <git command path>");
			log.error("\t<git repo meta directory> : the path to the meta directory of the source git repository");
            log.error("\t<path to collapse> : the path to a native git ");
			log.error("\t<git command path> : the path to a native git ");
			throw new IllegalArgumentException("invalid arguments");
		}

		setRepo(GitRepositoryUtils.buildFileRepository(
                new File(args.get(0)).getAbsoluteFile(), false));

		setBranchRefSpec(Constants.R_HEADS);

        setExternalGitCommandPath(args.get(2).trim());

        targetPath = args.get(1).trim();
	}

    @Override
    protected boolean processCommitTree(org.eclipse.jgit.revwalk.RevCommit commit, org.kuali.student.git.model.tree.GitTreeData tree) throws org.eclipse.jgit.errors.MissingObjectException, org.eclipse.jgit.errors.IncorrectObjectTypeException, org.eclipse.jgit.errors.CorruptObjectException, java.io.IOException {
        ObjectId targetTree = tree.find(getRepo(), targetPath);

        if (targetTree != null)
            return true;
        else
            return false; // might still be processed if the parent was rewritten.
    }

    @Override
    protected CommitBuilder createCommitBuilder(RevCommit commit, GitTreeData tree) throws java.io.IOException {

        CommitBuilder builder = new CommitBuilder();

        builder.setAuthor(commit.getAuthorIdent());
        builder.setMessage(commit.getFullMessage());

        builder.setCommitter(commit.getCommitterIdent());

        ObjectId targetTree = tree.find(getRepo(), targetPath);

        if (targetTree != null) {

            builder.setTreeId(targetTree);

            builder.setMessage(org.kuali.student.cleaner.model.GitSvnIdUtils.applyPathToExistingGitSvnId(commit.getFullMessage(), targetPath));

        }
        else {

            if (tree.isTreeDirty()) {

                ObjectId newTreeId = tree.buildTree(inserter);

                builder.setTreeId(newTreeId);
            } else {
                builder.setTreeId(commit.getTree().getId());
            }
        }

        builder.setEncoding("UTF-8");

        Set<ObjectId> newParents = processParents(commit);

        builder.setParentIds(new ArrayList<>(newParents));

        return builder;
    }

    /* (non-Javadoc)
             * @see org.kuali.student.git.cleaner.AbstractRepositoryCleaner#getFileNameSuffix()
             */
	@Override
	protected String getFileNameSuffix() {
		return "split-multi-module";
	}

	
}
