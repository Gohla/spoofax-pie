#!groovy
@Library('metaborg.jenkins.pipeline@develop') _

gradlePipeline(
  publish: false,
  upstreamProjects: [
    '/metaborg/pie/develop',
    '/metaborg/spoofax-releng/master'
  ],
  slack: true,
  slackChannel: "#pie-dev"
)
