PROJECT_ROOT = $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

TARGET = libpfs.a

TEST_TARGET = pfs_tests

TEST_LIBS = -L $(BINARY) -l pfs

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

BINARY = $(PROJEXT_ROOT)bin/$(BUILD_MODE)/

OBJS = $(BINARY)pfs.o $(BINARY)bm.o $(BINARY)hashset.o $(BINARY)pfs-element.o $(BINARY)pfs-folder.o $(BINARY)pfs-file.o

TARGET_WITH_PATH = $(BINARY)$(TARGET)

TEST_TARGET_WITH_PATH = $(BINARY)$(TEST_TARGET)

all:	INIT_MKDIR $(TARGET_WITH_PATH) $(TEST_TARGET_WITH_PATH)
	$(BINARY)$(TEST_TARGET)

INIT_MKDIR:
	mkdir -p $(BINARY)

$(TARGET_WITH_PATH):	$(OBJS)
	$(AR) -rc $(TARGET_WITH_PATH) $^
# no mkdir needed, because final executable is in same binary folder like the object files

$(TEST_TARGET_WITH_PATH): $(TARGET_WITH_PATH) $(TEST_TARGET_WITH_PATH).o
	$(CC) -o $(TEST_TARGET_WITH_PATH) $(TEST_TARGET_WITH_PATH).o  $(TEST_LIBS)

$(BINARY)%.o:	$(PROJECT_ROOT)%.cpp
	$(CXX) -c $(CFLAGS) $(CXXFLAGS) $(CPPFLAGS) -o $@ $<

$(BINARY)%.o:	$(PROJECT_ROOT)%.c
	$(CC) -x c -c $(CFLAGS) -o $@ $<

clean:
	rm -fr $(BINARY)
