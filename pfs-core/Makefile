PROJECT_ROOT = $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

ifeq ($(CC),)
	CC = gcc
endif
ifeq ($(AR),)
	AR = ar
endif

CFLAGS += -fPIC
LDFLAGS += -luuid

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

CORE_BIN = $(BIN)core/

API_BIN = $(BIN)api/

ROOT_SOURCE = $(PROJECT_ROOT)src/

INTSOURCE = $(ROOT_SOURCE)core/

APISOURCE = $(ROOT_SOURCE)api/

EXPSOURCE = $(ROOT_SOURCE)include/

EXP = $(PROJECT_ROOT)exports/

TARGET_A = $(CORE_BIN)libpfs-core.a

TARGET_SO = $(CORE_BIN)libpfs-core.so

CORE_TEST_TARGET = $(CORE_BIN)pfs_tests

CORE_OBJS = $(CORE_BIN)pfs.o $(CORE_BIN)pfs-element.o $(CORE_BIN)pfs-folder.o $(CORE_BIN)pfs-file.o

OBJS = $(CORE_OBJS) $(API_BIN)hashset.o $(API_BIN)bm.o $(API_BIN)pfs.o $(API_BIN)pfs-element.o $(API_BIN)pfs-folder.o $(API_BIN)pfs-file.o $(API_BIN)pfs-pipe.o $(API_BIN)pfs-stream.o $(API_BIN)pfs-iter.o

all:	INIT $(CORE_TEST_TARGET) $(TARGET_A) $(TARGET_SO)
	$(CORE_TEST_TARGET)
	cp -r -t $(EXP)shared/ $(TARGET_SO)
	cp -r -t $(EXP)static/ $(TARGET_A)

static: INIT $(TARGET_A)
	cp -r -t $(EXP)static/ $(TARGET_A)

shared: INIT $(TARGET_SO)
	cp -r -t $(EXP)shared/ $(TARGET_SO)

INIT_SO:
	mkdir -p $(EXP)shared/

INIT_A:
	mkdir -p $(EXP)static/

INIT:
	echo build mode: $(BUILD_MODE)
	mkdir -p $(CORE_BIN)
	mkdir -p $(API_BIN)
	mkdir -p $(PROJECT_ROOT)testout/fs-root/
	mkdir -p $(EXP)
	ln -sf -T $(EXPSOURCE) $(EXP)include

$(CORE_TEST_TARGET): $(CORE_TEST_TARGET).o $(CORE_OBJS) $(API_BIN)hashset.o $(API_BIN)bm.o
	$(CC) -o $(CORE_TEST_TARGET) $(LDFLAGS) $(CORE_TEST_TARGET).o $(CORE_OBJS) $(API_BIN)hashset.o $(API_BIN)bm.o

$(TARGET_SO):	INIT_SO $(OBJS)
	$(CC) -shared -o $@ $(LDFLAGS) $(OBJS)

$(TARGET_A):	INIT_A $(OBJS)
	$(AR) -rc $@ $(OBJS)

$(CORE_BIN)%.o:	$(INTSOURCE)%.c
	$(CC) -x c -c $(CFLAGS) -o $@ $<

$(API_BIN)%.o:	$(APISOURCE)%.c
	$(CC) -x c -c $(CFLAGS) -o $@ $<

clean:
	rm -frd $(PROJECT_ROOT)testout/ $(PROJECT_ROOT)exports/ $(BIN_ROOT)
