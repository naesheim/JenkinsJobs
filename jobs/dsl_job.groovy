job("my job"){
  scm {
    github 'naesheim/VBAdemo'
  }
  steps {
    shell("echo ${GIT_BRANCH}")
  }

  if("${GIT_BRANCH}"=='origin/master'){
    publishers{
      downstream('second_job')
    }
  }
  else{
    publishers{
      downstream('third_job')
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
