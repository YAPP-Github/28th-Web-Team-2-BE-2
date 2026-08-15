locals {
  common_tags = {
    Environment = "production"
    ManagedBy   = "Terraform"
    Project     = "marketgo"
  }
}

data "aws_caller_identity" "current" {}

data "aws_ecr_repository" "backend" {
  name = var.ecr_repository_name
}

data "aws_iam_policy_document" "github_deploy_assume_role" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [var.github_oidc_provider_arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${var.github_repository}:ref:refs/heads/main"]
    }
  }
}

resource "aws_iam_role" "github_deploy" {
  name               = "marketgo-github-deploy"
  assume_role_policy = data.aws_iam_policy_document.github_deploy_assume_role.json

  tags = local.common_tags
}

resource "aws_ssm_document" "backend_deploy" {
  name            = "marketgo-deploy-backend"
  document_type   = "Command"
  document_format = "JSON"

  content = jsonencode({
    schemaVersion = "2.2"
    description   = "Deploy one validated MarketGo backend image"
    parameters = {
      Image = {
        type              = "String"
        interpolationType = "ENV_VAR"
        allowedPattern    = "^${data.aws_caller_identity.current.account_id}\\.dkr\\.ecr\\.${var.aws_region}\\.amazonaws\\.com/${var.ecr_repository_name}:[0-9a-f]{40}$"
      }
    }
    mainSteps = [
      {
        action = "aws:runShellScript"
        name   = "deployBackend"
        inputs = {
          timeoutSeconds = "2100"
          runCommand = [
            "test -x /usr/local/sbin/marketgo-deploy",
            "if [ -z \"$${SSM_Image+x}\" ]; then export SSM_Image=\"{{Image}}\"; fi",
            "/usr/local/sbin/marketgo-deploy \"$SSM_Image\"",
          ]
        }
      },
    ]
  })

  tags = local.common_tags
}

data "aws_iam_policy_document" "github_deploy" {
  statement {
    sid       = "AuthorizeEcr"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid = "PublishBackendImage"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:CompleteLayerUpload",
      "ecr:DescribeImages",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart",
    ]
    resources = [data.aws_ecr_repository.backend.arn]
  }

  statement {
    sid     = "RunDeployOnBackend"
    actions = ["ssm:SendCommand"]
    resources = [
      "arn:aws:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:instance/${var.existing_instance_id}",
      aws_ssm_document.backend_deploy.arn,
    ]
  }

  statement {
    sid       = "ReadDeployResult"
    actions   = ["ssm:CancelCommand", "ssm:GetCommandInvocation"]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "github_deploy" {
  name   = "marketgo-ssm-deploy"
  role   = aws_iam_role.github_deploy.id
  policy = data.aws_iam_policy_document.github_deploy.json
}

data "aws_iam_policy_document" "ec2_ecr_pull" {
  statement {
    sid       = "AuthorizeEcr"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid = "PullBackendImage"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:GetDownloadUrlForLayer",
    ]
    resources = [data.aws_ecr_repository.backend.arn]
  }
}

resource "aws_iam_role_policy" "ec2_ecr_pull" {
  name   = "marketgo-ecr-pull"
  role   = var.ec2_instance_role_name
  policy = data.aws_iam_policy_document.ec2_ecr_pull.json
}

resource "aws_iam_role_policy_attachment" "ec2_ssm_core" {
  role       = var.ec2_instance_role_name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}
