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
package org.kuali.student.repository.viewer;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.kuali.student.git.model.GitRepositoryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;

/**
 * @author ocleirig
 *
 */
public class ViewerMain {

	private static final Logger log = LoggerFactory.getLogger(ViewerMain.class);
	
	/**
	 * 
	 */
	public ViewerMain() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if (args.length != 1) {
			System.err.println("USAGE: <git meta directory>");
			System.exit(-1);
		}
		
		String gitDirectory = args[0];
		
		File gitDir = new File (gitDirectory);
		
		if (!gitDir.exists()) {
			System.err.println(gitDirectory + " does not exist");
			System.exit(-1);
		}
		
		try {
			
			Repository repo = GitRepositoryUtils.buildFileRepository(gitDir, false, true);
			
			Graph<RevCommit, String> graph = new DirectedSparseMultigraph<RevCommit, String>();
		
			RevWalk rw = new RevWalk (repo);
		
			
			Map<String, Ref> branchHeads = repo.getRefDatabase().getRefs(Constants.R_HEADS);
			
			Map<RevCommit, String>branchHeadCommitToBranchNameMap = new HashMap<RevCommit, String>();
			
			for (Map.Entry<String, Ref>entry : branchHeads.entrySet()) {
				
				RevCommit branchHeadCommit = rw.parseCommit(entry.getValue().getObjectId());
				
				rw.markStart(branchHeadCommit);
				
				branchHeadCommitToBranchNameMap.put(branchHeadCommit, entry.getValue().getName());
				
				
			}

			rw.sort(RevSort.TOPO);
			rw.sort(RevSort.REVERSE, true);
			
			RevCommit currentCommit = null;
			
			int edgeCounter = 1;
			
			Set<RevCommit>commits = new HashSet<RevCommit>();
			
			while ((currentCommit = rw.next()) != null) {
			
				commits.add(currentCommit);
				
			}
			
			for(RevCommit commit :  commits) {
				
				for (RevCommit parentCommit : commit.getParents()) {
//					log.info("add edge from " + currentCommit + " to " + parentCommit);
					graph.addEdge(String.valueOf(edgeCounter), commit, parentCommit);
					edgeCounter++;
				}
			}
			
			new GitGraphFrame(gitDir, graph, branchHeadCommitToBranchNameMap);
			
		} catch (Exception e) {
			log.error("viewing failed", e);
		}
		
	}

}