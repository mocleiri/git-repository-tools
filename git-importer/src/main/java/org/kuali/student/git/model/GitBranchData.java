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
package org.kuali.student.git.model;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.Repository;
import org.kuali.student.git.model.SvnRevisionMapper.SvnRevisionMap;
import org.kuali.student.git.model.branch.exceptions.VetoBranchException;
import org.kuali.student.git.model.branch.utils.GitBranchUtils;
import org.kuali.student.git.model.tree.GitTreeData;
import org.kuali.student.git.model.tree.GitTreeNodeData;
import org.kuali.student.git.model.tree.GitTreeNodeInitializer;
import org.kuali.student.git.model.tree.utils.GitTreeDataUtils;
import org.kuali.student.git.model.tree.utils.GitTreeProcessor;
import org.kuali.student.git.model.util.GitBranchDataUtils;
import org.kuali.student.svn.model.ExternalModuleInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Represents the
 * 
 * @author Kuali Student Team
 * 
 */
public class GitBranchData {

	public static final Logger log = LoggerFactory
			.getLogger(GitBranchData.class);

	private GitTreeData branchRoot;

	private String branchPath;

	private String branchName;

	private ObjectId parentId;

    private Map<String, SvnRevisionMap>mergeParents = new LinkedHashMap<String, SvnRevisionMap>();

	private AtomicLong blobsAdded = new AtomicLong(0L);

	private boolean created;

	private long revision;

	private GitTreeProcessor treeProcessor;

	private Map<String, BranchMergeInfo>branchPathToMergeInfoMap = new HashMap<String, BranchMergeInfo>();
	
	private boolean alreadyInitialized = false;

	private boolean blobsDeleted = false;

	private List<ExternalModuleInfo> externals = new ArrayList<>();

	private SvnRevisionMapper revisionMapper;

	private GitTreeNodeInitializer nodeInitializer;

	private Repository repo;

	/**
	 * @param revision
	 * @param branchPath
	 * @param revision 
	 * @param largeBranchNameProvider 
	 * @param path
	 * 
	 */
	public GitBranchData(Repository repo, String branchName, long revision, SvnRevisionMapper revisionMapper, GitTreeProcessor treeProcessor, GitTreeNodeInitializer nodeInitializer) {
		this.repo = repo;
		this.revisionMapper = revisionMapper;
		this.treeProcessor = treeProcessor;
		this.nodeInitializer = nodeInitializer;
		this.branchPath = GitBranchUtils.getBranchPath(branchName, revision, revisionMapper);
		this.revision = revision;
		this.branchName = GitBranchUtils.getCanonicalBranchName(this.branchPath, revision, revisionMapper);
		
		this.branchRoot = new GitTreeData(nodeInitializer);

	}

	/**
	 * @return the branchPath
	 */
	public String getBranchPath() {
		return branchPath;
	}

	/**
	 * @return the branchName
	 */
	public String getBranchName() {
		return branchName;
	}

	public long getBlobsAdded() {
		return blobsAdded.get();
	}
	
	

	/**
	 * @return the blobsDeleted
	 */
	public boolean isBlobsDeleted() {
		return blobsDeleted;
	}

	public void addBlob(String path, ObjectId blobSha1, PrintWriter blobLog)
			throws VetoBranchException, MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {

		
		if (!path.startsWith(this.branchPath + "/")) {
			String errorMessage = String.format("%s : blob absolute path(%s) does not match this branch (%s)", String.valueOf (revision), path, this.branchName);
			log.error(errorMessage);
			blobLog.println(errorMessage);
			return;
		}

		initialize();
		
		blobsAdded.addAndGet(1L);
		
		String filePath = path.substring(this.branchPath.length());
		
		if (filePath.startsWith("/"));
			filePath = filePath.substring(1);
			
		if (filePath.length() == 0) {
			String errorMessage = String.format ("trying to index an empty file path.  Revision = %d, Path = %s, File Path = %s, blobId = %s, ", revision, path, filePath, blobSha1);
			
			log.warn(errorMessage);
			blobLog.println(errorMessage);
			
			/*
			 * Indexing an empty file breaks the JGit Tree so exclude the file.
			 */
			return;
		}

		branchRoot.addBlob(filePath, blobSha1);

	}

	/**
	 * @param inserter
	 * @return
	 * @throws IOException
	 * @see org.kuali.student.git.model.tree.GitTreeData#buildTree(org.eclipse.jgit.lib.ObjectInserter)
	 */
	public ObjectId buildTree(ObjectInserter inserter) throws IOException {
		return branchRoot.buildTree(inserter);

	}

	public int getBlobCount() {
		return GitTreeDataUtils.countBlobs(branchRoot);
	}

	public void deletePath(String path, long currentRevision) throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
		
		initialize();
		
		// should we strip of the branch name part of the path and only pass
		// through the
		// remaining path.

