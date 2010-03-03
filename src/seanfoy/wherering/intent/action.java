/*
 * Copyright 2010 Sean M. Foy
 * 
 * This file is part of WhereRing.
 *
 *  WhereRing is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  WhereRing is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with WhereRing.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package seanfoy.wherering.intent;

public enum action {
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
	SUBSCRIBE,
	/**
	 * Re-subscribe only if the WhereRing
	 * service is already active.
	 */
	SIGHUP
}
