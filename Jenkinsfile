pipeline {
	agent {
		docker 'python:slim'
	}
	stages {
		stage('runScript') {	
			steps {
				sh 'python scripts/hello.py'
			}
		}
	}
}
