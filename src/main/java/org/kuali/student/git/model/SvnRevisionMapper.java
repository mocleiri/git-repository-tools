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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.kuali.student.git.utils.GitBranchUtils;
import org.kuali.student.git.utils.GitBranchUtils.ILargeBranchNameProvider;
import org.kuali.student.svn.tools.merge.model.BranchData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Kuali Student Team
 * 
 */
public class SvnRevisionMapper implements ILargeBranchNameProvider {

	private static final Logger log = LoggerFactory
			.getLogger(SvnRevisionMapper.class);

	private static final String REVISION_MAP_FILE_NAME = "revisions.map";

	private static final String REVISION_MAP_INDEX_FILE_NAME = "revisions.idx";
	
	private static final String REVISION_BRANCH_MERGE_FILE_NAME = "merge.map";

	private static final String REVISION_BRANCH_MERGE_INDEX_FILE_NAME = "merge.idx";

	
	
	public static class SvnRevisionMap {
		private long revision;
		private String branchName;
		private String branchPath;
		private String commitId;

		/**
		 * @param branchName
		 * @param commitId
		 */
		public SvnRevisionMap(long revision, String branchName,
				String branchPath, String commitId) {
			super();
			this.revision = revision;
			this.branchName = branchName;
			this.branchPath = branchPath;
			this.commitId = commitId;
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

		/**
		 * @return the commitId
		 */
		public String getCommitId() {
			return commitId;
		}

		/**
		 * @return the revision
		 */
		public long getRevision() {
			return revision;
		}

		/**
		 * @param revision
		 *            the revision to set
		 */
		public void setRevision(long revision) {
			this.revision = revision;
		}

	}
	
	private static class RevisionMapOffset {
		private long revision;
		private long startBtyeOffset;
		private long totalBytes;
		/**
		 * @param revision
		 * @param startBtyeOffset
		 * @param totalBytes
		 */
		public RevisionMapOffset(long revision, long startBtyeOffset,
				long totalBytes) {
			super();
			this.revision = revision;
			this.startBtyeOffset = startBtyeOffset;
			this.totalBytes = totalBytes;
		}
		/**
		 * @return the revision
		 */
		public long getRevision() {
			return revision;
		}
		/**
		 * @return the startBtyeOffset
		 */
		public long getStartBtyeOffset() {
			return startBtyeOffset;
		}
		/**
		 * @return the totalBytes
		 */
		public long getTotalBytes() {
			return totalBytes;
		}
		
		
	}

	private File revisonMappings;
	
	private TreeMap<String, RevisionMapOffset>revisionMap = new TreeMap<>();

	private File revisionMapDataFile;

	private File revisionMapIndexFile;

	private PrintWriter revisionMapIndexWriter;

	private RandomAccessFile revisionMapDataRandomAccessFile;

	private long endOfRevisionMapDataFileInBytes;
	
	private File revisionBranchMergeDataFile;
	private File revisionBranchMergeIndexFile;
	
	private PrintWriter revisionBranchMergeIndexWriter;

	private RandomAccessFile revisionBranchMergeDataRandomAccessFile;

	private long endOfRevisionBranchMergeDataFileInBytes;
	
	private TreeMap<String, Map<String, RevisionMapOffset>>revionMergeMap = new TreeMap<>();

	private GitTreeProcessor treeProcessor;

	

	
	/**
	 * 
	 */
	public SvnRevisionMapper(Repository repo) {
		
		treeProcessor = new GitTreeProcessor(repo);

		revisonMappings = new File(repo.getDirectory(), "jsvn");

		revisonMappings.mkdirs();

		revisionMapDataFile = new File(revisonMappings, REVISION_MAP_FILE_NAME);
		
		revisionMapIndexFile = new File(revisonMappings, REVISION_MAP_INDEX_FILE_NAME);
		
		revisionBranchMergeDataFile = new File(revisonMappings, REVISION_BRANCH_MERGE_FILE_NAME);
		
		revisionBranchMergeIndexFile = new File(revisonMappings, REVISION_BRANCH_MERGE_INDEX_FILE_NAME);
		
		
	}
	
