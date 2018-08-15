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
                .withOAuthToken(env.GITHUB_PASSWORD, env.GITHUB_USERNAME)
                .build()

            final GHUser ghUser = gh.getUser(user)
            final GHOrganization ghOrg = gh.getOrganization(org)
            return ghUser.isMemberOf(ghOrg)
        }
    }
}

def call(String org='ocf', String credentialsId='ocfbot') {
    if (!env.CHANGE_AUTHOR) {
        println "This doesn't look like a GitHub PR, continuing"
    } else if (!isOrgMember(env.CHANGE_AUTHOR, org, credentialsId)) {
        input(
            message: "Trusted approval needed for change from ${env.CHANGE_AUTHOR}",
            submitter: 'authenticated'
        )
    } else {
        println "${env.CHANGE_AUTHOR} is trusted, continuing"
    }
}
