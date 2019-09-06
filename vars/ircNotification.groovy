/** Send a notification to IRC when a build completes
 *
 *  Taken from https://github.com/mozilla/fxtest-jenkins-pipeline and
 *  customized a bit.
*/

def call(String channel = '#rebuild-spam',
         String basenick = 'jenkins',
         String server = 'irc.ocf.berkeley.edu:6697') {
    cleanJobName = JOB_NAME.replaceAll('[^a-zA-Z0-9_-]', '-')
    nick = "${basenick}-${cleanJobName}".take(32)
    result = currentBuild.currentResult

    if (result == 'SUCCESS') {
        // Display success messages in green text on IRC
        resultDisplay = "\u000303Success\u000f"
    } else {
        // Display failure messages in red bold text on IRC and bold text on Slack
        resultDisplay = "\u000304\u0002*${result}*\u000f"
    }

    message = "Project ${JOB_NAME} (#${BUILD_NUMBER}): ${resultDisplay}: ${BUILD_URL}"
    ircMessage(message, nick, channel, server)
}
