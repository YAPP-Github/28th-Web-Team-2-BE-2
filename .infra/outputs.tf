output "github_deploy_role_arn" {
  description = "Set this value as the AWS_DEPLOY_ROLE_ARN repository variable."
  value       = aws_iam_role.github_deploy.arn
}

output "ssm_document_name" {
  description = "SSM document invoked by the CD workflow."
  value       = aws_ssm_document.backend_deploy.name
}

output "image_bucket_name" {
  description = "백엔드 호스트 /etc/marketgo/backend.env 에 AWS_S3_BUCKET= 으로 추가한다 (ops/deploy/compose.yaml:7 env_file)."
  value       = aws_s3_bucket.images.bucket
}

output "image_base_url" {
  description = "/etc/marketgo/backend.env 에 AWS_S3_BASE_URL= 으로 추가한다. key 를 그대로 이어 붙인다. 주의: 버킷이 비공개이므로 이 URL 자체로는 조회되지 않는다 — 조회 경로는 별도 결정이 필요하다."
  value       = "https://${aws_s3_bucket.images.bucket_regional_domain_name}/"
}
