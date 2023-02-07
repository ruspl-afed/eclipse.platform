pipeline {
	options {
		timeout(time: 40, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'5'))
		disableConcurrentBuilds(abortPrevious: true)
	}
	agent {
		label "centos-latest"
	}
	
	environment {
		MAVEN_OPTS = "-Dbuild.sysclasspath=ignore -Dincludeantruntime=false"
	}
	tools {
		maven 'apache-maven-latest'
		jdk 'openjdk-jdk17-latest'
	}
	stages {
		stage('Build') {
			steps {
				wrap([$class: 'Xvnc', useXauthority: true]) {
					sh """
					mvn clean verify --batch-mode --fail-at-end -Dmaven.repo.local=$WORKSPACE/.m2/repository \
						-Pbuild-individual-bundles -Pbree-libs -Papi-check \
						-Dcompare-version-with-baselines.skip=false \
						-Dmaven.compiler.failOnWarning=true -Dproject.build.sourceEncoding=UTF-8 -T1C \
						-Dorg.slf4j.simpleLogger.showDateTime=true -Dorg.slf4j.simpleLogger.dateTimeFormat=HH:mm:ss.SSS \
						-DtrimStackTrace=false
						-Dbuild.sysclasspath=ignore -Dincludeantruntime=false
						
					"""
				}
			}
			post {
				always {
					archiveArtifacts artifacts: '.*log,*/target/work/data/.metadata/.*log,*/tests/target/work/data/.metadata/.*log,apiAnalyzer-workspace/.metadata/.*log', allowEmptyArchive: true
					junit '**/target/surefire-reports/TEST-*.xml'
					discoverGitReferenceBuild referenceJob: 'eclipse.platform/master'
					recordIssues publishAllIssues: true, tools: [java(), mavenConsole(), javaDoc()]
				}
			}
	}
}
