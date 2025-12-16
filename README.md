### Zoo Eurail — Spring Boot + DynamoDB Local

This project implements a simple backend-only web application for managing a Zoo with Rooms and Animals.

Key features:
- CRUD for Animal and Room (single-entity endpoints, JSON in/out)
- Place/move/remove Animal in/from Room
- Assign/unassign favorite Rooms for Animals (many favorites allowed)
- Extra endpoints:
  - Get all Animals in a specific Room (sorting by `title` or `located`, order `asc|desc`, pagination)
  - List of favorite Rooms (title) and number of Animals that marked them as favorite (only rooms with count > 0)

Tech stack:
- Java 21, Spring Boot 3.3.5
- DynamoDB Local (AWS SDK v2 Enhanced Client)
- Redis (optional; Caffeine in dev, Redis in production)
- OpenAPI/Swagger UI
- Docker & docker-compose
- Kubernetes manifests (bonus)

Caching:
- Backed by Caffeine (in-memory) via Spring Cache. See `src/main/java/com/eurail/zooeurail/config/CacheConfig.java` and defaults in `src/main/resources/application.yml` under `app.cache`.
- Caches and defaults (overridable via properties):
  - `animalsById` — TTL `10m` (config key: `app.cache.ttl.animals-by-id`).
  - `roomsById` — TTL `10m` (config key: `app.cache.ttl.rooms-by-id`).
  - `favoriteRoomsAggByTitle` — TTL `60s` (config key: `app.cache.ttl.favorites-agg`).
- Usage highlights:
  - `AnimalService#get(id)` is `@Cacheable` into `animalsById`.
  - Mutations affecting an animal (create/update/delete/place/move/remove) `@CacheEvict` its `animalsById` entry.
  - Favorite-room aggregation by title is `@Cacheable` in `favoriteRoomsAggByTitle` and is evicted on favorite assign/unassign operations.
- Notes:
  - Dev/test default: Caffeine. Production default: Redis (see `application-production.yml`).
  - For multiple instances, Redis is recommended. This repo includes both Caffeine and Redis CacheManager via `app.cache.type`.
  - You can tune sizes/TTLs in `CacheConfig` or via `application.yml` without code changes.

Profiles:
- `dev` (default/local)
- `production`

Run locally (Docker Compose):
1. Build the jar:
   ```bash
   ./mvnw clean package -DskipTests
   ```
2. Start services:
   ```bash
   docker compose up --build
   ```
3. Open API docs:
   - Swagger UI: `http://localhost:8080/swagger-ui.html`

Kubernetes (bonus):
1. Build/push the image to your registry (adjust image name in manifests).
2. Apply manifests:
   ```bash
   kubectl apply -f k8s/
   ```
3. Port-forward or expose the service as needed.

Redis on Kubernetes:
- A minimal Redis StatefulSet and Services are provided in `k8s/redis-statefulset.yaml`.
- Apply it before the app (or together via `kubectl apply -f k8s/`). The Service DNS will be `redis.default.svc.cluster.local` (or `redis` within the same namespace).
- The EKS app manifest (`k8s/zoo-app-eks.yaml`) sets `REDIS_HOST=redis` and `REDIS_PORT=6379`. The production profile uses Redis automatically.

Production on AWS (EKS + CloudFormation):
- The repo includes baseline CloudFormation templates and EKS-ready manifests to deploy the app with the `production` profile against AWS DynamoDB.

Stacks/templates:
- `cloudformation/ecr.yaml` — ECR repository for the app image (optional if you already have one).
- `cloudformation/dynamodb.yaml` — Creates `animals` and `rooms` tables with GSIs `gsi_title` and `gsi_roomId` (PAY_PER_REQUEST by default).
- `cloudformation/eks.yaml` — EKS cluster, managed node group, and OIDC provider (requires existing VPC + subnets).
- `cloudformation/iam-irsa-dynamodb.yaml` — IAM role and policy for IRSA (ServiceAccount) to access DynamoDB (see notes below).
- `cloudformation/elasticache-redis.yaml` — ElastiCache Redis (single-node) with Security Group ingress from your EKS worker node SG. Outputs the Redis endpoint and port.
- `k8s/zoo-app-eks.yaml` — K8s ServiceAccount (IRSA), Deployment (uses `SPRING_PROFILES_ACTIVE=production`), and Service `LoadBalancer`.

High-level steps (example):
1) Create ECR (optional):
   ```bash
   aws cloudformation deploy \
     --stack-name zoo-ecr \
     --template-file cloudformation/ecr.yaml \
     --capabilities CAPABILITY_NAMED_IAM
   ```
   Get the repo URI: `aws cloudformation describe-stacks --stack-name zoo-ecr --query "Stacks[0].Outputs" --output table`

2) Build and push the image:
   ```bash
   REGION=<region>
   ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
   REPO=zoo-eurail
   aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com"
   ./mvnw clean package -DskipTests
   docker build -t $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/$REPO:latest .
   docker push $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/$REPO:latest
   ```

3) Create DynamoDB tables:
   ```bash
   aws cloudformation deploy \
     --stack-name zoo-ddb \
     --template-file cloudformation/dynamodb.yaml \
     --parameter-overrides TablePrefix= ProdPrefix= \
     --capabilities CAPABILITY_NAMED_IAM
   ```

