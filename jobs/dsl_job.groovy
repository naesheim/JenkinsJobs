job("my job"){
  scm {
    github 'naesheim/project'
  }
  steps {
    shell("echo ${GIT_BRANCH}")
  }
  publishers{
    downstream('second_job', 'SUCCESS')
  }
}

job("second_job"){
  steps {
    shell 'echo "test"'
  }
}
