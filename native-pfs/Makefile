PROJECT_ROOT = $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

TARGET = libpfs.so

LDFLAGS = 

CC = gcc

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

OBJS = $(BINARY)pfs.o $(BINARY)bm.o $(BINARY)hashset.o

TARGET_WITH_PATH = $(BINARY)$(TARGET)

all:	$(TARGET)

$(TARGET):	$(OBJS)
	$(CC) -shared -o $(TARGET_WITH_PATH) $^ $(LDFLAGS)
# no mkdir needed, because final executable is in same binary folder like the object files

$(BINARY)%.o:	$(PROJECT_ROOT)%.cpp
	mkdir -p $(BINARY)
	$(CXX) -c $(CFLAGS) $(CXXFLAGS) $(CPPFLAGS) -o $@ $<

$(BINARY)%.o:	$(PROJECT_ROOT)%.c
	mkdir -p $(BINARY)
	$(CC) -x c -c $(CFLAGS) -o $@ $<

clean:
	rm -fr $(BINARY)
