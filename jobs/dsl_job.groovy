job("my job"){
  scm {
    github 'naesheim/project'
  }
  steps {
    shell("echo ${GIT_BRANCH}")
  }
  publishers{
    downstreamParameterized {
      trigger('third_job'){
        condition(${GIT_BRANCH}=='origin/master')
        triggerWithNoParameters(true)
      }
      trigger('second_job'){
        condition(${GIT_BRANCH}=='origin/relase')
        triggerWithNoParameters(true)
      }
    }
  }
}

job("second_job"){
  steps {
    shell 'echo "this is the second job"'
  }
}

job("third_job"){
  steps {
    shell('echo "this is the third job"')
  }
}