	public void initialize () throws IOException {
		
		// tracks the branch heads at each revision
		revisionMapDataRandomAccessFile = new RandomAccessFile(revisionMapDataFile, "rws");
		
		if (revisionMapIndexFile.exists()) {
			// load in any existing data.
			loadRevisionMapIndexData();
		}
		
		endOfRevisionMapDataFileInBytes = revisionMapDataFile.length();
		
		revisionMapIndexWriter = new PrintWriter(new FileOutputStream(revisionMapIndexFile, true));
		
		// tracks the merge info of each branch at each revision
		// used so we can compute the delta.
		revisionBranchMergeDataRandomAccessFile = new RandomAccessFile(revisionBranchMergeDataFile, "rws");
		
		if (revisionBranchMergeIndexFile.exists()) {
			// load in any existing data.
			loadRevisionMergeIndexData();
		}
		
		endOfRevisionBranchMergeDataFileInBytes = revisionBranchMergeDataFile.length();
		
		revisionBranchMergeIndexWriter = new PrintWriter(new FileOutputStream(revisionBranchMergeIndexFile, true));
		
		
	}
	public void shutdown() throws IOException {
		
		revisionMapIndexWriter.flush();
		revisionMapIndexWriter.close();
		
		revisionMapDataRandomAccessFile.close();
		
		revisionBranchMergeIndexWriter.flush();
		revisionBranchMergeIndexWriter.close();
		
		revisionBranchMergeDataRandomAccessFile.close();
		
		
	}

	private void loadRevisionMergeIndexData () throws IOException {
		
		BufferedReader indexReader = new BufferedReader(new InputStreamReader(
				new FileInputStream(revisionBranchMergeIndexFile)));


		while (true) {

			String line = indexReader.readLine();

			if (line == null)
				break;

				String parts[] = line.split("::");

				if (parts.length != 3)
					continue;

				long revision = Long.parseLong(parts[0]);
				String targetBranch = parts[1];
				long byteStartOffset = Long.parseLong(parts[2]);
				long totalbytes = Long.parseLong(parts[3]);
				
				Map<String, RevisionMapOffset> targetBranchOffsetMap = getRevisionMergeDataByTargetBranch (parts[0], true);
				
				targetBranchOffsetMap.put(targetBranch, new RevisionMapOffset(revision, byteStartOffset, totalbytes));


		}

		indexReader.close();
	}
	
	
	private Map<String, RevisionMapOffset> getRevisionMergeDataByTargetBranch(String revisionString, boolean createIfDoesNotExist) {

		Map<String, RevisionMapOffset>targetBranchOffsetMap = revionMergeMap.get(revisionString);
		
		if (targetBranchOffsetMap == null && createIfDoesNotExist) {
			targetBranchOffsetMap = new HashMap<String, SvnRevisionMapper.RevisionMapOffset>();
			revionMergeMap.put(revisionString, targetBranchOffsetMap);
		}
		
		return targetBranchOffsetMap;
	}

	private void loadRevisionMapIndexData() throws IOException {
		
		BufferedReader indexReader = new BufferedReader(new InputStreamReader(
				new FileInputStream(revisionMapIndexFile)));


		while (true) {

			String line = indexReader.readLine();

			if (line == null)
				break;

				String parts[] = line.split("::");

				if (parts.length != 3)
					continue;

				long revision = Long.parseLong(parts[0]);
				long byteStartOffset = Long.parseLong(parts[1]);
				long totalbytes = Long.parseLong(parts[2]);
				
				revisionMap.put(parts[0], new RevisionMapOffset(revision, byteStartOffset, totalbytes));


		}

		indexReader.close();
		
	}
	
	/*
	 * returns the total number of bytes written to the data file
	 */
	private long createRevisionEntry (RandomAccessFile dataFile, long endOfDataFileOffset, long revision, List<String>revisionLines) throws IOException {
		
		OutputStream revisionMappingStream = null;
		
		ByteArrayOutputStream bytesOut;
		
		revisionMappingStream = 
					new BZip2CompressorOutputStream(bytesOut = new ByteArrayOutputStream());

		PrintWriter pw = new PrintWriter(revisionMappingStream);

		IOUtils.writeLines(revisionLines, "\n", pw);

		pw.flush();
		
		pw.close();
		
		byte[] data = bytesOut.toByteArray();
		
		dataFile.seek(endOfDataFileOffset);

		dataFile.write(data);
		
		return data.length;
	}
	
