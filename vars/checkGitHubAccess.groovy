/** Check if someone making a pull request is a collaborator before testing
 *  their PR, otherwise require someone in the OCF with access to go into
 *  Jenkins and approve the Jenkins run. Taken mostly from:
 *  https://blog.grdryn.me/blog/jenkins-pipeline-trust.html and the code in
 *  https://github.com/feedhenry/fh-pipeline-library/blob/master/vars/enforceTrustedApproval.groovy
*/

import org.kohsuke.github.GHOrganization
import org.kohsuke.github.GHUser
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder

final boolean isOrgMember(String user, String org, String credentialsId) {
    node {
        withCredentials([usernamePassword(
            credentialsId: credentialsId,
            passwordVariable: 'GITHUB_PASSWORD',
            usernameVariable: 'GITHUB_USERNAME')
        ]) {
            final GitHub gh = new GitHubBuilder()
                .withPassword(env.GITHUB_USERNAME, env.GITHUB_PASSWORD)
                .build()

            final GHUser ghUser = gh.getUser(user)
            final GHOrganization ghOrg = gh.getOrganization(org)
            return ghUser.isMemberOf(ghOrg)
        }
    }
}

def call(String org='ocf', String credentialsId='ocfbot') {
    def author = env.CHANGE_AUTHOR

    // These are bots that are not part of the ocf GitHub org, but we
    // want to run tests on the pull requests they open anyway and
    // trust them not to make pull requests trying to exploit Jenkins.
    def allowedBots = ['pre-commit-ci[bot]', 'dependabot[bot]']

    if (!author) {
        println "This doesn't look like a GitHub PR, continuing"
    } else if (allowedBots.contains(author)) {
        println "${author} is an allowed bot, continuing"
    } else if (isOrgMember(author, org, credentialsId)) {
        println "${author} is a trusted org member, continuing"
    } else {
        ircMessage(
            "Project ${JOB_NAME} (#${BUILD_NUMBER}): " +
            "\u000309\u0002*Pending approval*\u000f, please add " +
            "${author} to the GitHub org if they are staff or " +
            "check if the PR is malicious before approving: ${BUILD_URL}"
        )
        input(
            message: "${author} is not in the GitHub org, check " +
                     "the PR contents and log in to approve the build",
            submitter: 'authenticated'
        )
    }
}