4) Create EKS cluster (requires existing VPC + subnets):
   ```bash
   aws cloudformation deploy \
     --stack-name zoo-eks \
     --template-file cloudformation/eks.yaml \
     --parameter-overrides \
       ClusterName=zoo-prod \
       VpcId=vpc-xxxxxxxx \
       PrivateSubnetIds=subnet-aaaa,subnet-bbbb \
       PublicSubnetIds=subnet-cccc,subnet-dddd \
       KubernetesVersion=1.29 \
       NodeGroupInstanceTypes=t3.medium \
       NodeGroupDesiredSize=2 NodeGroupMinSize=1 NodeGroupMaxSize=4 \
     --capabilities CAPABILITY_NAMED_IAM

   # Update kubeconfig
   aws eks update-kubeconfig --name zoo-prod --region $REGION
   ```

5) Create IRSA role for the app to access DynamoDB:
   - Find OIDC Provider ARN and issuer domain in the EKS console or via CLI.
   - IMPORTANT: CloudFormation cannot parameterize map keys portably; the template uses placeholders.
   - Replace the placeholders in `cloudformation/iam-irsa-dynamodb.yaml` with your literal keys if your region/account does not support LanguageExtensions.

   Example deploy (with parameters):
   ```bash
   # Discover values
   OIDC_PROVIDER_ARN=$(aws iam list-open-id-connect-providers --query 'OpenIDConnectProviderList[?contains(Arn, `oidc.eks`) == `true`].Arn' --output text)
   OIDC_ISSUER=$(aws eks describe-cluster --name zoo-prod --query 'cluster.identity.oidc.issuer' --output text)
   OIDC_DOMAIN=${OIDC_ISSUER#https://}
   OIDC_SUB_KEY="$OIDC_DOMAIN:sub"
   OIDC_AUD_KEY="$OIDC_DOMAIN:aud"
   ANIMALS_ARN=$(aws cloudformation describe-stack-resources --stack-name zoo-ddb --query 'StackResources[?LogicalResourceId==`AnimalsTable`].PhysicalResourceId' --output text)
   ROOMS_ARN=$(aws cloudformation describe-stack-resources --stack-name zoo-ddb --query 'StackResources[?LogicalResourceId==`RoomsTable`].PhysicalResourceId' --output text)

   aws cloudformation deploy \
     --stack-name zoo-irsa \
     --template-file cloudformation/iam-irsa-dynamodb.yaml \
     --parameter-overrides \
        OIDCProviderArn="$OIDC_PROVIDER_ARN" \
        OIDCSubKey="$OIDC_SUB_KEY" \
        OIDCAudKey="$OIDC_AUD_KEY" \
        ServiceAccountNamespace=default \
        ServiceAccountName=zoo-app \
        AnimalsTableArn="$ANIMALS_ARN" \
        RoomsTableArn="$ROOMS_ARN" \
     --capabilities CAPABILITY_NAMED_IAM
   ```

6) Deploy the app to EKS:
- Edit `k8s/zoo-app-eks.yaml`: set your `role-arn` on the ServiceAccount annotation and the image to your ECR repo.
- Apply:
  ```bash
  kubectl apply -f k8s/zoo-app-eks.yaml
  ```
- Get the service EXTERNAL-IP:
  ```bash
  kubectl get svc zoo-app
  ```

7) (Optional) Provision ElastiCache Redis and point the app to it:
```bash
aws cloudformation deploy \
  --stack-name zoo-redis \
  --template-file cloudformation/elasticache-redis.yaml \
  --parameter-overrides \
    VpcId=vpc-xxxxxxxx \
    SubnetIds=subnet-aaaa,subnet-bbbb \
    WorkerNodeSecurityGroupId=sg-eksworkers \
    NodeType=cache.t4g.small \
    EngineVersion=7.1
```
Fetch the endpoint:
```bash
aws cloudformation describe-stacks --stack-name zoo-redis \
  --query "Stacks[0].Outputs[?OutputKey=='RedisEndpointAddress'].OutputValue" --output text
```
Update `k8s/zoo-app-eks.yaml` to set `REDIS_HOST` to the ElastiCache endpoint DNS and apply again:
```bash
kubectl apply -f k8s/zoo-app-eks.yaml
```

Notes:
- The production profile (`src/main/resources/application-production.yml`) disables table auto-creation and relies on AWS SDK default endpoint. Do not set `APP_DYNAMODB_ENDPOINT` in EKS.
- If you need a domain/TLS, integrate AWS Load Balancer Controller and Ingress (not included in this baseline).
- Tagging/quotas/limits should be adjusted for your environment.

DynamoDB Local:
- Runs via docker-compose as `dynamodb-local` on port `8000`.
- The app uses endpoint `http://dynamodb-local:8000` in Compose and `http://dynamodb-local:8000` in Kubernetes via Service DNS.
Redis Local (K8s):
- The app connects to `redis:6379` inside the cluster by default (see `application-production.yml` and EKS manifest env).

Notes on data model:
- An `Animal` has a single current `roomId` (or `null`). A `Room` can contain many animals.
- Favorite rooms are kept as a set of `roomId`s on the `Animal`.
