# 제보 사진 저장용 버킷.
#
# 계약은 `.agents/skills/image-upload-flow/SKILL.md`에 있다. 요지는 둘이다.
#   - key 는 `images/{UUID}.{png|jpg}`
#   - 영구 URL 은 `AWS_S3_BASE_URL + key` 이고 `images/` 접두사는 공개 읽기다
#
# 업로드는 서버 경유 단일 PUT 하나뿐이다. 그래서 두지 않는 것이 둘 있다.
#   - CORS: 브라우저가 S3 에 직접 PUT 하지 않는다.
#   - lifecycle(중단된 multipart 정리): 5MB 상한이 SDK 의 multipart 임계값보다 작아 multipart
#     업로드 자체가 생기지 않는다.
# 클라이언트 직접 업로드가 필요해지면 둘 다 함께 추가한다.

resource "aws_s3_bucket" "images" {
  bucket = var.image_bucket_name

  tags = local.common_tags
}

resource "aws_s3_bucket_public_access_block" "images" {
  bucket = aws_s3_bucket.images.id

  # ACL 은 계속 차단한다. 공개 읽기는 아래 bucket policy 로만 허용한다 — 어느 객체가 공개인지
  # 한 곳에서 읽히고, 객체마다 ACL 이 달라지는 상황이 생기지 않는다.
  block_public_acls       = true
  ignore_public_acls      = true
  block_public_policy     = false
  restrict_public_buckets = false
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
  }
}

# EC2 인스턴스 역할에 붙이는 권한.
#
# 백엔드는 access key 를 두지 않고 인스턴스 역할(IMDS)로 S3 에 붙는다. 업로드는 PutObject,
# 인식에 넘길 URL 검증은 객체를 읽지 않으므로 GetObject 는 향후 서버측 조회용이다.
data "aws_iam_policy_document" "ec2_image_storage" {
  statement {
    # key prefix 는 SKILL.md 의 images/ 규칙에 묶여 있다. 백엔드가 prefix 를 바꾸면
    # 여기도 함께 바꿔야 한다 — 안 바꾸면 컴파일·plan 은 통과하고 런타임 AccessDenied 만 난다.
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

# 평문 HTTP 접근 차단.
#
# presigned URL 이 유출되거나 클라이언트가 http 로 붙으면 사진과 SigV4 서명이 평문으로 흐른다.
# SDK 는 https 로 만들지만 버킷 차원에서 막을 수 있는 것을 열어 둘 이유가 없다.
# Deny 문장은 public grant 가 아니므로 block_public_policy 와 충돌하지 않는다.
data "aws_iam_policy_document" "images" {
  # 제보 사진 공개 읽기.
  #
  # 계약(SKILL.md)이 "영구 저장 URL"을 사용자 데이터에 저장하라고 정한다. 그 URL 로 사진이 보이려면
  # 객체를 직접 가져올 수 있어야 한다. 비공개로 두면 조회마다 presigned GET 을 발급해야 하고
  # URL 이 만료돼 브라우저·CDN 캐시가 무효가 된다.
  #
  # key 가 UUIDv4 라 추측할 수 없고 대상은 매장 가격표 사진이다. 범위를 images/ 접두사로 한정해
  # 버킷의 다른 경로가 함께 열리지 않게 한다.
  statement {
    sid       = "PublicReadReportImages"
    effect    = "Allow"
    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.images.arn}/images/*"]

    principals {
      type        = "*"
      identifiers = ["*"]
    }
  }

  statement {
    sid     = "DenyInsecureTransport"
    effect  = "Deny"
    actions = ["s3:*"]
    resources = [
      aws_s3_bucket.images.arn,
      "${aws_s3_bucket.images.arn}/*",
    ]

    principals {
      type        = "*"
      identifiers = ["*"]
    }

    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
  }
}

resource "aws_s3_bucket_policy" "images" {
  bucket = aws_s3_bucket.images.id
  policy = data.aws_iam_policy_document.images.json

  depends_on = [aws_s3_bucket_public_access_block.images]
}
