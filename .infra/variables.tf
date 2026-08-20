variable "aws_region" {
  description = "AWS region containing the existing EC2 instance and ECR repository."
  type        = string
  default     = "ap-northeast-2"
}

variable "existing_instance_id" {
  description = "Prepared EC2 instance targeted by SSM without managing its lifecycle."
  type        = string

  validation {
    condition     = can(regex("^i-[0-9a-f]{8,17}$", var.existing_instance_id))
    error_message = "existing_instance_id must be an EC2 instance ID."
  }
}

variable "ec2_instance_role_name" {
  description = "Existing IAM role attached to the prepared EC2 instance."
  type        = string
}

variable "github_oidc_provider_arn" {
  description = "Existing GitHub Actions OIDC provider ARN in this AWS account."
  type        = string

  validation {
    condition     = can(regex("^arn:aws:iam::[0-9]{12}:oidc-provider/token\\.actions\\.githubusercontent\\.com$", var.github_oidc_provider_arn))
    error_message = "github_oidc_provider_arn must identify the GitHub Actions OIDC provider."
  }
}

variable "github_repository" {
  description = "GitHub owner and repository allowed to deploy from main."
  type        = string
  default     = "YAPP-Github/28th-Web-Team-2-BE-2"
}

variable "ecr_repository_name" {
  description = "Existing ECR repository containing backend images."
  type        = string
  default     = "demo-backend"
}

variable "image_bucket_name" {
  description = "S3 bucket holding user report photos. Bucket names are globally unique."
  type        = string
  default     = "marketgo-images"

  validation {
    condition     = can(regex("^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$", var.image_bucket_name))
    # 점을 허용하지 않는다. 점이 든 이름은 virtual-hosted-style HTTPS 에서 와일드카드
    # 인증서와 매칭되지 않아 image_base_url 로 접근할 때 TLS 오류가 난다.
    error_message = "image_bucket_name must be a valid S3 bucket name without dots."
  }
}

