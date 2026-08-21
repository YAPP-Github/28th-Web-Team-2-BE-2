INSERT INTO news_articles (title, summary, original_url, thumbnail_url, published_at)
VALUES
    (
        '충남 아산·홍성 동부에 폭염주의보 해제',
        '기상청이 21일 오전 아산과 홍성 동부의 폭염주의보를 해제했습니다. 논산·공주·부여·금산·청양에는 폭염주의보가 유지됩니다.',
        'https://www.yna.co.kr/amp/view/AKR20260821053200527',
        NULL,
        '2026-08-21T01:02:00Z'::TIMESTAMP WITH TIME ZONE
    ),
    (
        '“처서 매직 없다니 이게 무슨 날벼락”…주말 소나기 지나면 찜통더위 [주말 날씨]',
        '처서 주말에도 비와 소나기가 잠시 더위를 식히는 데 그치고, 다음 주까지 높은 기온과 습도가 이어질 전망입니다.',
        'https://m.mk.co.kr/amp/12133150',
        NULL,
        '2026-08-21T05:20:41Z'::TIMESTAMP WITH TIME ZONE
    ),
    (
        '폭염에도 사과값 안정…출하 20.5%↑·소매가 28.6%↓',
        '폭염과 가뭄에도 사과·배 수급 영향은 제한적이며, 사과 출하량 증가로 소매가격은 안정세를 보였습니다.',
        'https://m.newspim.com/news/view/20260820000935',
        NULL,
        '2026-08-20T00:00:00Z'::TIMESTAMP WITH TIME ZONE
    ),
    (
        '폭염에 채솟값 뛰고 가공식품도 줄인상…8월 장바구니 물가 부담',
        '폭염으로 시금치·배추·양배추·깻잎·상추 등 일부 채소의 소매가격이 상승했고, 애호박과 오이 가격도 올랐습니다.',
        'https://www.yna.co.kr/view/AKR20260812047400030',
        NULL,
        '2026-08-12T00:00:00Z'::TIMESTAMP WITH TIME ZONE
    ),
    (
        '배추·무 등 여름 채소 공급 안정…추석까지 가격 낮을 듯',
        '양호한 작황과 출하량 증가로 배추·무·양배추 가격이 평년보다 낮고, 큰 기상이변이 없으면 추석까지 공급이 안정될 전망입니다.',
        'https://www.imaeil.com/page/view/2026081113532545452',
        NULL,
        '2026-08-11T04:53:37Z'::TIMESTAMP WITH TIME ZONE
    )
ON CONFLICT (original_url) DO UPDATE
SET title = EXCLUDED.title,
    summary = EXCLUDED.summary,
    thumbnail_url = EXCLUDED.thumbnail_url,
    published_at = EXCLUDED.published_at;
