/** Send a notification to IRC when a build completes
 *
 *  Taken from https://github.com/mozilla/fxtest-jenkins-pipeline and
 *  customized a bit.
*/

def call(String channel = '#rebuild-spam',
         String basenick = 'jenkins',
				 String server = 'irc.ocf.berkeley.edu:6697') {
    nick = "${basenick}-${JOB_NAME}"
    result = currentBuild.result ?: 'SUCCESS'
    message = "Project ${JOB_NAME} (#${BUILD_NUMBER}): ${result}: ${BUILD_URL}"
    sh """
        (
        echo NICK ${nick}
        echo USER ${nick} 8 * : ${nick}
        sleep 5
        echo "JOIN ${channel}"
        echo "NOTICE ${channel} :${message}"
        echo QUIT
        ) | openssl s_client -connect ${server}
    """
}
