package no.novelda

class Jobs {
    /**
     * A factory for JobDSL methods. Should almost always be the 'this' in the
     * calling scripts' context:
     *
     * >>> import no.novelda.*
     * >>> new Jobs(factory: this, repo: 'x').build()'
     */
    def factory

    /**
     * Which project the repository is located under.
     */
    String project = 'XTSW'

    /**
     * Which repository should be used. This together with `project` generates
     * the git and stash URLs.
     */
    String repo

    /**
     * The build style to be used. See BuildStyle.groovy for a complete list.
     * See _runBuildStyle for implementation.
     *
     * If Map, it should be from arch to BuildStyle, otherwise it should be
     * just BuildStyle.
     */
    def buildStyle = BuildStyle.DOCKER

    /**
     * Reverse dependencies. Used to trigger downstream snapshot builds.
     *
     * * [[project, repo]], e.g. [['XTSW', 'xtDSP'], ['XTSW', 'xtRadarDSP']]
     * * full name, like 'gen_XTSW_misc_snapshot'
     * * Map from arch to one of the above, ['unix': 'gen_X_unittests']
     */
    def revdeps = []

    /**
     * [resulting architecture : machine label]
     *
     * e.g. [win32: 'win10-3-build']
     *
     * arch may be empty if there's only one possibility
     */
    Map<String,String> machines = ['': 'ubuntu-work1.novelda.no']

    String gitUrl(p = null, r = null) {
        if (!p) { p = project }
        if (!r) { r = repo }
        'https://repo1.novelda.no/scm/' + p.toLowerCase() + '/' + r.toLowerCase() + '.git'
    }

    String stashUrl(p = null, r = null) {
        if (!p) { p = project }
        if (!r) { r = repo }
        'https://repo1.novelda.no/projects/' + p + '/repos/' + r
    }

    /**
     * The refspecs we use to listen to upstream changes that should trigger a
     * snapshot build.
     */
    String snapshotRefspec =
        '+refs/heads/master:refs/remotes/origin/master ' +
        '+refs/heads/maint/*:refs/remotes/origin/maint/*' +
        '+refs/heads/build/*:refs/remotes/origin/build/*'

    /**
     * The branches that should trigger a snapshot build.
     */
    String snapshotBranch = ':origin/(master|maint/.*|build/.*)'

    /**
     * The refspecs we use to listen to upstream changes that should trigger a
     * release.
     */
    String releaseRefspec = '+refs/heads/release/*:refs/remotes/origin/release/*'

    /**
     * The branches that should trigger a release.
     */
    String releaseBranch = 'origin/release/*'

    /**
     * Parameters to the gradle build.
     */
    private _gradleParams(Boolean isRelease, arch, machine) {
        "-P release=${isRelease} -P targetArch=${arch}"
    }

    /**
     * Test if the arch and machine specifies a windows machine.
     */
    private _isWindows(arch, machine) {
        arch =~ /^win.*/ || machine =~ /^win.*/
    }

    /**
     * Add a step that runs the build process.
     */
    private _runBuildStyle(it, Boolean isRelease, arch, machine) {
        def extraPath
        if (repo =~ /_thin$/) {
            extraPath = repo.substring(0, repo.length() - 5) + "/"
        } else {
            extraPath = ''
        }
        def lbs
        if (this.buildStyle instanceof Map) {
            lbs = this.buildStyle[arch]
        } else {
            lbs = this.buildStyle
        }
        if (lbs == BuildStyle.DOCKER) {
            it.steps {
                shell('${WORKSPACE}/' + extraPath + 'docker/dockerRun.sh ' +
                    _gradleParams(isRelease, arch, machine))
            }
        } else if (lbs == BuildStyle.GRADLE) {
            def cmd = "--refresh-dependencies publish " +
                _gradleParams(isRelease, arch, machine)
            if (extraPath) {
                extraPath = 'cd ' + extraPath + ' && '
            }
            if (_isWindows(arch, machine)) {
                it.steps {
                    batchFile(extraPath + 'gradlew.bat ' + cmd)
                }
            } else {
                it.steps {
                    shell(extraPath + './gradlew ' + cmd)
                }
            }
        }
    }

