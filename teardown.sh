#!/usr/bin/env bash
# Tear down the Employee Management capstone and stop all GCP billing.
set -uo pipefail

PROJECT="capstone-project-505907"
CLUSTER="autopilot-cluster-1"
REGION="us-central1"

echo "This will PERMANENTLY DELETE:"
echo "  • GKE cluster '$CLUSTER' ($REGION) and ALL workloads"
echo "  • the app, ArgoCD, sealed-secrets, LoadBalancers and their disks"
echo "  • project: $PROJECT"
read -r -p "Type 'yes' to proceed: " ans
[ "$ans" = "yes" ] || { echo "Aborted."; exit 1; }

# 1) Delete LoadBalancer Services FIRST so GKE releases the GCP load balancers
#    (forwarding rules / external IPs) instead of orphaning them.
echo "[1/4] Releasing LoadBalancers..."
kubectl delete svc frontend -n employee-app --ignore-not-found
kubectl patch svc argocd-server -n argocd --type merge -p '{"spec":{"type":"ClusterIP"}}' 2>/dev/null || true
echo "      waiting for the cloud LB controller to clean up..."
sleep 30

# 2) Delete the app + controllers (removes PVCs -> their backing PD disks).
echo "[2/4] Deleting app, ArgoCD, sealed-secrets..."
kubectl delete -k k8s/ --ignore-not-found 2>/dev/null || true
kubectl delete namespace employee-app argocd sealed-secrets --ignore-not-found 2>/dev/null || true
sleep 10

# 3) Delete the cluster (the main cost). Autopilot: this removes the nodes too.
echo "[3/4] Deleting the GKE cluster (takes a few minutes)..."
gcloud container clusters delete "$CLUSTER" --region "$REGION" --project "$PROJECT" --quiet

# 4) Surface any leftover billable resources.
echo "[4/4] Checking for orphaned billable resources..."
echo "--- Forwarding rules (LB):"; gcloud compute forwarding-rules list --project "$PROJECT"
echo "--- Reserved static IPs:";   gcloud compute addresses list        --project "$PROJECT"
echo "--- Persistent disks:";      gcloud compute disks list            --project "$PROJECT"
echo "--- Target pools:";          gcloud compute target-pools list     --project "$PROJECT"

cat <<EOF

Cluster deleted. If anything is listed above (GKE-created leftovers), remove it:
  gcloud compute disks delete <NAME> --zone <ZONE> --project $PROJECT --quiet
  gcloud compute forwarding-rules delete <NAME> --region $REGION --project $PROJECT --quiet
  gcloud compute addresses delete <NAME> --region $REGION --project $PROJECT --quiet

Unaffected (no charge / keep these): your GitHub repo and Docker Hub images.
EOF
