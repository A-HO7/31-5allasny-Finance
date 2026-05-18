& minikube -p minikube docker-env --shell powershell | Invoke-Expression
Write-Host "Building images inside Minikube..."
docker build -t api-gateway:1.0-SNAPSHOT ./api-gateway
docker build -t account-service:latest ./account-service
docker build -t budget-service:latest ./budget-service
docker build -t reporting-service:latest ./reporting-service
docker build -t transaction-service:latest ./transaction-service
docker build -t user-service:latest ./user-service
Write-Host "Build complete! Deleting pods to force restart..."
kubectl delete pods -l app=api-gateway -n financetracker
kubectl delete pods -l app=account-service -n financetracker
kubectl delete pods -l app=budget-service -n financetracker
kubectl delete pods -l app=reporting-service -n financetracker
kubectl delete pods -l app=transaction-service -n financetracker
kubectl delete pods -l app=user-service -n financetracker
