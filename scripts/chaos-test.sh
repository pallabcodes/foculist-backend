#!/bin/bash
set -e

echo "👻 Starting Chaos Simulation with Pumba..."

# Ensure Pumba is not already running
docker stop pumba-chaos >/dev/null 2>&1 || true
docker rm pumba-chaos >/dev/null 2>&1 || true

# Target service: planning-service
# Action: Inject 3s latency for 30s
echo "🕸️ Injecting 3000ms latency into 'planning-service' for 30 seconds..."

docker run -d --name pumba-chaos \
    -v /var/run/docker.sock:/var/run/docker.sock \
    gaiaadm/pumba netem --duration 30s --tcimage gaiadocker/iproute2 \
    delay --time 3000 planning-service

echo "🚀 Chaos initiated. Monitor the logs or try accessing Planning APIs via Gateway."
echo "💡 To stop manually: 'make chaos-stop'"

# Wait for Pumba to finish or be stopped
docker wait pumba-chaos >/dev/null 2>&1 || true
echo "🏁 Chaos simulation complete."
