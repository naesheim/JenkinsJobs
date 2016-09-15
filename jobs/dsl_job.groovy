job("my job"){
  scm {
    github 'naesheim/JenkinsJobs'
  }
  triggers {
    scm '@hourly'
  }
  steps {
    gradle 'clean test'
  }
}
