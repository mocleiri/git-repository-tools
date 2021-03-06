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
package org.kuali.student.cleaner.model.sort;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.kuali.student.cleaner.model.bitmap.RevCommitBitMapIndex;
import org.kuali.student.git.cleaner.model.CommitDependency;
import org.kuali.student.git.model.ExternalModuleUtils;
import org.kuali.student.svn.model.ExternalModuleInfo;

/**
 * A Topo sort would cause the children to be ordered ahead of their parents.
 * 
 * This comparator will also include fusion data dependencies (as if they are parents aswell)
 * 
 * @author ocleirig
 *
 */
public class FusionAwareTopoSortComparator implements
		Comparator<RevCommit> {


	
	private RevCommitBitMapIndex index;

	/**
	 * 
	 */
	public FusionAwareTopoSortComparator(RevCommitBitMapIndex index) {
		super();
		this.index = index;
	}

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(RevCommit o1, RevCommit o2) {
		
		try {
			if (o1.getId().equals(o2.getId()))
				return 0;
			
			if (isCommitAfter (o1, o2)) 
				return +1;
			else if (isCommitAfter(o2, o1))
				return -1;
			else
				return 0; // commits are independent so make them equal
			
		} catch (MissingObjectException e) {
			throw new RuntimeException("FusionAwareTopoSortComparator failed: ", e);
		} catch (IncorrectObjectTypeException e) {
			throw new RuntimeException("FusionAwareTopoSortComparator failed: ", e);
		} catch (IOException e) {
			throw new RuntimeException("FusionAwareTopoSortComparator failed: ", e);
		}
		
	}

	/**
	 * Test if based on an analysis of the dependencies that o1 should occur after o2.
	 * 
	 * o1 should be after o2 if any parent in o1 depends on o2.
	 * 
	 * @param o1
	 * @param o2
	 * @return
	 * @throws IOException 
	 * @throws IncorrectObjectTypeException 
	 * @throws MissingObjectException 
	 */
	private boolean isCommitAfter(RevCommit o1, RevCommit o2) throws MissingObjectException, IncorrectObjectTypeException, IOException {

		CommitDependency o1Dependency = index.getCommitDependency(o1.getId());
		
		if (o1Dependency.containsParent(o2))
			return true;
		else
			return false;
	}


}
