# Deploy to Kubernetes (GKE)

Manifests for the Employee Management app. Images live on Docker Hub:
`likith0129/employee-backend:1.0.0` and `likith0129/employee-frontend:1.0.1`.

## 0. Prerequisites (one time)

Install the gcloud CLI + kubectl, then point them at your project:

```bash
gcloud auth login
gcloud config set project <YOUR_PROJECT_ID>
gcloud services enable container.googleapis.com
gcloud components install kubectl        # if kubectl isn't already installed
```

## 1. Create a GKE cluster

Standard cluster with 3 nodes (so `kubectl get nodes` shows real nodes):

```bash
gcloud container clusters create employee-cluster \
  --zone us-central1-a \
  --num-nodes 3 \
  --machine-type e2-medium

# Wire kubectl up to the new cluster:
gcloud container clusters get-credentials employee-cluster --zone us-central1-a
```

## 2. Deploy (order matters: namespace + config first)

```bash
kubectl apply -f k8s/namespace/namespace.yaml
kubectl apply -f k8s/config/          # ConfigMap + Secret
kubectl apply -f k8s/mysql/           # DB (StatefulSet + PVC + seed)
kubectl apply -f k8s/redis/           # cache
kubectl apply -f k8s/backend/         # API + Service + HPA
kubectl apply -f k8s/frontend/        # UI + LoadBalancer
# kubectl apply -f k8s/ingress/       # OPTIONAL — only if using Ingress not LB
```

## 3. Look around — nodes & pods

```bash
kubectl get nodes -o wide                       # the VMs backing the cluster
kubectl get pods -n employee-app -o wide        # all app pods + which node they're on
kubectl get pods -n employee-app -w             # watch them go Pending -> Running
kubectl get all -n employee-app                 # deployments, services, statefulset, hpa
kubectl get pvc -n employee-app                 # the MySQL persistent volume claim
kubectl get svc frontend -n employee-app        # EXTERNAL-IP of the app (wait for it)
kubectl get hpa -n employee-app                 # autoscaler status
```

Describe / debug a specific pod:

```bash
kubectl describe pod <pod-name> -n employee-app
kubectl logs <pod-name> -n employee-app
kubectl top nodes                               # needs metrics-server (GKE has it)
kubectl top pods -n employee-app
```

## 4. Open the app

```bash
kubectl get svc frontend -n employee-app
# once EXTERNAL-IP is populated:  http://<EXTERNAL-IP>
```

## 5. Tear down (avoid GCP charges)

```bash
kubectl delete namespace employee-app
gcloud container clusters delete employee-cluster --zone us-central1-a
```
