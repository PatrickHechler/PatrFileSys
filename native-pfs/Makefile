PROJECT_ROOT = $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

all:
	echo make native core
	make all --directory=$(PROJECT_ROOT)native-core
	echo make jni
	rm -rfd $(PROJECT_ROOT)bin/
	make all --directory=$(PROJECT_ROOT)jni
	rm -rfd $(PROJECT_ROOT)bin/

clean:
	make clean --directory=$(PROJECT_ROOT)native-core
	make clean --directory=$(PROJECT_ROOT)jni
	rm -rfd $(PROJECT_ROOT)bin/
