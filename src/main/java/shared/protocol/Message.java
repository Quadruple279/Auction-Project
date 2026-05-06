package shared.protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class Message {
    public static final int PROTOCOL_VERSION = 1;
    //các field bắt buộc theo từng type - method valit sẽ kiểm tra trc khi gửi
    private static final Map<MessageType, List<String>> REQUIRED_FIELDS = Map.of(
            MessageType.LOGIN,          List.of("username", "password"),
            MessageType.REGISTER,       List.of("username", "password", "role"),
            MessageType.BID,            List.of("auctionId", "amount"),
            MessageType.SUBSCRIBE,      List.of("auctionId"),
            MessageType.UNSUBSCRIBE,    List.of("auctionId"),
            MessageType.LOGIN_FAILED,   List.of("reason"),
            MessageType.AUCTION_UPDATE, List.of("auctionId", "currentPrice", "leadingBidder", "eventType"),
            MessageType.ERROR,          List.of("reason")
    );
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(SerializationFeature.INDENT_OUTPUT);
    private int version = PROTOCOL_VERSION;
    private MessageType type;
    private Map<String, String> payload;
    // Constructor mặc định — Jackson cần để deserialize
    public Message() {
        this.payload = new HashMap<>();
    }

    public Message(MessageType type) {
        this.type = type;
        this.payload = new HashMap<>();
    }
    // ── Factory + Fluent API ─────────────────────────────────────────────────
    //tao 1 Msg moi voi type cho truoc
    public static Message of(MessageType type) {
        return new Message(type);
    }
    // them thong tin vao payload
    public Message put(String key, String value) {
        this.payload.put(key, value);
        return this;
    }
    // lay gia tri tu payload
    public String get(String key) {
        String value = payload.get(key);
        if (value == null) {
            throw new IllegalStateException(
                    "Payload thiếu field '" + key + "' trong message type " + type);
        }
        return value;
    }
    // lay gia tri va tra ve fallback neu khong co trong cac field tu chon
    public String getOrDefault(String key, String fallback) {
        return payload.getOrDefault(key, fallback);
    }
    // kiem tra payload co du thuoc tinh bat buoc khong?
    public Message validate() {
        List<String> required = REQUIRED_FIELDS.get(this.type);
        if (required == null) return this; // type không có ràng buộc

        for (String field : required) {
            String value = payload.get(field);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException(
                        "Message [" + type + "] thiếu field bắt buộc: '" + field + "'");
            }
        }
        return this;
    }
    //Chuyen Msg thanh chuoi Json(Serialize)
    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Không thể serialize Message: " + e.getMessage());
        }
    }
    // chuyen tu chuoi Json ve lai Object (Deserialize)
    public static Message fromJson(String json) {
        try {
            Message msg = MAPPER.readValue(json, Message.class);
            // Kiểm tra version để phát hiện client/server không tương thích
            if (msg.version != PROTOCOL_VERSION) {
                return Message.of(MessageType.ERROR)
                        .put("reason", "Protocol version mismatch: "
                                + "nhận " + msg.version
                                + ", yêu cầu " + PROTOCOL_VERSION);
            }
            return msg;
        } catch (JsonProcessingException e) {
            return Message.of(MessageType.ERROR)
                    .put("reason", "Parse failed: " + e.getOriginalMessage());
        }
    }
    // Getter-setter
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public Map<String, String> getPayload() { return payload; }
    public void setPayload(Map<String, String> payload) { this.payload = payload; }

    @Override
    public String toString() {
        return "Message{v=" + version + ", type=" + type + ", payload=" + payload + "}";
    }
}
