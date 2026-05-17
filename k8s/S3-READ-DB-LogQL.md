# S3-READ-DB LogQL Panels

As part of the S3-READ-DB slice, here are 3+ LogQL panels for the `transaction-service` Grafana dashboard. These will be integrated into the final dashboard JSON by the S3-INFRA slice owner.

### 1. Transaction Service Error Rate
**Description:** Tracks the rate of ERROR level logs to identify sudden spikes in failures.
**LogQL Query:**
```logql
sum by (app) (rate({app="transaction-service", env="k8s", level="ERROR"}[5m]))
```

### 2. Transaction Completion Events Over Time
**Description:** Monitors the volume of `transaction.completed` events being published to RabbitMQ.
**LogQL Query:**
```logql
sum by (app) (rate({app="transaction-service"} |= "Published transaction.completed for txn=" [5m]))
```

### 3. Missing Feign Client / Fallback Activations
**Description:** Tracks how often the `transaction-service` is forced to fall back to local checks because a Feign client (like `user-service` or `account-service`) is unavailable.
**LogQL Query:**
```logql
sum by (app) (rate({app="transaction-service"} |= "unavailable" |= "using defaults" [5m]))
```

### 4. Saga State Machine Feedback (Report Initiated/Completed/Failed)
**Description:** Analyzes the volume of saga feedback events processed by the S3 consumer.
**LogQL Query:**
```logql
sum by (routingKey) (
  rate({app="transaction-service"} |= "saga-feedback received routingKey=" | regexp "routingKey=(?P<routingKey>[^\\s]+)" [5m])
)
```
