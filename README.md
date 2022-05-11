# PatrFileSys
A File System
## elements
### flags
* folder
    * marks an element as a folder
* file
    * marks an element as a file
* link
    * marks an element as a link
    * links are also flags as folder or file. (like the link-target)
* read-only
    * marks an element as a read-only.
    * write operations on the given element will fail.
    * for folders: no elements can be added/removed
    * for files: no content can be written/appended
    * for links: the target can not be set
* executable
    * marks an element as a executable
* hidden
    * marks an element as a hidden
### name
### parent
### times
* create time
* last modify time
* last meta modify time
### lock
### folders
folders
### files
### links