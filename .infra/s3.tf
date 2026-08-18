# 제보 사진 저장용 버킷.
#
# 계약은 `.agents/skills/image-upload-flow/SKILL.md`에 있다. 요지는 셋이다.
#   - key는 `images/{UUID}.{png|jpg}`
#   - 영구 URL은 `AWS_S3_BASE_URL + key`
#   - 클라이언트 직접 업로드는 10분 만료 presigned PUT
#
# 버킷은 공개하지 않는다. 읽기도 presigned GET으로 처리하므로 public access는 전부 차단한다.
# (SKILL.md의 "영구 저장 URL"은 경로 규칙일 뿐 공개 접근을 뜻하지 않는다.)

resource "aws_s3_bucket" "images" {
  bucket = var.image_bucket_name

  tags = local.common_tags
}

resource "aws_s3_bucket_public_access_block" "images" {
  bucket = aws_s3_bucket.images.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_ownership_controls" "images" {
  bucket = aws_s3_bucket.images.id

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "images" {
  bucket = aws_s3_bucket.images.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }

    bucket_key_enabled = true
  }
}

# 중단된 multipart 업로드만 청소한다.
#
# 제보에 붙은 사진은 사용자에게 계속 보여야 하므로 객체 만료는 걸지 않는다. 업로드만 되고
# 제보에 연결되지 않은 orphan 객체를 지우려면 DB와 대조하는 조정 작업이 필요한데,
# SKILL.md가 "Media DB 상태 머신이나 업로드 확인 API를 새로 만들지 않는다"고 못 박아 두었다.
# 그 결정이 바뀌면 여기에 expiration 규칙을 추가한다.
resource "aws_s3_bucket_lifecycle_configuration" "images" {
  bucket = aws_s3_bucket.images.id

  rule {
    id     = "abort-incomplete-multipart-upload"
    status = "Enabled"

    filter {}

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }
}

# 브라우저가 presigned URL로 직접 PUT하는 경로에만 필요하다.
# 허용 origin은 backend `CorsConfig.ALLOWED_ORIGIN_PATTERNS`와 같은 목록을 유지한다.
resource "aws_s3_bucket_cors_configuration" "images" {
  bucket = aws_s3_bucket.images.id

  cors_rule {
    allowed_methods = ["PUT"]
    allowed_origins = var.image_upload_allowed_origins
    allowed_headers = ["Content-Type", "Content-Length"]
    expose_headers  = ["ETag"]
    max_age_seconds = 3000
  }
}

# EC2 인스턴스 역할에 붙이는 권한.
#
# presigned URL 발급 자체는 API 호출이 아니라 역할 자격증명으로 로컬 서명하는 동작이므로
# 별도 권한이 필요 없다. 다만 서명된 요청이 실제로 통과하려면 역할에 그 action이 있어야 한다.
data "aws_iam_policy_document" "ec2_image_storage" {
  statement {
    sid = "ReadWriteReportImages"
    actions = [
      "s3:PutObject",
      "s3:GetObject",
    ]
    resources = ["${aws_s3_bucket.images.arn}/images/*"]
  }
}

resource "aws_iam_role_policy" "ec2_image_storage" {
  name   = "marketgo-image-storage"
  role   = var.ec2_instance_role_name
  policy = data.aws_iam_policy_document.ec2_image_storage.json
}
