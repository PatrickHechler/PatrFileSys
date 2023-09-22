#!/bin/sh
if [ $1 == "" ]
  then
	git pull
	cd ./pfs-driver
	make all
else
	cd ./pfs-driver
fi
sudo dmesg -C --follow &
echo 'add now Patr-FS driver'
sudo insmod patr_fs_driver.ko
sleep 1
echo 'remove now Patr-FS driver'
exec sudo rmmod patr_fs_driver.ko
