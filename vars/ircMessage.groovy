/** Send a generic message to IRC
*/

def call(String message,
         String nick = null,
         String channel = '#rebuild-spam',
         String server = 'irc.ocf.berkeley.edu:6697') {
    if (nick == null) {
        cleanJobName = JOB_NAME.replaceAll('[^a-zA-Z0-9_-]', '-')
        nick = "jenkins-${cleanJobName}".take(32)
    }

    sh """
        (
        echo NICK ${nick}
        echo USER ${nick} 8 * : ${nick}
        sleep 5
        echo "JOIN ${channel}"
        echo "PRIVMSG ${channel} :${message}"
        echo QUIT
        ) | chronic openssl s_client -connect ${server}
    """
}
