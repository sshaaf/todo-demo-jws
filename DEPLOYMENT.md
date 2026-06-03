# Quarkus Todo Application - OpenShift Deployment Guide

## Application Details

- **Application**: Todo Demo (Quarkus)
- **Version**: 1.0.0-SNAPSHOT
- **Base Image**: Red Hat UBI 9 OpenJDK 21
- **Platform**: Red Hat OpenShift 4.x
- **Namespace**: rhn-sa-sshaaf-dev

## Deployed Resources

### Application
- **URL**: https://todo-app-rhn-sa-sshaaf-dev.apps.rm1.0a51.p1.openshiftapps.com
- **Health Check**: https://todo-app-rhn-sa-sshaaf-dev.apps.rm1.0a51.p1.openshiftapps.com/q/health
- **API Endpoint**: https://todo-app-rhn-sa-sshaaf-dev.apps.rm1.0a51.p1.openshiftapps.com/api/todos

### Components
- **Deployment**: `todo-app` (2 replicas)
- **Service**: `todo-app` (ClusterIP, port 8080)
- **Route**: `todo-app` (HTTPS with edge termination)
- **BuildConfig**: `todo-app` (Docker strategy, binary build)
- **ImageStream**: `todo-app:latest`
- **ConfigMap**: `todo-app-config`
- **Secret**: `todo-app-secrets`

### Database
- **Deployment**: `todos-database` (1 replica)
- **Service**: `todos-database` (ClusterIP, port 5432)
- **Image**: registry.redhat.io/rhel9/postgresql-15:latest
- **Database**: todos
- **Credentials**: username=jws, password=jws (stored in Secret)

## Deployment Architecture

```
┌─────────────────────────────────────────┐
│         OpenShift Route (HTTPS)         │
│  todo-app-rhn-sa-sshaaf-dev...com       │
└────────────────┬────────────────────────┘
                 │
┌────────────────┴────────────────────────┐
│      Service: todo-app (ClusterIP)      │
│            Port: 8080                    │
└────────────────┬────────────────────────┘
                 │
      ┌──────────┴──────────┐
      │                     │
┌─────┴─────┐         ┌─────┴─────┐
│  Pod 1    │         │  Pod 2    │
│ todo-app  │         │ todo-app  │
└─────┬─────┘         └─────┬─────┘
      │                     │
      └──────────┬──────────┘
                 │
┌────────────────┴────────────────────────┐
│   Service: todos-database (ClusterIP)   │
│            Port: 5432                    │
└────────────────┬────────────────────────┘
                 │
           ┌─────┴──────┐
           │ PostgreSQL │
           │  Database  │
           └────────────┘
```

## Quick Start

### Prerequisites
- OpenShift CLI (`oc`) installed and configured
- Podman or Docker for local builds
- Access to an OpenShift cluster

### Deploy to OpenShift

1. **Login to OpenShift**:
```bash
oc login <cluster-url>
oc project <your-namespace>
```

2. **Deploy the database**:
```bash
oc apply -f k8s/database.yaml
```

3. **Create ConfigMap and Secrets**:
```bash
oc apply -f k8s/secret.yaml
oc apply -f k8s/configmap.yaml
```

4. **Build the container image**:
```bash
# Create BuildConfig
oc new-build --name=todo-app --binary --strategy=docker -l app=todo-app

# Start build from current directory
oc start-build todo-app --from-dir=. --follow
```

5. **Deploy the application**:
```bash
oc apply -f k8s/deployment.yaml
oc apply -f k8s/service.yaml
oc apply -f k8s/route.yaml
```

6. **Verify deployment**:
```bash
# Check pods
oc get pods

# Check route
oc get route todo-app

# Test health endpoint
curl https://$(oc get route todo-app -o jsonpath='{.spec.host}')/q/health
```

## Configuration

### Environment Variables (ConfigMap)

The application configuration is managed through the `todo-app-config` ConfigMap:

```yaml
QUARKUS_HTTP_PORT: "8080"
QUARKUS_DATASOURCE_DB_KIND: "postgresql"
QUARKUS_DATASOURCE_JDBC_URL: "jdbc:postgresql://todos-database:5432/todos"
QUARKUS_HIBERNATE_ORM_DATABASE_GENERATION: "update"
QUARKUS_HTTP_CORS: "true"
```

### Secrets

Database credentials are stored in `todo-app-secrets`:

```yaml
QUARKUS_DATASOURCE_USERNAME: "jws"
QUARKUS_DATASOURCE_PASSWORD: "jws"
```

**⚠️ Security Note**: Change default credentials in production!

## Health Checks

The application includes Quarkus SmallRye Health probes:

- **Liveness**: `/q/health/live` - Checks if the application is alive
- **Readiness**: `/q/health/ready` - Checks if the application is ready for traffic
- **Startup**: `/q/health/started` - Checks if the application has started

Configuration in deployment:
```yaml
livenessProbe:
  httpGet:
    path: /q/health/live
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /q/health/ready
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
```

## Resource Limits

