/** Create a GitHub gist via the Java github-api interface
 *  See https://github-api.kohsuke.org/apidocs/org/kohsuke/github/GHGist.html
 *  and
 *  https://github-api.kohsuke.org/apidocs/org/kohsuke/github/GHGistBuilder.html
 *  for which methods are available
 *
 *  Currently this only supports adding single-file gists, but it could have
 *  multi-file support added in the future if that is useful.
*/

import org.kohsuke.github.GHGist
import org.kohsuke.github.GHGistBuilder
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder

def String call(String filename, String content, String credentialsId='ocfbot') {
    node {
        withCredentials([usernamePassword(
            credentialsId: credentialsId,
            passwordVariable: 'GITHUB_PASSWORD',
            usernameVariable: 'GITHUB_USERNAME')
        ]) {
            final GitHub gh = new GitHubBuilder()
                .withPassword(env.GITHUB_USERNAME, env.GITHUB_PASSWORD)
                .build()

            // Start building a gist
            GHGistBuilder gistBuilder = gh.createGist()
            // Add a file to the gist with the given filename and content
            gistBuilder = gistBuilder.file(filename, content)
            // Actually go and create the gist
            GHGist gist = gistBuilder.create()
            // Get a URL with which to refer to the new gist
            return gist.getHtmlUrl().toString()
        }
    }
}
