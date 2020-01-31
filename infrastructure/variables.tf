variable "product" {
  type    = "string"
}

variable "component" {
  type = "string"
}

variable "location_app" {
  type    = "string"
  default = "UK South"
}

variable "env" {
  type = "string"
}

variable "ilbIp" {}

variable "subscription" {}

variable "capacity" {
  default = "1"
}

variable "common_tags" {
  type = "map"
}

variable "wiremock_server_mappings_path" {
  description = "WireMock server mappings files path containing json response stubs"
  type = "string"
  default = "wiremock"
}

variable "enable_ase" {
  default = true
}
