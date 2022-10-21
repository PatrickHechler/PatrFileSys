PROJECT_ROOT = $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

PFS_LIB_FOLDER = $(PROJECT_ROOT)../pfs-core/exports/static/

PFS_LIB_FILE = $(PFS_LIB_FOLDER)libpfscore.a

LDFLAGS = -lreadline

CC = gcc

AR = ar

CFLAGS = -fPIC -I$(PROJECT_ROOT)../pfs-core/exports/include/

ifeq ($(BUILD_MODE),debug)
	CFLAGS += -g -Og
else ifeq ($(BUILD_MODE),run)
	CFLAGS += -Ofast -O10
else ifeq ($(BUILD_MODE),linuxtools)
	CFLAGS += -g -pg -fprofile-arcs -ftest-coverage
	LDFLAGS += -pg -fprofile-arcs -ftest-coverage
else
    $(error Build mode $(BUILD_MODE) not supported by this Makefile)
endif

BIN_ROOT = $(PROJECT_ROOT)binary/

BIN = $(BIN_ROOT)$(BUILD_MODE)/

SOURCE = $(PROJECT_ROOT)src/

TARGET = $(BIN)pfs-shell

OBJS = $(BIN)pfs-shell.o $(PFS_LIB_FILE)

all:	INIT $(TARGET)

INIT:
	echo build mode: $(BUILD_MODE)
	mkdir -p $(BIN)

$(TARGET): $(OBJS)
	$(CC) $(LDFLAGS) -o $(TARGET) $(OBJS)

$(PFS_LIB_FILE): 
	make -C $(PROJECT_ROOT)../pfs-core/ static

$(BIN)%.o:	$(SOURCE)%.c
	$(CC) -x c -c $(CFLAGS) -o $@ $<

clean:
	rm -frd $(BIN_ROOT)
