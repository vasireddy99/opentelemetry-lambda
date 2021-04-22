variable "collector_layer_name" {
  type        = string
  description = "Name of published collector layer"
  default     = "opentelemetry-collector"
}

variable "sdk_layer_name" {
  type        = string
  description = "Name of published SDK layer"
  default     = "opentelemetry-java-wrapper"
}

variable "function_name" {
  type        = string
  description = "Name of sample app function / API gateway"
  default     = "hello-java-awssdk-wrapper"
}

variable "tracing_mode" {
  type        = string
  description = "Lambda function tracing mode"
  default     = "PassThrough"
}