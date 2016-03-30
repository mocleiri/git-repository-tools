package org.kuali.student.cleaner;

import org.kuali.student.cleaner.model.GitSvnIdUtils;

import static org.junit.Assert.assertEquals;

/**
 * Created by ocleirig on 3/30/2016.
 */
@org.junit.runner.RunWith (value = org.junit.runners.BlockJUnit4ClassRunner.class)
public class TestRewriteGitSvnUrl {


    @org.junit.Test
    public void testRewriteGitSvnId () {

        String message = "[maven-release-plugin] prepare for next development iteration\n" +
                "\n" +
                "    git-svn-id: https://svn.jenkins-ci.org/trunk@41410 71c3de6d-444a-0410-be80-ed276b4c234a\n";

        String expectedMessage = "[maven-release-plugin] prepare for next development iteration\n" +
                "\n" +
                "    git-svn-id: https://svn.jenkins-ci.org/trunk/src/modules/my-module@41410 71c3de6d-444a-0410-be80-ed276b4c234a\n";

        String convertedMessage = GitSvnIdUtils.applyPathToExistingGitSvnId(message, "src/modules/my-module");

        assertEquals(expectedMessage, convertedMessage);

    }
}
