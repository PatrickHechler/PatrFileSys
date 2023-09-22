#!/bin/sh
cd ./pfs-driver
make all
sudo dmesg -C --follow &
echo 'add now Patr-FS driver'
sudo insmod patr_fs_driver.ko
sleep 1
echo 'remove now Patr-FS driver'
exec sudo rmmod patr_fs_driver.ko