	private void createRevisionMapEntry (long revision, List<String>branchHeadLines) throws IOException {
		
		long bytesWritten = createRevisionEntry(revisionMapDataRandomAccessFile, endOfRevisionMapDataFileInBytes, revision, branchHeadLines);
		
		/*
		 * Write the number of bytes written for this revision.
		 */

		updateRevisionMapIndex(revision, endOfRevisionMapDataFileInBytes, bytesWritten);
		
		endOfRevisionMapDataFileInBytes += bytesWritten;
		
		
	}

	public void createRevisionMap(long revision, List<Ref> branchHeads)
			throws IOException {

		List<String>branchHeadLines = new ArrayList<>(branchHeads.size());
		
		for (Ref branchHead : branchHeads) {
			/*
			 * Only archive active branches. skip those containing @
			 */
			if (!branchHead.getName().contains("@"))
				branchHeadLines.add(revision + "::" + branchHead.getName() + "::"
						+ branchHead.getObjectId().name());
		}
		
		
		createRevisionMapEntry(revision, branchHeadLines);
		
	}

	private void updateRevisionMapIndex(long revision, long revisionStartByteIndex, long bytesWritten) {
		
		revisionMap.put(String.valueOf(revision), new RevisionMapOffset(revision, revisionStartByteIndex, bytesWritten));
		
		revisionMapIndexWriter.println(revision + "::" + revisionStartByteIndex + "::"
				+ bytesWritten);

		revisionMapIndexWriter.flush();
	}
	
	private void updateMergeDataIndex(long revision, String targetBranchName, List<BranchMergeInfo> mergeInfo, long revisionStartByteIndex, long bytesWritten) {
		
		String revisionString = String.valueOf(revision);
		
		Map<String, RevisionMapOffset> targetRevisionMap = getRevisionMergeDataByTargetBranch(revisionString, true);
		
		targetRevisionMap.put(targetBranchName, new RevisionMapOffset(revision, revisionStartByteIndex, bytesWritten));
		
		revisionBranchMergeIndexWriter.println(revision + "::" + targetBranchName + "::" + revisionStartByteIndex + "::"
				+ bytesWritten);

		revisionBranchMergeIndexWriter.flush();
	}
	
	private void updateIndex(Map<String, RevisionMapOffset>revisionMap, PrintWriter indexWriter, long revision, long revisionStartByteIndex, long bytesWritten) {
		revisionMap.put(String.valueOf(revision), new RevisionMapOffset(revision, revisionStartByteIndex, bytesWritten));
		
		indexWriter.println(revision + "::" + revisionStartByteIndex + "::"
				+ bytesWritten);

		indexWriter.flush();
		
	}

	/**
	 * Get the list of all references at the svn revision number given.
	 * 
	 * @param revision
	 * @return
	 * @throws IOException
	 */
	public List<SvnRevisionMap> getRevisionHeads(long revision)
			throws IOException {

		InputStream inputStream = getRevisionInputStream(revision);
		
		if (inputStream == null)
			return null;

		List<String> lines = IOUtils.readLines(inputStream, "UTF-8");
		
		inputStream.close();
		
		String revisionString = String.valueOf(revision);

		List<SvnRevisionMap> revisionHeads = new ArrayList<SvnRevisionMap>();

		for (String line : lines) {

			String[] parts = line.split("::");
			
			if (!parts[0].equals(revisionString)) {
				log.warn(parts[0] + " is not a line for " + revisionString);
				continue;
			}

			String branchName = parts[1];

			String commitId = parts[2];

			String branchPath = GitBranchUtils.getBranchPath(branchName,
					revision, this);

			revisionHeads.add(new SvnRevisionMap(revision, branchName,
					branchPath, commitId));

		}

		return revisionHeads;

	}
	
