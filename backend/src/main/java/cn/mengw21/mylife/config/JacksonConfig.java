package cn.mengw21.mylife.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.Serial;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Jackson 序列化定制（对齐《02-API接口文档》1.3 / 1.4）
 * <ul>
 *     <li>业务日期 {@code LocalDate} → {@code 2026-11-06}</li>
 *     <li>时间戳 {@code LocalDateTime} → UTC ISO-8601 {@code 2026-11-06T10:30:00Z}</li>
 *     <li>ID 通过 VO 上 {@code @JsonSerialize(using = ToStringSerializer.class)} 输出字符串，
 *         避免 JavaScript 数字精度问题，本类不做全局改写</li>
 * </ul>
 * 只追加定制，不替换 Spring Boot 自动配置的 ObjectMapper，
 * 因此 {@code spring.jackson.*} 下的配置依然生效。
 */
@Configuration
public class JacksonConfig {

    private static final String DATE_PATTERN = "yyyy-MM-dd";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer javaTimeCustomizer() {
        return builder -> {
            builder.serializerByType(LocalDate.class, new LocalDateISOSerializer());
            builder.deserializerByType(LocalDate.class, new LocalDateISODeserializer());
            builder.serializerByType(LocalDateTime.class, new UtcLocalDateTimeSerializer());
            builder.deserializerByType(LocalDateTime.class, new UtcLocalDateTimeDeserializer());
        };
    }

    /** {@code LocalDate} → {@code yyyy-MM-dd} */
    private static final class LocalDateISOSerializer extends StdSerializer<LocalDate> {

        @Serial
        private static final long serialVersionUID = 1L;

        LocalDateISOSerializer() {
            super(LocalDate.class);
        }

        @Override
        public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeString(value == null ? null : value.format(DateTimeFormatter.ofPattern(DATE_PATTERN)));
        }
    }

    /** {@code LocalDate} ← {@code yyyy-MM-dd} */
    private static final class LocalDateISODeserializer extends StdDeserializer<LocalDate> {

        @Serial
        private static final long serialVersionUID = 1L;

        LocalDateISODeserializer() {
            super(LocalDate.class);
        }

        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            return text == null || text.isBlank() ? null : LocalDate.parse(text, DateTimeFormatter.ofPattern(DATE_PATTERN));
        }
    }

    /**
     * {@code LocalDateTime} → UTC ISO-8601 字符串 {@code yyyy-MM-dd'T'HH:mm:ss'Z'}
     * <p>
     * 约定：数据库存储的 LocalDateTime 一律为 UTC 值（见 MyMetaObjectHandler）。
     */
    private static final class UtcLocalDateTimeSerializer extends StdSerializer<LocalDateTime> {

        @Serial
        private static final long serialVersionUID = 1L;

        UtcLocalDateTimeSerializer() {
            super(LocalDateTime.class);
        }

        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value == null) {
                gen.writeNull();
                return;
            }
            gen.writeString(value.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
    }

    /**
     * {@code LocalDateTime} ← UTC ISO-8601（兼容 {@code Z} 与普通本地时间格式）
     */
    private static final class UtcLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {

        @Serial
        private static final long serialVersionUID = 1L;

        UtcLocalDateTimeDeserializer() {
            super(LocalDateTime.class);
        }

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            if (text == null || text.isBlank()) {
                return null;
            }
            try {
                // 带时区偏移（如 Z / +08:00）→ 统一换算成 UTC
                return OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .withOffsetSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime();
            } catch (DateTimeParseException e) {
                // 不带偏移 → 视为 UTC 本地时间直读
                return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        }
    }
}