/** Send an email notification when a build fails */

def call(String email = 'root@ocf.berkeley.edu') {
    result = currentBuild.result ?: 'SUCCESS'
    subject = "${JOB_NAME} - Build #${BUILD_NUMBER} - Failure!"
    message = "Project ${JOB_NAME} (#${BUILD_NUMBER}): ${result}: ${BUILD_URL}"

    if (BRANCH_NAME == 'master') {
        mail to: email, subject: subject, body: message
    } else {
        mail to: emailextrecipients([
            [$class: 'CulpritsRecipientProvider'],
            [$class: 'DevelopersRecipientProvider']
        ]), subject: subject, body: message
    }
}
