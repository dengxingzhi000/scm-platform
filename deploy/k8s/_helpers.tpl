{{- define "scm-service.deployment" -}}
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ .name }}
  namespace: {{ .namespace | default "scm-platform" }}
  labels:
    app: {{ .name }}
    tier: {{ .tier | default "business" }}
spec:
  replicas: {{ .replicas | default 1 }}
  selector:
    matchLabels:
      app: {{ .name }}
  template:
    metadata:
      labels:
        app: {{ .name }}
    spec:
      containers:
        - name: {{ .name }}
          image: {{ .image | default (printf "scm-platform/%s:latest" .name) }}
          ports:
            - containerPort: {{ .port }}
              name: http
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "kubernetes"
            - name: NACOS_SERVER_ADDR
              valueFrom:
                configMapKeyRef:
                  name: scm-config
                  key: nacos-server
            - name: NACOS_NAMESPACE
              valueFrom:
                configMapKeyRef:
                  name: scm-config
                  key: nacos-namespace
            - name: DB_HOST
              valueFrom:
                configMapKeyRef:
                  name: scm-config
                  key: db-host
            - name: DB_USERNAME
              valueFrom:
                secretKeyRef:
                  name: scm-secrets
                  key: db-username
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: scm-secrets
                  key: db-password
            - name: API_SECRET_INTERNAL_SERVICE
              valueFrom:
                secretKeyRef:
                  name: scm-secrets
                  key: api-secret-internal-service
            - name: REDIS_HOST
              valueFrom:
                configMapKeyRef:
                  name: scm-config
                  key: redis-host
            - name: REDIS_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: scm-secrets
                  key: redis-password
            - name: JAVA_OPTS
              value: {{ .javaOpts | default "-Xms256m -Xmx512m -XX:+UseG1GC" }}
          resources:
            requests:
              cpu: {{ .cpuRequest | default "200m" }}
              memory: {{ .memoryRequest | default "256Mi" }}
            limits:
              cpu: {{ .cpuLimit | default "500m" }}
              memory: {{ .memoryLimit | default "512Mi" }}
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: {{ .port }}
            initialDelaySeconds: 60
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: {{ .port }}
            initialDelaySeconds: 30
            periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: {{ .name }}
  namespace: {{ .namespace | default "scm-platform" }}
spec:
  selector:
    app: {{ .name }}
  ports:
    - port: {{ .port }}
      targetPort: {{ .port }}
      name: http
  type: ClusterIP
{{- end -}}
