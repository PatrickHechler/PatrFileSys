package de.hechler.patrick.pfs.objects.bfs;

import java.lang.ref.WeakReference;

import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFolder;
import de.hechler.patrick.pfs.interfaces.PatrLink;
import de.hechler.patrick.pfs.objects.IntHashMap;

public class PatrFileSysElementBuffer {
	
	public final BufferedFileSysImpl                             fs;
	public PatrFileSysElement                              element;
	public WeakReference <PatrFileSysElementBuffer>              parent;
	public WeakReference <BufferedFileSysElementImpl>            me;
	public boolean                                               elementTypeChanged;
	public boolean                                               metaChanged;
	public boolean                                               dataChanged;
	public int                                                   type;
	public int                                                   owner;
	public long                                                  createTime;
	public long                                                  dataModTime;
	public long                                                  metaModTime;
	public long                                                  lockData;
	public long                                                  lockTime;
	public WeakReference <String>                                name;
	public WeakReference <PatrLink>                              link;
	public WeakReference <PatrFolder>                            folder;
	public WeakReference <PatrFile>                              file;
	public long                                                  length;
	public WeakReference <byte[]>                                hash;
	public IntHashMap <WeakReference <byte[]>>                   value;
	public IntHashMap <WeakReference <PatrFileSysElementBuffer>> childs;
	public int                                                   elementCount;
	public WeakReference <PatrFileSysElementBuffer>              target;
	
	public PatrFileSysElementBuffer(BufferedFileSysImpl fs, PatrFileSysElement element) {
		this.fs = fs;
		this.element = element;
	}
	
}
