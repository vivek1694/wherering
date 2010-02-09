package seanfoy.wherering.intent;

public final class IntentHelpers {
	public static <T extends Enum<T>> String fullname(T v) {
		return String.format("%s.%s", v.getClass().getName(), v);
	}
	public static <T extends Enum<T>> T parse(Class<T> token, String fullname) {
		String prefix = String.format("%s.", token.getClass().getName());
		return (T)Enum.valueOf(token, prefix.substring(prefix.length()));
	}
}
