# Kubernetes Manifests

This scaffold uses Kustomize overlays for environment separation.

## Prepare secret once per environment
```bash
cp platform/k8s/base/runtime-secret.example.yaml platform/k8s/base/runtime-secret.yaml
# edit DB_USERNAME / DB_PASSWORD before apply
kubectl apply -f platform/k8s/base/runtime-secret.yaml
```

## Apply overlays
```bash
kubectl apply -k platform/k8s/overlays/local
kubectl apply -k platform/k8s/overlays/dev
kubectl apply -k platform/k8s/overlays/prod
```

## Overlay intent
- `overlays/local`: local cluster defaults, ingress host `api-local.foculist.internal`, replicas scaled down to 1.
- `overlays/dev`: dev runtime endpoints, ingress host `api-dev.foculist.internal`.
- `overlays/prod`: prod runtime endpoints, ingress host `api.foculist.com`, TLS block enabled, higher replicas.

Notes:
- `gateway-bff` is the only internet-facing service.
- Domain services remain `ClusterIP` behind the gateway.
- Runtime variables map to env placeholders in each service `application.yml`.
- Do not commit real secrets; only `runtime-secret.example.yaml` is versioned.
