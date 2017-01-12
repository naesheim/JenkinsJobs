parallel repos.collectEntries {repo -> [/* thread label */repo, {
    node {
        dir('sources') { // switch to subdir
            git url: "https://github.com/user/${repo}"
            sh 'make all -Dtarget=../build'
        }
    }
}]}