    /**
     * Create a trigger for a submodule.
     *
     * Used to make commits to submodules trigger a snapshot build
     * of their parent module (one downstream job per machine).
     *
     * submoduleTrigger() = trigger [project, repo + '_thin']
     * submoduleTrigger('x') = trigger [project, 'x']
     * submoduleTrigger(['proj', 'y']) = trigger ['proj', 'y']
     * submoduleTrigger([['proj', 'y'],['proj', 'x']])
     *     = trigger both ['proj', 'y'] and ['proj', 'x']
     *
     * Use thinBuild to create the jobs for the _thin repository.
     */
    def submoduleTrigger(rds = null) {
        if (!rds) {
            rds = repo + '_thin'
        }
        if (rds instanceof String) {
            rds = [project, rds]
        }
        if (rds[0] instanceof String && rds[1] instanceof String) {
            rds = [rds]
        }
        // XXX: Parameterize which machines it should be built on?

        factory.job('gen_' + project + '_' + repo + '_trigger') {
            scm {
                git {
                    remote {
                        url(gitUrl())
                        credentials('builduser')
                        refspec(snapshotRefspec)
                    }
                    browser {
                        stash(stashUrl())
                    }
                    branch(snapshotBranch)
                }
                triggers {
                    scm('')
                }
            }
            publishers {
                downstreamParameterized {
                    rds.each {toProj, toRepo ->
                        machines.each {arch, _ ->
                            if (arch) {
                                arch = '_' + arch
                            }
                            trigger('gen_' + toProj + '_' + toRepo + arch + '_snapshot') {
                                parameters {
                                    predefinedProp('SUB_BRANCH', '${GIT_BRANCH}')
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Generate a build job for a _thin repository, i.e. one that is mainly a
     * container for various submodules with source dependencies to each other.
     *
     * Calls a specialized form of this.build.
     */
    def thinBuild(ext = null, snapExt = null, releaseExt = null) {
        def tmpRepo = repo
        repo += '_thin'

        build ext, {
            description '''
                Builds the latest version of the submodules.

                Any submoduleTrigger jobs send their current branch in through
                SUB_BRANCH, which this one tries to check out in the *_thin*
                repository.

                % pwd
                /x_thin/submod
                % git checkout -b maint/X.Y
                # If this is close to a release, make sure there are no
                # -SNAPSHOT dependencies, etc. Commit as needed.
                % git push origin maint/X.Y
                % git checkout master
                % sed -i s/version=X\\.Y.*/version=X.$(( Y + 1 )).0/ build.properties
                % git commit -am "bump master version"
                % git push origin master
                % git checkout maint/X.Y
                # In the meantime the build system should have created a branch
                # maint/X.Y in the _thin repo:
                % cd ..
                % git fetch
                % git checkout maint/X.Y
                % git submodule status
                # Check that everything is OK for a release, and then:
                % git push maint/X.Y:release/X.Y.Z
            '''.stripIndent().trim()

            /* Exclude triggering builds based on commits made by this build
             * type:
             */
            // configure { node ->
            //     node / 'scm' / 'extensions' /
            //             'hudson.plugins.git.extensions.impl.MessageExclusion' {
            //         excludedMessage(/jenkins job \d*/)
            //     }
            // }

            // This does not work, probably because of a bug in the current
            // jenkins version. Kept here in case we ever upgrade.

            parameters {
                stringParam(
                    'SUB_BRANCH',
                    '',
                    'The branch of the submodule that triggered the build, if any.'
                )
            }

            steps {
                shell '''
                    set -e

                    [ -n "${SUB_BRANCH}" ] && git checkout -B ${SUB_BRANCH#origin/}
                    REFERENCE=""
                    if [ -f .git/objects/info/alternates ]; then
                        REFERENCE="$(cat .git/objects/info/alternates)"
                        REFERENCE="${REFERENCE%/objects}"
                        REFERENCE="--reference ${REFERENCE%/.git}"
                    fi

                    sleepStep() {
                      n=0
                      until [ $n -ge 5 ]; do
                          pushCurrentModules && break
                          n=$(( n + 1 ))
                          sleep 15
                          git fetch
                          git checkout ${SUB_BRANCH:-origin/master}
                      done

                    }

                    pushCurrentModules() {
                        git submodule update $REFERENCE --recursive --init
                        git submodule foreach 'git checkout "${SUB_BRANCH:-crash_please}" || git checkout origin/master'
                        if git commit -am "jenkins job ${BUILD_NUMBER}"; then
                            TMP=${SUB_BRANCH#origin/}
                            git push origin HEAD:${TMP:-${GIT_BRANCH#origin/}}
                        fi
                    }
                    fetchCustomModules(){
                      git submodule update --init --recursive
                    }

                    case "$SUB_BRANCH" in
                      *build*) fetchCustomModules ;;
                      *) sleepStep ;;
                    esac


                    # Special _thin stuff, like applying patches in order to
                    # build cleanly.
                    if [ -r prebuild.sh ]; then sh prebuild.sh; fi
                '''.stripIndent().trim()
            }

            if (snapExt) {
                snapExt.delegate = it
                snapExt(it)
            }
        }, {
            description '''
                Builds the version of the submodules commited in the release
                branch.
            '''.stripIndent().trim()

            steps {
                shell '''
                    git submodule update --init --recursive
                '''.stripIndent().trim()
            }
            if (releaseExt) {
                releaseExt.delegate = it
                releaseExt(it)
            }
        }

        repo = tmpRepo
    }

    /**
     * Create two build jobs for every machine, one for snapshots and one for
     * releases.
     */
    def build(ext = null, snapExt = null, releaseExt = null) {
        def nvJob = {mode, rs, branches, arch, machine, ext2 ->
            def narch = arch
            if (narch) {
                narch = '_' + narch
            }
            def lrevdeps
            if (revdeps instanceof Map) {
                if (revdeps[arch]) {
                    lrevdeps = revdeps[arch]
                }
            } else {
                lrevdeps = revdeps
            }
            factory.job('gen_' + project + '_' + repo + narch + '_' + mode) {
                description """
                    Build the ${mode} of
                    ${project}/${repo}${arch ? ' (' + arch + ')' : ''}.
                """.stripIndent().trim()

                label(machine)

                scm {
                    git {
                        remote {
                            url(gitUrl())
                            credentials('builduser')
                            refspec(rs)
                        }
                        browser {
                            stash(stashUrl())
                        }
                        branch(branches)

                        try {
                            wipeOutWorkspace(true)
                        } catch (MissingMethodException e) {
                            extensions {
                                wipeOutWorkspace()
                            }
                        }
                        /*
                         * Use a reference repo to speed up clones.
                         *
                         * Git objects are cached here and simply referenced,
                         * instead of being downloaded over network every time.
                         * In personal repos this may be a problem, because if
                         * the reference repository is deleted, the repository
                         * that uses it becomes unusable. Here, where we
                         * always do complete clones, it should however be
                         * safe. If it doesn't exist, Jenkins is able to do
                         * without it. It can thus be created on demand.
                         *
                         * The reference repository must be updated manually.
                         * TODO: automatically, through a jenkins job.
                         */
                        configure { node ->
                            node / 'extensions' / 'hudson.plugins.git.extensions.impl.CloneOption' {
                                if (_isWindows(arch, machine)) {
                                    reference 'C:/git-reference-repo'
                                } else {
                                    reference '/home/builduser/.git-reference-repo'
                                }
                            }
                        }
                    }
                }

                triggers {
                    scm('')
                }

                steps {
                    shell '''
                        git describe --tags || :
                        g++ --version || :
                        gcc --version || :
                        ldd --version || :
                        make --version || :
                        mingw32-make --version || :
                        python --version || :
                    '''.stripIndent().trim()
                }

                if (mode == 'snapshot') {
                    publishers {
                        lrevdeps.each {
                            if (it instanceof List) {
                                downstream('gen_' + it[0] + '_' + it[1] + narch + '_snapshot')
                            } else {
                                downstream(it)
                            }
                        }
                    }
                }

                if (ext) {
                    ext.delegate = it
                    ext(it)
                }

                if (ext2) {
                    ext2.delegate = it
                    ext2(it)
                }

                _runBuildStyle(it, mode == 'release', arch, machine)
            }
        }

        machines.each {arch, machine ->
            nvJob('snapshot', snapshotRefspec, snapshotBranch, arch, machine, snapExt)
            nvJob('release', releaseRefspec, releaseBranch, arch, machine, releaseExt)
        }
    }
}
