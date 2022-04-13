package de.hechler.patrick.pfs.objects.java;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import de.hechler.patrick.pfs.interfaces.PatrFileSystem;
import de.hechler.patrick.pfs.objects.java.PFSPathImpl.Name;

public class PFSFileSystemImpl extends FileSystem {
	
	public static final String ATTR_VIEW_BASIC = "basic";
	
	private final PFSFileSystemProviderImpl provider;
	private final PatrFileSystem            fileSys;
	private volatile boolean                closed;
	
	public PFSFileSystemImpl(PFSFileSystemProviderImpl provider, PatrFileSystem fileSys) {
		this.provider = provider;
		this.fileSys = fileSys;
		this.closed = false;
	}
	
	
	public PatrFileSystem getFileSys() {
		return fileSys;
	}
	
	@Override
	public PFSFileSystemProviderImpl provider() {
		return provider;
	}
	
	@Override
	public void close() throws IOException {
		synchronized (fileSys) {
			if (closed) {
				return;
			}
			this.fileSys.close();
		}
	}
	
	@Override
	public boolean isOpen() {
		return !closed;
	}
	
	@Override
	public boolean isReadOnly() {
		return false;
	}
	
	@Override
	public String getSeparator() {
		return "/";
	}
	
	@Override
	public Iterable <Path> getRootDirectories() {
		return Arrays.asList((Path) new PFSPathImpl(this));
	}
	
	@Override
	public Iterable <FileStore> getFileStores() {
		return Arrays.asList((FileStore) new PFSFileStoreImpl(fileSys));
	}
	
	@Override
	public Set <String> supportedFileAttributeViews() {
		return new HashSet <>(Arrays.asList(ATTR_VIEW_BASIC));
	}
	
	@Override
	public Path getPath(String first, String... more) {
		List <Name> path = new ArrayList <>();
		add(path, first);
		for (String p : more) {
			add(path, p);
		}
		return new PFSPathImpl(this, new PFSPathImpl(this), path.toArray(new Name[path.size()]));
	}
	
	private void add(List <Name> path, String names) {
		String[] splitted = names.split("[\\/\\\\\\\0]");
		for (String part : splitted) {
			switch (part) {
			case ".":
				path.add(Name.NOTHING);
				break;
			case "..":
				path.add(Name.BACK);
				break;
			default:
				path.add(Name.create(part));
				break;
			}
		}
	}
	
	@Override
	public PathMatcher getPathMatcher(String syntaxAndPattern) {
		String[] strs = syntaxAndPattern.split("\\:", 2);
		switch (strs[0].toLowerCase()) {
		case "glob": {
			strs[1] = convertGlob(strs[1]);
		}
		case "regex":
			break;
		default:
			throw new IllegalArgumentException("Syntax '" + strs[0] + "' does not exist");
		}
		final Pattern pattern = Pattern.compile(strs[1], Pattern.DOTALL);
		return path -> pattern.matcher(path.toString()).matches();
	}
	
	/* @formatter:off
• The * character matches zero or more characters of a name component without crossing directory boundaries. 
• The ** characters matches zero or more characters crossing directory boundaries. 
• The ? character matches exactly one character of a name component.
• The backslash character (\) is used to escape characters that would otherwise be interpreted as special characters. The expression \\ matches a single backslash and "\{" matches a left brace for example. 
• The [ ] characters are a bracket expression that match a single character of a name component out of a set of characters.For example, [abc] matches "a", "b", or "c".The hyphen (-) may be used to specify a range so [a-z]
	specifies a range that matches from "a" to "z" (inclusive). These forms can be mixed so [abce-g] matches "a", "b", "c", "e", "f" or "g". If the character after the [ is a ! then it is used for negation so [!a-c] matches any 
	character except "a", "b", or "c".
	Within a bracket expression the '*', '?' and '\' characters match themselves. The '-' character matches itself if it is the first character within the brackets, or the first character after the '!' if negating.
• The  { } characters are a group of sub-patterns, where the group matches if any sub-pattern in the group matches. The "," character is used to separate the sub-patterns. Groups cannot be nested.
• Leading period/dot characters in file name are treated as regular characters in match operations. For example,the "*" glob pattern matches file name ".login".The Files.isHidden method may be used to test whether a file is considered hidden. 
• All other characters match themselves in an implementation dependent manner. This includes characters representing any name-separators. 
• The matching of root components is highly implementation-dependent and is not specified. 
				 @formatter:on */
	private String convertGlob(String glob) {
		char[] chars = Arrays.copyOf(glob.toCharArray(), glob.length() | (glob.length() >>> 1));
		int len = chars.length;
		final int state_none = 0, state_subRule = 1, state_choose = 2;
		int state = state_none;
		for (int i = 0; i < chars.length; i ++ ) {
			switch (chars[i]) {
			case '*':
				if (state != state_choose) {
					if (chars.length > i + 1 && chars[i + 1] == '*') {// '.*'
						chars[i] = '.';
						i ++ ;
					} else {// '[^\/\\\0]*'
						char[] add = "[^\\/\\\\\\\0]*".toCharArray();
						if (len + add.length > chars.length) {
							chars = Arrays.copyOf(chars, chars.length + add.length);
						}
						System.arraycopy(chars, i + 1, chars, i + add.length, add.length - 1);
						System.arraycopy(add, 0, chars, i, add.length);
						i += add.length - 1;
					}
				}
				break;
			case '?':
				if (state == state_none) {
					chars[i] = '.';
				}
				break;
			case '(':
			case ')':
				if (len + 1 > chars.length) {
					chars = Arrays.copyOf(chars, len + 1);
				}
				System.arraycopy(chars, i, chars, i + 1, len - i - 1);
				chars[i] = '\\';
				len ++ ;
				i ++ ;
				break;
			case '[':
				if (state == state_none) {
					state = state_choose;
					if (chars.length > i + 1 && chars[i + 1] == '!') {
						chars[ ++ i] = '^';
					}
				}
				break;
			case ']':
				if (state == state_choose) {
					state = state_none;
				}
				break;
			case '{':
				if (state == state_none) {
					chars[i] = '(';
					state = state_subRule;
				}
				break;
			case ',':
				if (state == state_subRule) {
					chars[i] = '|';
				}
				break;
			case '}':
				if (state == state_subRule) {
					state = state_none;
				}
			}
		}
		return String.valueOf(chars, 0, len);
	}
	
	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		throw new UnsupportedOperationException("getUserPrincipalLookupService");
	}
	
	@Override
	public WatchService newWatchService() throws IOException {
		throw new UnsupportedOperationException("newWatchService");
	}
	
}
