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

public final class IntentHelpers {
	public static <T extends Enum<T>> String fullname(T v) {
		return String.format("%s.%s", v.getClass().getName(), v);
	}
	public static <T extends Enum<T>> T parse(Class<T> token, String fullname) {
		String prefix = String.format("%s.", token.getClass().getName());
		return (T)Enum.valueOf(token, prefix.substring(prefix.length()));
	}
}
