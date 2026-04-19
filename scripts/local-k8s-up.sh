#!/bin/bash
set -e

echo "🎡 Initializing Foculist Local Kubernetes Environment..."

# 1. Detect Cluster
if command -v minikube >/dev/null 2>&1 && minikube status >/dev/null 2>&1; then
    CLUSTER_TYPE="minikube"
    echo "✅ Detected Minikube cluster."
elif command -v kind >/dev/null 2>&1 && kind get clusters | grep -q "^kind$"; then
    CLUSTER_TYPE="kind"
    echo "✅ Detected Kind cluster."
else
    echo "❌ No local Kubernetes cluster (minikube/kind) detected or running."
    echo "Please start one first: 'minikube start' or 'kind create cluster'"
    exit 1
fi

# 2. Install Ingress Controller (if missing)
if ! kubectl get deployment -n ingress-nginx ingress-nginx-controller >/dev/null 2>&1; then
    echo "🌐 Installing Nginx Ingress Controller..."
    if [ "$CLUSTER_TYPE" == "minikube" ]; then
        minikube addons enable ingress
    else
        kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
    fi
    echo "⏳ Waiting for Ingress Controller to be ready..."
    kubectl wait --namespace ingress-nginx \
      --for=condition=ready pod \
      --selector=app.kubernetes.io/component=controller \
      --timeout=180s
fi

# 3. Apply Manifests
echo "📦 Applying Foculist Manifests (Overlays: Local)..."
kubectl apply -k platform/k8s/overlays/local

# 4. Final Verification
echo "🚀 Deployment targeted. Check status with 'kubectl get pods'."
echo "💡 To access the platform:"
echo "   - API Gateway: http://foculist.local (requires /etc/hosts entry)"
echo "   - Grafana: kubectl port-forward svc/grafana 3000:3000"

echo "✅ Local Kubernetes Maturation Complete."
