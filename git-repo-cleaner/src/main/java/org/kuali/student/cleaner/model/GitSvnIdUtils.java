package org.kuali.student.cleaner.model;

/**
 * Created by ocleirig on 3/30/2016.
 */
public final class GitSvnIdUtils {

    private GitSvnIdUtils() {
    }

    public static String applyPathToExistingGitSvnId (String originalCommitMessage, String additionalPath) {

        StringBuilder builder = new StringBuilder(originalCommitMessage);

        int index = builder.indexOf("git-svn-id:");

        int atRevisionIndex = builder.indexOf("@", index);

        String prefix = additionalPath.charAt(0) == '/'?"":"/";

        builder = builder.insert(atRevisionIndex, prefix + additionalPath);

        return builder.toString();
    }
}