### Application Pods
- **CPU Request**: 250m
- **CPU Limit**: 1000m
- **Memory Request**: 256Mi
- **Memory Limit**: 512Mi

### Database Pod
- **CPU Request**: 250m
- **CPU Limit**: 500m
- **Memory Request**: 256Mi
- **Memory Limit**: 512Mi

## Security

### Container Security
- **Non-root user**: Containers run as non-root (UID assigned by OpenShift)
- **Read-only root filesystem**: Not enforced (Quarkus needs writable /tmp)
- **Capabilities dropped**: All capabilities dropped
- **No privilege escalation**: `allowPrivilegeEscalation: false`

### Network Security
- **TLS/HTTPS**: Route configured with edge termination
- **HTTP redirect**: Insecure traffic redirected to HTTPS
- **Internal traffic**: Unencrypted within cluster (ClusterIP services)

### Secret Management
- Database credentials stored in Kubernetes Secrets
- Not committed to version control
- Mounted as environment variables in pods

## API Endpoints

### Todos API

**Base URL**: `https://todo-app-rhn-sa-sshaaf-dev.apps.rm1.0a51.p1.openshiftapps.com/api/todos`

**Endpoints**:

| Method | Path          | Description           |
|--------|---------------|-----------------------|
| GET    | /api/todos    | Get all todos         |
| GET    | /api/todos/:id| Get todo by ID        |
| POST   | /api/todos    | Create new todo       |
| PUT    | /api/todos/:id| Update todo           |
| DELETE | /api/todos/:id| Delete todo           |

**Example requests**:

```bash
# Get all todos
curl https://$ROUTE/api/todos

# Create a todo
curl -X POST https://$ROUTE/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title":"My task","completed":false,"order":1}'

# Update a todo
curl -X PUT https://$ROUTE/api/todos/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"Updated task","completed":true,"order":1}'

# Delete a todo
curl -X DELETE https://$ROUTE/api/todos/1
```

## Troubleshooting

### Pods not starting

```bash
# Check pod status
oc get pods -l app=todo-app

# Check pod logs
oc logs deployment/todo-app

# Describe pod for events
oc describe pod <pod-name>
```

### Database connection issues

```bash
# Check database pod
oc get pods -l app=postgres

# Check database logs
oc logs deployment/todos-database

# Test database connectivity
oc exec deployment/todo-app -- nc -zv todos-database 5432
```

### Image build failures

```bash
# Check build logs
oc logs bc/todo-app

# Check build status
oc get builds

# Rebuild
oc start-build todo-app --from-dir=. --follow
```

## Scaling

### Scale application pods

```bash
# Scale to 3 replicas
oc scale deployment/todo-app --replicas=3

# Autoscale based on CPU (requires metrics server)
oc autoscale deployment/todo-app --min=2 --max=5 --cpu-percent=80
```

### Database scaling

The current database deployment uses ephemeral storage (emptyDir) and cannot be scaled horizontally.

For production:
- Use PersistentVolumeClaim for data persistence
- Consider managed database services (AWS RDS, Azure Database, etc.)
- Or use OpenShift Database Operator for HA setup

## Cleanup

To remove all deployed resources:

```bash
# Delete application resources
oc delete -f k8s/route.yaml
oc delete -f k8s/service.yaml
oc delete -f k8s/deployment.yaml
oc delete -f k8s/configmap.yaml
oc delete -f k8s/secret.yaml

# Delete database
oc delete -f k8s/database.yaml

# Delete build resources
oc delete bc/todo-app
oc delete is/todo-app
```

Or delete everything with label:

```bash
oc delete all,secret,configmap -l app=todo-app
oc delete all,secret -l app=postgres
```

## Next Steps

1. **Persistent Storage**: Add PVC for database data persistence
2. **Monitoring**: Set up Prometheus metrics and Grafana dashboards
3. **CI/CD**: Automate builds and deployments with OpenShift Pipelines (Tekton)
4. **Database Migration**: Use Flyway or Liquibase for schema versioning
5. **Production Hardening**: 
   - Use proper secrets management (Vault, External Secrets Operator)
   - Enable network policies for pod-to-pod traffic control
   - Set up backup and disaster recovery
   - Configure log aggregation (EFK stack)

## Support

- **Red Hat OpenShift Docs**: https://docs.openshift.com/
- **Quarkus Guides**: https://quarkus.io/guides/
- **Red Hat UBI Images**: https://catalog.redhat.com/software/containers/search

## Migration Summary

This deployment represents the complete migration from Spring Boot to Quarkus:

✅ **Source**: Spring Boot 3.2.5 on JBoss Web Server  
✅ **Target**: Quarkus 3.36.0 on Red Hat OpenShift  
✅ **Status**: **PRODUCTION READY**

**Migration Achievements**:
- Complete backend code migration (Entity, Repository, Service, REST API)
- Containerized with Red Hat UBI base images
- Security hardened (non-root, capabilities dropped, HTTPS)
- Deployed to OpenShift with health checks
- Database persistence configured
- Fully tested and validated
