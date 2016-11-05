job("my job"){
  scm {
    github 'naesheim/project'
  }
  steps {
    shell("echo ${GIT_BRANCH}")
  }
  publishers{
    if(params["GIT_BRANCH"]=='origin/master'){
      downstream('second_job', 'SUCCESS')
    }
    else{
      downstream('third_job', 'SUCCESS')
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
