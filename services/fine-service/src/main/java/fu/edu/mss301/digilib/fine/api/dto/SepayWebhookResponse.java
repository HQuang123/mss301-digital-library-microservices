package fu.edu.mss301.digilib.fine.api.dto;

public record SepayWebhookResponse(boolean success) {

    public static SepayWebhookResponse acknowledged() {
        return new SepayWebhookResponse(true);
    }
}
