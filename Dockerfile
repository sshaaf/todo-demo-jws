FROM registry.redhat.io/jboss-webserver-6/jws60-openjdk17-openshift-rhel8:6.0.2-2

COPY target/*.war /deployments

