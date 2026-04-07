package unillm;

public final class ChatResponse {
	private final String provider;
	private final String model;
	private final String text;

	public ChatResponse(String provider, String model, String text) {
		this.provider = provider;
		this.model = model;
		this.text = text;
	}

	public String provider() {
		return provider;
	}

	public String model() {
		return model;
	}

	public String text() {
		return text;
	}
}
