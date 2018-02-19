/**
 * Trigger Marathon to deploy an app/service, generally the latest version of
 * the job that has just been built.
*/

def call(String app, String version) {
    sh "ocf-marathon deploy-app \"$app\" \"$version\""
}
