package de.hechler.patrick.pfs.element;

import de.hechler.patrick.pfs.exceptions.PatrFileSysException;
import de.hechler.patrick.pfs.folder.PFSFolder;

public interface PFSElement {

    /**
     * get the flags of the {@link PFSElement}
     * 
     * @return the flags of the {@link PFSElement}
     * @throws PatrFileSysException if an error occurs
     */
    int flags() throws PatrFileSysException;

    /**
     * modify the flags of the {@link PFSElement}
     * <p>
     * note that {@code addFlags} and {@code remFlags} are not allowed to contain
     * common bits (<code>addFlags & remFlags</code> must be {@code 0})<br>
     * also {@code addFlags} and {@code remFlags} are not allowed to contain
     * {@link #PFS_UNMODIFIABLE_FLAGS unmodifiable} bits
     * (<code>(addFlags | remFlags) & {@link #PFS_UNMODIFIABLE_FLAGS}</code> must be
     * {@code 0})<br>
     * 
     * @param addFlags the flags to add to this {@link PFSElement}
     * @param remFlags the flags to remove from this {@link PFSElement}
     * @throws PatrFileSysException if an error occurs
     */
    void modifyFlags(int addFlags, int remFlags) throws PatrFileSysException;

    String name() throws PatrFileSysException;

    void name(String newName) throws PatrFileSysException;

    long createTime() throws PatrFileSysException;

    void createTime(long ct) throws PatrFileSysException;

    long lastModTime() throws PatrFileSysException;

    void lastModTime(long lmt) throws PatrFileSysException;

    void delete() throws PatrFileSysException;

    PFSFolder parent() throws PatrFileSysException;

    void parent(PFSFolder newParent) throws PatrFileSysException;

    void move(PFSFolder newParent, String newName) throws PatrFileSysException;

    static int PFS_UNMODIFIABLE_FLAGS = 0x000000FF;
    static int PFS_FLAGS_FOLDER = 0x00000001;
    static int PFS_FLAGS_FILE = 0x00000002;
    static int PFS_FLAGS_PIPE = 0x00000004;
    static int PFS_FLAGS_FILE_EXECUTABLE = 0x00000100;
    static int PFS_FLAGS_FILE_ENCRYPTED = 0x00000200;
    static int PFS_FLAGS_HIDDEN = 0x01000000;

}
