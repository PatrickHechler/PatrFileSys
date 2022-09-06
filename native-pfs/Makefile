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

BINARY = $(PROJEXT_ROOT)bin/$(BUILD_MODE)/

TEST_LIBS = -L $(BINARY) -l pfs

TARGET = $(BINARY)libpfs.a

TEST_TARGET = $(BINARY)pfs_tests

OBJS = $(BINARY)hashset.o $(BINARY)pfs.o $(BINARY)bm.o $(BINARY)pfs-element.o $(BINARY)pfs-folder.o $(BINARY)pfs-file.o

all:	INIT_MKDIR $(TEST_TARGET)
	$(TEST_TARGET)

INIT_MKDIR:
	mkdir -p $(BINARY)
	mkdir -p ./testout/

$(TEST_TARGET): $(TARGET) $(TEST_TARGET).o
	$(CC) -o $(TEST_TARGET) $(TEST_TARGET).o  $(TEST_LIBS)

$(TARGET):	$(OBJS)
	$(AR) -rc $@ $^

$(BINARY)%.o:	$(PROJECT_ROOT)%.cpp
	$(CXX) -c $(CFLAGS) $(CXXFLAGS) $(CPPFLAGS) -o $@ $<

$(BINARY)%.o:	$(PROJECT_ROOT)%.c
	$(CC) -x c -c $(CFLAGS) -o $@ $<

clean:
	rm -fr $(BINARY)
