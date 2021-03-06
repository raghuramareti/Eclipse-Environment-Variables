package com.jorisaerts.eclipse.rcp.environment.environment;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.sun.jna.Library;
import com.sun.jna.Native;

public class Environment {

	public interface WinLibC extends Library {
		public int _putenv(String name);
	}

	public interface LinuxLibC extends Library {
		public int setenv(String name, String value, int overwrite);

		public int unsetenv(String name);
	}

	static public class POSIX {
		static Object libc;
		static {
			final String os = System.getProperty("os.name");
			if ("Linux".equals(os) || "Mac OS X".equals(os)) {
				libc = Native.loadLibrary("c", LinuxLibC.class);
			} else {
				libc = Native.loadLibrary("msvcrt", WinLibC.class);
			}
		}

		public int setenv(final String name, final String value, final int overwrite) {
			if (libc instanceof LinuxLibC) {
				return ((LinuxLibC) libc).setenv(name, value, overwrite);
			} else {
				return ((WinLibC) libc)._putenv(name + "=" + value);
			}
		}

		public int unsetenv(final String name) {
			if (libc instanceof LinuxLibC) {
				return ((LinuxLibC) libc).unsetenv(name);
			} else {
				return ((WinLibC) libc)._putenv(name + "=");
			}
		}
	}

	static POSIX libc = new POSIX();

	public static int unsetenv(final String name) {
		final Map<String, String> map = getenv();
		map.remove(name);
		final Map<String, String> env2 = getwinenv();
		env2.remove(name);
		return libc.unsetenv(name);
	}

	public static int setenv(final String name, final String value, final boolean overwrite) {
		if (name.lastIndexOf("=") != -1) {
			throw new IllegalArgumentException("Environment variable cannot contain '='");
		}
		final Map<String, String> map = getenv();
		final boolean contains = map.containsKey(name);
		if (!contains || overwrite) {
			map.put(name, value);
			final Map<String, String> env2 = getwinenv();
			env2.put(name, value);
		}
		return libc.setenv(name, value, overwrite ? 1 : 0);
	}

	@SuppressWarnings("unchecked") public static Map<String, String> getwinenv() {
		try {
			final Class<?> sc = Class.forName("java.lang.ProcessEnvironment");
			final Field caseinsensitive = sc.getDeclaredField("theCaseInsensitiveEnvironment");
			caseinsensitive.setAccessible(true);
			return (Map<String, String>) caseinsensitive.get(null);
		} catch (final Exception e) {
		}
		return new HashMap<String, String>();
	}

	@SuppressWarnings("unchecked") public static Map<String, String> getenv() {
		try {
			final Map<String, String> theUnmodifiableEnvironment = System.getenv();
			final Class<?> cu = theUnmodifiableEnvironment.getClass();
			final Field m = cu.getDeclaredField("m");
			m.setAccessible(true);
			return (Map<String, String>) m.get(theUnmodifiableEnvironment);
		} catch (final Exception ex2) {
		}
		return new HashMap<String, String>();
	}

}