	private InputStream getRevisionInputStream(final long revision)
			throws IOException {

		return getInputStream(new RevisionMapOffsetProvider() {
			
			@Override
			public RevisionMapOffset getRevisionMapOffset() {
				
				return revisionMap.get(String.valueOf(revision));
			}
		}, revisionMapDataRandomAccessFile);
		
	}
	
	private InputStream getMergeDataInputStream(final long revision, final String targetBranch) throws IOException {
		return getInputStream(new RevisionMapOffsetProvider() {
			
			@Override
			public RevisionMapOffset getRevisionMapOffset() {
				
				Map<String, RevisionMapOffset> map = revionMergeMap.get(String.valueOf(revision));
				
				if (map == null)
					return null;
				
				return map.get(targetBranch);
				
			}
		}, revisionBranchMergeDataRandomAccessFile);
	}
	
	private static interface RevisionMapOffsetProvider {
		public RevisionMapOffset getRevisionMapOffset ();
	};
	
	private InputStream getInputStream (RevisionMapOffsetProvider offsetProvider, RandomAccessFile dataFile) throws IOException {
		
		RevisionMapOffset revisionOffset = offsetProvider.getRevisionMapOffset();

		if (revisionOffset == null)
			return null;

		byte[] data = new byte[(int) revisionOffset.getTotalBytes()];
		
		dataFile.seek(revisionOffset.getStartBtyeOffset());
		
		dataFile.readFully(data);

		return new BZip2CompressorInputStream(new ByteArrayInputStream(data));

	}

	/**
	 * Get the object id of the commit refered to by the branch at the revision
	 * given.
	 * 
	 * @param revision
	 * @param branchName
	 * @return
	 * @throws IOException
	 */
	public ObjectId getRevisionBranchHead(long revision, String branchName)
			throws IOException {

		InputStream inputStream = getRevisionInputStream(revision);
		
		if (inputStream == null)
			return null;

		List<String> lines = IOUtils.readLines(inputStream, "UTF-8");
		
		inputStream.close();
		
		String revisionString = String.valueOf(revision);
		
		for (String line : lines) {

			String[] parts = line.split("::");

			if (!parts[0].equals(revisionString)) {
				log.warn("incorrect version");
				continue;
			}
			
			if (parts[1].equals(Constants.R_HEADS + branchName)) {
				ObjectId id = ObjectId.fromString(parts[2]);

				return id;

			}

		}

		// this is actually an exceptional case
		// if not found it means that the reference can't be found.
		return null;
	}
	
	/*
	 * When we compute the list of revisions for a path its useful to know what the matched subpath was.
	 */
	public static class SvnRevisionMapResults {
		
		public SvnRevisionMapResults(SvnRevisionMap revMap) {
			this (revMap, "");
		}
		
		public SvnRevisionMapResults (SvnRevisionMap revMap, String subPath) {
			this.revMap = revMap;
			this.subPath = subPath;
		}

		private final SvnRevisionMap revMap;
		
		private final String subPath;

		/**
		 * @return the revMap
		 */
		public SvnRevisionMap getRevMap() {
			return revMap;
		}

		/**
		 * @return the subPath
		 */
		public String getSubPath() {
			return subPath;
		}

	
		
	}

	public List<SvnRevisionMapResults> getRevisionBranches(long targetRevision,
			String targetPath) throws IOException {

		ArrayList<SvnRevisionMapResults> branches = new ArrayList<>();

		List<SvnRevisionMap> heads = this
				.getRevisionHeads(targetRevision);
		
		if (heads == null)
			return branches;

		for (SvnRevisionMap revMap : heads) {

			SvnRevisionMapResults results = findResults(revMap, targetPath);
			
			if (results != null)
				branches.add(results);

		}

		return branches;
	}

