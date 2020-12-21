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
            return ghUser.isMemberOf(ghOrg) || (user == "pre-commit-ci")
        }
    }
}

def call(String org='ocf', String credentialsId='ocfbot') {
    if (!env.CHANGE_AUTHOR) {
        println "This doesn't look like a GitHub PR, continuing"
    } else if (!isOrgMember(env.CHANGE_AUTHOR, org, credentialsId)) {
        ircMessage(
            "Project ${JOB_NAME} (#${BUILD_NUMBER}): " +
            "\u000309\u0002*Pending approval*\u000f, please add " +
            "${env.CHANGE_AUTHOR} to the GitHub org if they are staff or " +
            "check if the PR is malicious before approving: ${BUILD_URL}"
        )
        input(
            message: "${env.CHANGE_AUTHOR} is not in the GitHub org, check " +
                     "the PR contents and log in to approve the build",
            submitter: 'authenticated'
        )
    } else {
        println "${env.CHANGE_AUTHOR} is trusted, continuing"
    }
}
