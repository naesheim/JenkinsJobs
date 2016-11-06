job("my job"){
  scm {
    github 'naesheim/project'
  }
  steps {
    shell("echo ${GIT_BRANCH}")
  }

  if(${GIT_BRANCH}=='origin/release'){
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
