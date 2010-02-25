package seanfoy.wherering.intent;

public enum action {
    /**
     * Subscribe and periodically renew
     * subscriptions to proximity alerts
     */
	STARTUP,
	/**
	 * Cancel proximity subscriptions
	 * and don't renew them.
	 */
	SHUTDOWN,
	/**
	 * Set the ringer mode according to
	 * the user's notable places
	 * configuration
	 */
	PROXIMITY,
	/**
	 * Notify the user that the ringer
	 * mode has been changed by this
	 * app
	 */
	ALERT,
	/**
	 * Establish non-recurring proximity
	 * alerts according to the user's
	 * notable places configuration
	 */
	SUBSCRIBE
}
