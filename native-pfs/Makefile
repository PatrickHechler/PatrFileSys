PROJECT_ROOT = $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

all:
	make all --directory=$(PROJECT_ROOT)native-core
	rm -rfd $(PROJECT_ROOT)bin/

clean:
	make clean --directory=$(PROJECT_ROOT)native-core
	rm -rfd $(PROJECT_ROOT)bin/