		if (path.startsWith(branchPath)) {

			StringBuilder withinBranchPath = new StringBuilder(path);

			withinBranchPath.delete(0, branchPath.length());

			if (withinBranchPath.charAt(0) == '/')
				withinBranchPath.deleteCharAt(0);

			boolean deletedBlob = branchRoot.deletePath(withinBranchPath.toString());
			
			if (deletedBlob && !blobsDeleted)
				blobsDeleted = true;
			
		} else {
			log.warn("invalid branch");
		}

	}

	
	
	private void initialize() throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
		
		if (alreadyInitialized || parentId == null)
			return;
		
		alreadyInitialized = true;
		
		this.branchRoot = treeProcessor.extractExistingTreeDataFromCommit (parentId);
		
		this.branchRoot.resetDirtyFlag();

		GitBranchDataUtils.extractAndStoreBranchMerges(this.revision-1L, this.branchName, this, revisionMapper);
		
		GitBranchDataUtils.extractExternalModules(repo, this.branchRoot, this, treeProcessor);
		
	}
	
	

	public Set<ObjectId> getMergeParentIds(Set<SvnRevisionMap> svnMergeData) {

        for (SvnRevisionMap mergeData : svnMergeData) {
            addMergeBranch(mergeData);
        }

        Set<ObjectId>latestBranchParentIds = new LinkedHashSet<ObjectId>();

        for (Map.Entry<String, SvnRevisionMap>entry : this.mergeParents.entrySet()) {
            latestBranchParentIds.add(entry.getValue().getCommitId());
        }

		return Collections.unmodifiableSet(latestBranchParentIds);

	}

	public void setParentId(ObjectId parentId) {
		this.parentId = parentId;

	}

	/**
	 * @return the parentId
	 */
	public ObjectId getParentId() {
		return parentId;
	}

    /**
     * Add the merge branch.  If nothing from the indicated branch exists then store it.
     *
     * If there is an existing merge branch on the same branch with a smaller revision then replace it.
     *
     * @param revMap
     */
	public void addMergeBranch(SvnRevisionMap revMap) {

        String branchName = revMap.getBranchName();

        SvnRevisionMap existingRevMap = this.mergeParents.get(branchName);

        if (existingRevMap == null) {
            this.mergeParents.put(branchName, revMap);
        }
        else {
            if (existingRevMap.getRevision() < revMap.getRevision())
                this.mergeParents.put(branchName, revMap);
        }

	}

	public void setCreated(boolean created) {
		this.created = created;

	}

	/**
	 * @return the created
	 */
	public boolean isCreated() {
		return created;
	}

	public void reset() {

		created = false;
		blobsAdded.set(0L);
		branchRoot = new GitTreeData(nodeInitializer);
		mergeParents.clear();
		parentId = null;

	}

	public List<BranchMergeInfo>getAccumulatedBranchMergeData() {
		return new ArrayList<BranchMergeInfo>(this.branchPathToMergeInfoMap.values());
	}
	
	public void accumulateMergeInfo(
			List<BranchMergeInfo> extractBranchMergeInfoFromString) {
		
		for (BranchMergeInfo branchMergeInfo : extractBranchMergeInfoFromString) {
			
			BranchMergeInfo existingMergeInfo = this.branchPathToMergeInfoMap.get(branchMergeInfo.getBranchName());
			
			if (existingMergeInfo == null) {
				this.branchPathToMergeInfoMap.put(branchMergeInfo.getBranchName(), branchMergeInfo);
			}
			else {
				Set<Long>mergedRevisions = new HashSet<>();
				
				mergedRevisions.addAll(existingMergeInfo.getMergedRevisions());
				mergedRevisions.addAll(branchMergeInfo.getMergedRevisions());
				
				existingMergeInfo.setMergedRevisions(mergedRevisions);
			}
			
		}
		
	}

	public void setExternals(List<ExternalModuleInfo> externals) {
		this.externals = externals;
	}
	
	/**
	 * @return the externals
	 */
	public List<ExternalModuleInfo> getExternals() {
		return Collections.unmodifiableList(externals);
	}

	public void clearMergeInfo() {

		this.branchPathToMergeInfoMap.clear();
	}

	public void clearExternals() {
		
		this.externals.clear();
		
	}

	public void addTree(String path, ObjectId treeId) throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
		
		if (path.isEmpty()) {
			
			GitTreeNodeData root = treeProcessor.extractExistingTreeData(treeId, "");
			
			this.branchRoot.setRoot(root);
		}
		else {
			/*
			 * Adding a sub path so we need to initialize
			 */
			initialize();
			this.branchRoot.addTree(treeProcessor, path, treeId);
		}
	}

	public ObjectId findPath(Repository repo, String subPath) throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
		
		return this.branchRoot.find(repo, subPath);
	}

	public boolean isTreeDirty() {
		return this.branchRoot.isTreeDirty();
	}


    public void addParentBranch(SvnRevisionMap parentRevMap) {

        addMergeBranch(parentRevMap);

        this.parentId = parentRevMap.getCommitId();
    }
}
