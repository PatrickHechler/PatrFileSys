CONFIG_MODULE_SIG=n

obj-m += patrfs.o

patr_fs-y += super.o

all:
	make -C /lib/modules/$(shell uname -r)/build M=$(PWD) modules

clean:
	make -C /lib/modules/$(shell uname -r)/build M=$(PWD) clean
