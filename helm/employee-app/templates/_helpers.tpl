{{/*
Common labels applied to every object. Note: we intentionally keep the Service
names fixed (mysql, redis, backend, frontend) rather than release-prefixed,
because the nginx image proxies to "backend:8082" and the app connects to
"mysql"/"redis" by those exact names.
*/}}
{{- define "employee-app.labels" -}}
app.kubernetes.io/part-of: employee-app
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end -}}
