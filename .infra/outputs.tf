output "github_deploy_role_arn" {
  description = "Set this value as the AWS_DEPLOY_ROLE_ARN repository variable."
  value       = aws_iam_role.github_deploy.arn
}

output "ssm_document_name" {
  description = "SSM document invoked by the CD workflow."
  value       = aws_ssm_document.backend_deploy.name
}
