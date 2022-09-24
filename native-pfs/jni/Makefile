PROJECT_ROOT = $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

LIBS = -L $(PROJECT_ROOT)lib/ -l pfs

LDFLAGS = $(LIBS)

CC = gcc

IMPORT = $(PROJECT_ROOT)../native-core/exports/

CFLAGS = -fPIC -I /lib/jvm/java-17-openjdk/include/ -I /usr/lib/jvm/java-17-openjdk/include/linux/ -I $(IMPORT)include/

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

SOURCE = $(PROJECT_ROOT)src/

BINARY = $(PROJECT_ROOT)bin/$(BUILD_MODE)/

TARGET = $(BINARY)libjnipfs.so

OBJS = $(BINARY)jnipfs-fs.o $(BINARY)jnipfs-folder.o $(BINARY)jnipfs-file.o $(BINARY)jnipfs-pipe.o

all:	INIT $(TARGET)

INIT:
	mkdir -p $(BINARY)
	mkdir -p $(PROJECT_ROOT)lib/
	cp -t $(PROJECT_ROOT)lib/ $(IMPORT)libpfs.a

$(TARGET):	$(OBJS)
	$(CC) -shared -o $@ $(LDFLAGS) $^
	cp -T $(TARGET) $(PROJECT_ROOT)../../core/lib/libjnipfs.so

$(BINARY)%.o:	$(SOURCE)%.cpp
	$(CXX) -c $(CFLAGS) $(CXXFLAGS) $(CPPFLAGS) -o $@ $<

$(BINARY)%.o:	$(SOURCE)%.c
	$(CC) -x c -c $(CFLAGS) -o $@ $<

clean:
	rm -frd $(BINARY) $(PROJECT_ROOT)lib/
