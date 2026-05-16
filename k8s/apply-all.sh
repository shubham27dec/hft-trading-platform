#!/usr/bin/env bash
set -euo pipefail

echo "Applying all Kubernetes manifests..."
kubectl apply -f k8s/

echo ""
echo "All manifests applied."
echo ""
echo "Minikube IP: $(minikube ip)"
echo ""
echo "Service endpoints (NodePorts):"
echo "  Keycloak:              http://$(minikube ip):30180"
echo "  Order Entry Service:   http://$(minikube ip):30085"
echo "  Position Service:      http://$(minikube ip):30081"
echo "  Risk Dashboard:        http://$(minikube ip):30082"
echo "  Notification Service:  http://$(minikube ip):30083"
echo "  Audit Service:         http://$(minikube ip):30084"
echo "  Prometheus:            http://$(minikube ip):30090"
echo "  Grafana:               http://$(minikube ip):30300"
