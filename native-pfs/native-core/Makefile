PROJECT_ROOT = $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

LDFLAGS = 

CC = gcc

AR = ar

CFLAGS = -fPIC

ifeq ($(BUILD_MODE),debug)
	CFLAGS += -g -Og
else ifeq ($(BUILD_MODE),run)
	CFLAGS += -Ofast
else ifeq ($(BUILD_MODE),linuxtools)
	CFLAGS += -g -pg -fprofile-arcs -ftest-coverage
	LDFLAGS += -pg -fprofile-arcs -ftest-coverage
else
    $(error Build mode $(BUILD_MODE) not supported by this Makefile)
endif

BINARY = $(PROJECT_ROOT)bin/$(BUILD_MODE)/

SOURCE = $(PROJECT_ROOT)src/

EXP = $(PROJECT_ROOT)exports/

TEST_LIBS = -L $(BINARY) -l pfs

TARGET_A = $(BINARY)libpfs.a

TARGET_SO = $(BINARY)libpfs.so

TEST_TARGET = $(BINARY)pfs_tests

OBJS = $(BINARY)hashset.o $(BINARY)pfs.o $(BINARY)bm.o $(BINARY)pfs-element.o $(BINARY)pfs-folder.o $(BINARY)pfs-file.o

# headers with a .c file
HDRS = $(SOURCE)bm.h $(SOURCE)hashset.h $(SOURCE)pfs-element.h $(SOURCE)pfs-folder.h $(SOURCE)pfs-file.h $(SOURCE)pfs-pipe.h

# general headers without a .c file + HDRS
HEADERS = $(SOURCE)pfs.h $(SOURCE)pfs-err.h $(SOURCE)patr-file-sys.h $(SOURCE)pfs-constants.h

all:	INIT_MKDIR $(TEST_TARGET) $(TARGET_A) $(TARGET_SO)
	$(TEST_TARGET)
	cp -t $(EXP) $(TARGET_A) $(TARGET_SO)
	cp -t $(EXP)include/ $(HEADERS) $(HDRS)

INIT_MKDIR:
	mkdir -p $(BINARY)
	mkdir -p $(PROJECT_ROOT)testout/fs-root/

$(TEST_TARGET): $(TARGET_A) $(TEST_TARGET).o
	$(CC) -o $(TEST_TARGET) $(TEST_TARGET).o  $(TEST_LIBS)

$(TARGET_SO):	$(OBJS)
	$(CC) -shared -o $@ $(LDFLAGS) $^

$(TARGET_A):	$(OBJS)
	$(AR) -rc $@ $^

$(BINARY)%.o:	$(SOURCE)%.cpp
	$(CXX) -c $(CFLAGS) $(CXXFLAGS) $(CPPFLAGS) -o $@ $<

$(BINARY)%.o:	$(SOURCE)%.c
	$(CC) -x c -c $(CFLAGS) -o $@ $<

clean:
	rm -frd $(BINARY) ./testout/
