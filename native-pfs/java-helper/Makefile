PROJECT_ROOT = $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

LDFLAGS = 

CC = gcc

CFLAGS = -fPIC -I /lib/jvm/java-18-openjdk/include/ -I /usr/lib/jvm/java-18-openjdk/include/linux/ -I

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

TARGET = $(BINARY)../libjava-helper.so

OBJS = $(BINARY)java-helper.o

all:	INIT $(TARGET)

INIT:
	mkdir -p $(BINARY)

$(TARGET):	$(OBJS)
	$(CC) -shared -o $@ $(LDFLAGS) $^

$(BINARY)%.o:	$(SOURCE)%.cpp
	$(CXX) -c $(CFLAGS) $(CXXFLAGS) $(CPPFLAGS) -o $@ $<

$(BINARY)%.o:	$(SOURCE)%.c
	$(CC) -x c -c $(CFLAGS) -o $@ $<

clean:
	rm -frd $(BINARY) $(PROJECT_ROOT)lib/
