#!/usr/bin/env bash
set -euo pipefail

bucket="${AWS_S3_BUCKET:-marketgo-images}"
region="${AWS_REGION:-ap-northeast-2}"
base_url="https://${bucket}.s3.${region}.amazonaws.com/images/items/"
work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT

upload() {
  local item_name="$1"
  local object_name="$2"
  local source_url="$3"
  local file_path="$work_dir/$object_name"
  local content_type
  if [[ "$object_name" == *.png ]]; then
    content_type='image/png'
  else
    content_type='image/jpeg'
  fi
  curl --fail --location --retry 3 --output "$file_path" "$source_url"
  aws s3 cp "$file_path" "s3://${bucket}/images/items/${object_name}" \
    --region "$region" \
    --content-type "$content_type" \
    --cache-control 'public,max-age=31536000,immutable'
  printf '%s -> %s%s\n' "$item_name" "$base_url" "$object_name"
}

upload '감자' 'potato.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/515743752899172-0f32c471-74b9-4e09-ad7d-b42b3474a4ca.jpg'
upload '마늘' 'garlic.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/6332660849526376-a951ecdf-92d5-4051-ac71-f3f05c40572f.jpg'
upload '양파' 'onion.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/1729902560488915-32b55f7e-10fe-43de-bc99-c68510085f87.jpg'
upload '고구마' 'sweet-potato.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/2021/01/25/18/4/3dbb233d-2992-42aa-b217-2a505194686b.jpg'
upload '당근' 'carrot.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/444264645662121-38c31ce3-8ff2-4929-abac-8bbc123f5cab.jpg'
upload '토마토' 'tomato.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/712722502290473-7cd913a1-5d74-470c-aea5-e74f54190a8d.jpg'
upload '피망' 'bell-pepper.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/4324266110830-b849e7dc-91bc-476b-a805-218af5df764a.jpg'
upload '배추' 'napa-cabbage.png' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/6843631065462347-75b36d8f-5b10-4b8e-a674-cd6e481ce214.png'
upload '무' 'radish.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/1482132093671966-acb44d46-3b28-4e28-925c-292da96e7ce1.jpg'
upload '애호박' 'zucchini.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/438319943630087-5f42d558-1a49-4634-8dcc-cfe7ba66a482.jpg'
upload '쥬키니' 'zucchini-2.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/233964839980982-d756a853-8f92-4348-ab32-89f50af7a033.jpg'
upload '오이' 'cucumber.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/6535352251288574-8117bce5-48fe-4c48-bc43-acb56556ea01.jpg'
upload '방울토마토' 'cherry-tomato.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/5399994035114890-84e8e38a-207b-48b8-bd43-ae3d73663e77.jpg'
upload '대추방울토마토' 'cherry-tomato.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/5399994035114890-84e8e38a-207b-48b8-bd43-ae3d73663e77.jpg'
upload '대파' 'green-onion.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/4063320789843989-2f070b18-3886-4b8c-871b-2403227cfec9.jpg'
upload '쪽파' 'chives.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/4215368233178-d1de6af8-4daf-49ae-a225-d7061754098e.jpg'
upload '풋고추' 'green-chili.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/3921314003570119-af5c0f7f-3ffc-46d8-9a78-ab12fb8ac9e2.jpg'
upload '꽈리고추' 'shishito-pepper.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/1767035181951557-3acc97b5-b185-42c7-9d96-6be7bf21b6fd.jpg'
upload '청양고추' 'cheongyang-chili.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/4277012004459-c7710d73-f1c9-4fe3-bb5d-a9ca72f84383.jpg'
upload '오이맛고추' 'cucumber-chili.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/1025_amir_coupang_oct_80k/83e0/177a553c6269e3baeef81f9c89031bdbab433d2acda2f3031f271caeac95.jpg'
upload '느타리버섯' 'oyster-mushroom.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/671552929137457-2b8c0d22-203e-46e2-a754-8b0f80f16415.jpg'
upload '팽이버섯' 'enoki-mushroom.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/1053590534676745-37ef4d09-b0ed-490c-a9f7-47031b6159a5.jpg'
upload '새송이버섯' 'king-oyster-mushroom.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/162903088821133-741aa4ff-9cab-43c5-91bd-df55578650c5.jpg'
upload '참깨' 'sesame.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/2025/04/01/15/4/b1853ec3-0ae7-4e29-8dc2-6a57951e3f65.jpg'
upload '땅콩' 'peanut.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/85461402273457-492f47c0-ea0e-466c-8794-1a03651ede5f.jpg'
upload '양배추' 'cabbage.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/5788964362181-7f48acee-e7f2-4b33-8f61-acdfd2d8f9e3.jpg'
upload '시금치' 'spinach.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/29400661142120-ead107fa-4ecf-49de-8351-dce264a21edb.jpg'
upload '얼갈이배추' 'young-napa-cabbage.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/6668881996972716-40b003a9-01a5-40c4-a043-ac2986be2c53.jpg'
upload '열무' 'young-radish.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/495140014533826-a54354ca-95ac-4ce2-b9a9-b6500556b851.jpg'
upload '건고추' 'dried-chili.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/249775612090021-d0d64b32-9382-43fc-9ea5-6a7aec565267.jpg'
upload '붉은고추' 'red-chili.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/7642314554052243-8c0088a3-9b03-4f22-9103-c3533e5dc3ac.jpg'
upload '생강' 'ginger.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/99342143505448-9718627e-b451-4596-bf64-5df61a03a298.jpg'
upload '고춧가루-국산' 'red-pepper-powder-domestic.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/vendor_inventory/8ccf/0af57d8adcc17294862b563997a6a7426b18de0a586d35fd4aee14c1fe7f.jpg'
upload '고춧가루-중국산' 'red-pepper-powder-imported.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/25433175896988-e7771d12-c34e-4b58-a97d-2b561a6058eb.jpg'
upload '미나리' 'water-dropwort.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/1025_amir_coupang_oct_80k/8ea2/4d058ac7b3620590f6b016290513475e5781601c510c34a3070a7df85a7a.jpg'
upload '깻잎' 'perilla-leaf.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/4593735002888961-258b9513-9da0-43f5-831c-36015681e882.jpg'
upload '파프리카' 'paprika.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/1025_amir_coupang_oct_80k/b363/c2b2f63460cad72d11704674edab0d3c75439f5b3b8c6be264db5dea857f.jpg'
upload '멜론' 'melon.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/63381076845993-735176e2-af3c-4c04-8aab-2a149421da33.jpg'
upload '브로콜리' 'broccoli.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/84438627163531-704bd851-3a88-4969-b0be-8c37a6f040b8.jpg'
upload '알배기배추' 'baby-napa-cabbage.png' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/6843631065462347-75b36d8f-5b10-4b8e-a674-cd6e481ce214.png'
upload '수박' 'watermelon.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/3304058638725079-6c135b10-c9e2-43ed-a3ee-30955f2809fc.jpg'
upload '참외' 'korean-melon.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/rs_quotation_api/floyi07x/f01de370966f4d428d11d02c4b875894.jpg'
upload '딸기' 'strawberry.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/vendor_inventory/44f0/bd0d44ad88ef7769488f28ecd00205f9ca9d39a98a604ece5eef52bf0aec.jpg'
upload '적상추' 'red-lettuce.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/11950556430039-85f12635-236c-46c2-b8e4-2e016fe6a754.jpg'
upload '청상추' 'green-lettuce.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/1874101060238-2d0121d6-56d7-44fa-a12a-49a5fe3b1949.jpg'
upload '갓' 'mustard-leaf.jpg' 'https://thumbnail.coupangcdn.com/thumbnails/remote/657x657q90trim/image/retail/images/644101524424126-b7eada4c-8be2-4fd6-ab60-daf29534b2a0.jpg'
