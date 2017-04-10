pipeline {
	agent {
		docker {
			label 'master'
			image 'python:slim'
			args '-v "$PWD/scripts:/scripts"'
		}
	}
	stages {
		stage('runScript') {
			steps {
				sh 'python /scrips/hello.py'
			}
		}
	}
}
