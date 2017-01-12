platforms = ["ubuntu-docker-qt551-small" : "linux", "osx" : "osx", "win7-1-build.novelda.no" : "win32"]
def jobname_base = "radarDSPlibs_" // jobname as it will appear in jenkins
//def gitrepo_url = "https://repo1.novelda.no/scm/xtsw/xtradardsp.git" // where to checkout the code
//def stash_url = "https://repo1.novelda.no/projects/XTSW/repos/xtradardsp/" // gives you the feedback icon in stash of the build result
def jenkins_foldername = "RadarDSPLibs" // defines the foldername in jenkins

def gitrepo_url = ["radarDSP" : "https://repo1.novelda.no/scm/xtsw/xtradardsp.git",
                  "xtserial" : "https://repo1.novelda.no/scm/xtsw/xtserial.git"]// where to checkout the code
//def stash_url = ["radarDSP" : "https://repo1.novelda.no/projects/XTSW/repos/xtmain/",
def stash_url = ["radarDSP" : "https://repo1.novelda.no/projects/XTSW/repos/xtradardsp/",
                "xtserial" : "https://repo1.novelda.no/projects/XTSW/repos/xtserial/"]//folder(jenkins_foldername)
folder(jenkins_foldername) {
  displayName(jenkins_foldername)
  description('This folder contains all jobs for generating dsp libraries for different platforms')
}

platforms.each
{
    def jobname = jobname_base + it.value
    def labelName = it.key
    //def downstream_jobname = downstream_job + it.value
    def platformName = it.value
    println jobname
    job(jenkins_foldername + '/' + jobname)
    {
      label(labelName)

      multiscm {
          git{
              remote {
                  url(gitrepo_url['radarDSP'])
                  credentials('builduser')
                  refspec('+refs/heads/master:refs/remotes/origin/master')
                  pruneBranches()
                  relativeTargetDir("InternalLibs/xtRadarDSP")
              }
              browser { // since 1.26
                  stash(stash_url['radarDSP'])
              }
              branch('*/master')
              wipeOutWorkspace()
          }
          git{
              remote {
                  url(gitrepo_url['xtserial'])
                  credentials('builduser')
                  refspec('+refs/heads/master:refs/remotes/origin/master')
                  pruneBranches()
                  relativeTargetDir("xtserial")
              }
              browser { // since 1.26
                  stash(stash_url['xtserial'])
              }
              branch('*/master')
              wipeOutWorkspace()
          }
      }
      triggers {
          scm('')
      }
      wrappers {
          timestamps()
      }
      steps {
          shell("cd InternalLibs/xtRadarDSP/src; ../gradlew --refresh-dependencies -i publish")
          shell("cd InternalLibs/xtRadarDSP/xtsim; ../gradlew --refresh-dependencies -i publish")

      }
      publishers{
          stashNotifier()
          warnings(['Maven'])
      }
  }
}
