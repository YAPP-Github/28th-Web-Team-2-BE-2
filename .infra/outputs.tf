output "github_deploy_role_arn" {
  description = "Set this value as the AWS_DEPLOY_ROLE_ARN repository variable."
  value       = aws_iam_role.github_deploy.arn
}

output "ssm_document_name" {
  description = "SSM document invoked by the CD workflow."
  value       = aws_ssm_document.backend_deploy.name
}

output "image_bucket_name" {
  description = "Set this value as the AWS_S3_BUCKET environment variable."
  value       = aws_s3_bucket.images.bucket
}

output "image_base_url" {
  description = "Set this value as the AWS_S3_BASE_URL environment variable. Keys are appended directly."
  value       = "https://${aws_s3_bucket.images.bucket_regional_domain_name}/"
}
