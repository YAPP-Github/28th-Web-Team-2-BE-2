package com.example.demo.external.qwen;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Qwen vision 실호출 smoke 테스트. `docs/adr/0002-qwen-vision-client.md` 의 미검증 가정을 확인한다.
 *
 * <p>레포의 다른 live smoke 테스트(11st·오아시스·GS샵)와 같은 방식이다 — 기본으로는 실행되지 않고
 * 시스템 프로퍼티를 켤 때만 돈다. CI 는 키가 없으므로 항상 skip 한다.
 *
 * <pre>
 * QWEN_API_KEY=... ./gradlew :external:qwen-client:test \
 *     --tests '*QwenVisionLiveSmokeTest*' -Dqwen.live=true --info
 * </pre>
 *
 * <p>이미지는 기본값으로 공개 샘플을 쓴다. 우리 버킷이 준비되면 실제 URL 로 바꿔 "DashScope 가 우리
 * S3 URL 을 가져올 수 있는가"까지 확인한다.
 *
 * <pre>
 * -Dqwen.live.imageUrl=https://&lt;bucket&gt;.s3.ap-northeast-2.amazonaws.com/images/&lt;uuid&gt;.jpg
 * </pre>
 *
 * <p>Feign 대신 JDK {@code HttpClient} 를 쓴다. 여기서 확인할 대상은 우리 배선이 아니라 <b>상대
 * 서버가 무엇을 받아들이는가</b> 이므로, Spring 컨텍스트 없이 와이어를 직접 보는 편이 실패 원인이
 * 좁혀진다. 대신 본문은 프로덕션과 같은 record·같은 ObjectMapper 로 만든다.
 */
@EnabledIfSystemProperty(named = "qwen.live", matches = "true")
class QwenVisionLiveSmokeTest {

    private static final String DEFAULT_IMAGE =
            "https://dashscope.oss-cn-beijing.aliyuncs.com/images/dog_and_girl.jpeg";
    /**
     * 기본값을 두지 않는다. 계정마다 host 가 다르고(workspace 전용 형태를 쓰는 계정이 있다) 틀린
     * host 로 돌리면 401·404 가 나서 "모델이 거부했다"로 오진하게 된다. 콘솔이 키와 함께 주는
     * {@code openAiCompatible} 값을 {@code -Dqwen.live.url} 로 넘긴다.
     */
    private static final String URL_PROPERTY = "qwen.live.url";
    private static final String DEFAULT_MODEL = "qwen-vl-plus";

    /** common.config.api.JacksonConfig 의 프로덕션 빈과 동일하다. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** ADR 가정 1·2·3·4·6 — response_format, temperature 0, endpoint, 모델 id, 응답 content 모양. */
    @Test
    void 프로덕션과_같은_요청_본문이_받아들여지고_JSON_본문이_돌아온다() throws Exception {
        final QwenChatRequest request = QwenChatRequest.jsonOnly(model(), List.of(
                QwenMessage.system("반드시 JSON 객체 하나만 출력한다. 설명·코드펜스를 쓰지 않는다."),
                QwenMessage.userWithImage(
                        "이 사진에 보이는 것을 {\"seen\": string} 형태 JSON 으로만 답해라.", imageUrl())));
        final String body = objectMapper.writeValueAsString(request);

        // 와이어 키가 snake_case 인지 먼저 확인한다 — 이게 틀리면 이미지가 전달되지 않는다.
        assertThat(body).contains("\"image_url\"", "\"response_format\"", "\"max_tokens\"");
        assertThat(body).doesNotContain("imageUrl", "responseFormat", "maxTokens");

        final HttpResponse<String> response = post(body);
        report("status", String.valueOf(response.statusCode()));
        report("body", abbreviate(response.body()));

        assertThat(response.statusCode())
                .withFailMessage("요청이 거부됐다. 본문:%n%s%n응답:%n%s", body, response.body())
                .isEqualTo(200);

        final QwenChatResponse parsed = objectMapper.readValue(response.body(), QwenChatResponse.class);
        final Optional<String> content = parsed.firstContent();
        report("finish_reason", finishReason(parsed));
        report("content", content.orElse("(없음)"));

        assertThat(content)
                .withFailMessage("본문을 꺼내지 못했다. message.content 가 문자열이 아닐 수 있다(ADR 가정 6).%n%s",
                        response.body())
                .isPresent();
        assertThat(objectMapper.readTree(content.get()).isObject())
                .withFailMessage("모델이 JSON 객체를 주지 않았다(ADR 가정 1). 받은 값:%n%s", content.get())
                .isTrue();
        assertThat(finishReason(parsed))
                .withFailMessage("출력이 잘렸다. max_tokens 를 늘려야 한다.")
                .isNotEqualTo("length");
        // 요청한 모델이 그대로 쓰였는지. 별도로 /models 를 부를 필요가 없다.
        assertThat(response.body()).contains("\"model\":\"" + model() + "\"");
    }

    /** ADR 가정 1 — response_format 을 뺀 요청과 비교해, 그 필드가 실제로 효과가 있는지 본다. */
    @Test
    void response_format_없이도_동작하는지_비교한다() throws Exception {
        final QwenChatRequest withoutFormat = new QwenChatRequest(
                model(),
                List.of(QwenMessage.userWithImage("이 사진에 보이는 것을 한 문장으로 말해라.", imageUrl())),
                0.0d,
                256,
                null);

        final HttpResponse<String> response = post(objectMapper.writeValueAsString(withoutFormat));
        report("response_format 없음 → status", String.valueOf(response.statusCode()));
        report("response_format 없음 → body", abbreviate(response.body()));

        // 여기서 200 이고 위 테스트가 400 이면 response_format 이 원인이다.
        assertThat(response.statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> post(final String body) throws Exception {
        return httpClient.send(
                HttpRequest.newBuilder(URI.create(baseUrl() + "/chat/completions"))
                        .header("Authorization", "Bearer " + apiKey())
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(60))
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String finishReason(final QwenChatResponse response) {
        if (response.choices() == null || response.choices().isEmpty()) {
            return "(없음)";
        }
        return String.valueOf(response.choices().getFirst().finishReason());
    }

    /**
     * 키는 환경변수로만 읽는다. 시스템 프로퍼티로 받으면 명령행과 셸 히스토리에 남는다.
     */
    private String apiKey() {
        final String key = System.getenv("QWEN_API_KEY");
        assertThat(key)
                .withFailMessage("QWEN_API_KEY 환경변수가 필요하다. -D 로 넘기지 말 것(히스토리에 남는다).")
                .isNotBlank();
        return key;
    }

    private String baseUrl() {
        final String url = System.getProperty(URL_PROPERTY);
        assertThat(url)
                .withFailMessage("-D%s 가 필요하다. 콘솔이 키와 함께 주는 openAiCompatible 값을 쓴다.",
                        URL_PROPERTY)
                .isNotBlank();
        return url;
    }

    private String model() {
        return System.getProperty("qwen.live.model", DEFAULT_MODEL);
    }

    private String imageUrl() {
        return System.getProperty("qwen.live.imageUrl", DEFAULT_IMAGE);
    }

    /** 결과를 눈으로 봐야 하는 테스트다. --info 없이도 보이게 stdout 에 찍는다. */
    private void report(final String label, final String value) {
        System.out.println("[qwen.live] " + label + " = " + value);
    }

    private String abbreviate(final String value) {
        if (value == null || value.length() <= 600) {
            return value;
        }
        return value.substring(0, 600) + "...(생략)";
    }
}
