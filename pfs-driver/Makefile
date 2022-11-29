ifneq ($(KERNELRELEASE),)
	obj-m := pfs-driver.o
else
	KERN_DIR ?= /usr/src/linux-headers-$(shell uname -r)/
	PWD := $(PWD)
default:
	$(MAKE) -C $(KERN_DIR) M=$(PWD) modules
endif

clean:
    sh -c 'rm -rf \*.o \*.ko \*.mod.c \*.order \*.symvers .\*.cmd .tmp_versions'