	private SvnRevisionMapResults findResults(SvnRevisionMap revMap, String copyFromPath) {

		/*
		 * In most cases the match is because the copyFromPath is an actual branch.
		 * 
		 * In other cases it is a prefix that can match several branches
		 * 
		 * In a few cases it will refer to a branch and then a subpath within it it.
		 * 
		 */
		String candidateBranchPath = revMap.getBranchPath().substring(Constants.R_HEADS.length());
		
		if (candidateBranchPath.startsWith(copyFromPath))
			return new SvnRevisionMapResults (revMap); // the common case
		
		String candidateBranchParts[] = candidateBranchPath.split("\\/");
		
		String copyFromPathParts[] = copyFromPath.split("\\/");
		
		int smallestLength = Math.min(candidateBranchParts.length, copyFromPathParts.length);
		
		boolean allEquals = true;
		
		for (int i = 0; i < smallestLength; i++) {
			
			String candidatePart = candidateBranchParts[i];
			String copyFromPart = copyFromPathParts[i];
			
			if (!copyFromPart.equals(candidatePart)) {
				allEquals = false;
				break;
			}
			
		}
		
		if (allEquals && copyFromPathParts.length > smallestLength) {
			// check inside of the branch for the rest of the path
			ObjectId commitId = ObjectId.fromString(revMap.getCommitId());
			
			String insidePath = StringUtils.join(copyFromPathParts, "/", smallestLength, copyFromPathParts.length);
			
			try {
				if (treeProcessor.treeContainsPath(commitId, insidePath)) {
					return new SvnRevisionMapResults(revMap, insidePath);
				}
				// fall through
			} catch (Exception e) {
				log.error("Failed to find paths for commit {}", commitId);
				// fall through
			}
		}
		
		return null;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.kuali.student.git.utils.GitBranchUtils.ILargeBranchNameProvider#
	 * getBranchName(java.lang.String, long)
	 */
	@Override
	public String getBranchName(String longBranchId, long revision) {

		try {
			File revisionFile = new File(revisonMappings, "r" + revision
					+ "-large-branches");

			List<String> lines = FileUtils.readLines(revisionFile, "UTF-8");

			for (String line : lines) {

				String[] parts = line.split("::");

				if (parts.length != 2) {
					continue;
				}

				if (parts[0].equals(longBranchId)) {
					return parts[1].trim();
				}

			}

			// not found
			return null;
		} catch (IOException e) {
			log.warn("failed to find longbranch for id = " + longBranchId);
			return null;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.kuali.student.git.utils.GitBranchUtils.ILargeBranchNameProvider#
	 * storeLargeBranchName(java.lang.String, java.lang.String, long)
	 */
	@Override
	public String storeLargeBranchName(String branchName, long revision) {

		try {
			ObjectId largeBranchNameId = GitBranchUtils
					.getBranchNameObjectId(branchName);

			String existingBranchName = getBranchName(largeBranchNameId.name(),
					revision);

			if (existingBranchName != null)
				return largeBranchNameId.getName();

			File revisionFile = new File(revisonMappings, "r" + revision
					+ "-large-branches");

			PrintWriter pw = new PrintWriter(new FileOutputStream(revisionFile,
					true));

			pw.println(largeBranchNameId.name() + "::" + branchName);

			pw.flush();
			pw.close();

			return largeBranchNameId.name();
		} catch (FileNotFoundException e) {
			log.warn("storeLargeBranchName: failed to open r" + revision
					+ "-large-branches");
			return null;
		}
	}

	public void repackMapFile() throws IOException {
		
		// close the data file
		revisionMapDataRandomAccessFile.close();
		
		
		// close the index file
		revisionMapIndexWriter.close();
		
		revisionMapIndexFile.delete();
		
		endOfRevisionMapDataFileInBytes = 0L;
		
		revisionMapIndexWriter = new PrintWriter(new FileOutputStream(new File(revisonMappings,
				REVISION_MAP_INDEX_FILE_NAME), true));
		
		// clear the in memory index
		revisionMap.clear();
		
		File copy = new File(revisonMappings, "repack-source.dat");
		
		FileUtils.copyFile(revisionMapDataFile, copy);
		
		revisionMapDataFile.delete();
		
		revisionMapDataRandomAccessFile = new RandomAccessFile(revisionMapDataFile, "rws");
		
		BufferedReader reader = new BufferedReader (new InputStreamReader(new BZip2CompressorInputStream(new FileInputStream(copy), true)));
		
		String currentRevision = null;
		
		List<String>currentRevisionHeads = new ArrayList<String>();
		
		while (true) {

			String line = reader.readLine();
			
			if (line == null) {
				if (currentRevision != null) {
					// archive the last revision
					createRevisionMapEntry(Long.parseLong(currentRevision), currentRevisionHeads);
					
				}
				break;
			}
		
			String parts[] = line.split ("::");
			
			String revisionString = parts[0];
			
			if (currentRevision == null)
				currentRevision = revisionString;
			
			if (!currentRevision.equals(revisionString)) {
				
				// write the revision data and update the index file
				createRevisionMapEntry(Long.parseLong(currentRevision), currentRevisionHeads);
				
				currentRevision = revisionString;
				
				currentRevisionHeads.clear();
				
			}
			
			currentRevisionHeads.add(line);
			
			
		}
		
		reader.close();
		
		copy.delete();
		
		
	}

	public void createMergeData(long revision, String targetBranch, List<BranchMergeInfo>mergeInfo) throws IOException {
		
		List<String>dataLines = new LinkedList<>();
		/*
		 * Format: revision :: target branch name :: merge branch name :: revision_1 , revision_2, .. revision_n.
		 */
		for (BranchMergeInfo bmi : mergeInfo) {
		
			List<String>lineParts = new LinkedList<>();
			
			lineParts.add(String.valueOf (revision));
			
			lineParts.add(targetBranch);
			
			lineParts.add(bmi.getBranchName());
			
			lineParts.add(StringUtils.join(bmi.getMergedRevisions().iterator(), ","));
			
			dataLines.add(StringUtils.join(lineParts, "::"));
			
			
		}
		
		
		long bytesWritten = createRevisionEntry(revisionBranchMergeDataRandomAccessFile, endOfRevisionBranchMergeDataFileInBytes, revision, dataLines);
		
		/*
		 * Write the number of bytes written for this revision.
		 */

		updateMergeDataIndex(revision, targetBranch, mergeInfo, endOfRevisionBranchMergeDataFileInBytes, bytesWritten);
		
		endOfRevisionBranchMergeDataFileInBytes += bytesWritten;
		
	}
	
	private BranchMergeInfo extractBranchMergeInfoFromLine (String branchName, String revisionParts[]) {

		BranchMergeInfo bmi = new BranchMergeInfo(branchName);
		
		for (String revisionString : revisionParts) {
			
			bmi.addMergeRevision(Long.valueOf(revisionString));
		}
		
		return bmi;
		
	}

	/**
	 * Get the list of branch merge info for the revision and target branch given.
	 * @param revision
	 * @param targetBranch
	 * @return
	 * @throws IOException
	 */
	public List<BranchMergeInfo>getMergeBranches(long revision, String targetBranch) throws IOException {
		
		List<BranchMergeInfo>bmiList = new LinkedList<>();
		
		InputStream inputStream = getMergeDataInputStream(revision, targetBranch);
		
		if (inputStream == null)
			return null;

		List<String> lines = IOUtils.readLines(inputStream, "UTF-8");
		
		inputStream.close();
		
		String revisionString = String.valueOf(revision);

		for (String line : lines) {

			String[] parts = line.split("::");
			
			if (!parts[0].equals(revisionString)) {
				log.warn(parts[0] + " is not a line for " + revisionString);
				continue;
			}

			String targetBranchName = parts[1];
			
			
			if (targetBranch.equals(targetBranchName)) {
				
				String mergeBranchName = parts[2];
		
				String mergedRevisionStrings[] = parts[3].split(",");
				
				BranchMergeInfo bmi = extractBranchMergeInfoFromLine(mergeBranchName, mergedRevisionStrings);

				bmiList.add(bmi);
				
			}
			else {
				log.warn(line + " is not a valid line for revision {} and target branch {}", revision, targetBranch);
			}
				
		}

		return bmiList;

	}
	
	public Set<Long> getMergeBranchRevisions(long revision, String targetBranch, String mergeBranch) throws IOException {
		
		List<BranchMergeInfo> bmiList = getMergeBranches(revision, targetBranch);
		
		for (BranchMergeInfo bmi : bmiList) {
			
			if (bmi.getBranchName().equals(mergeBranch)) {
				return bmi.getMergedRevisions();
			}
		}
		
		// no matches found.
		return new HashSet<>();
		
	}

}
