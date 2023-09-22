#!/bin/sh
cd ~/git/PatrFileSys/pfs-driver
make all
sudo dmesg --follow &
echo 'add no patr-fs driver'
sudo insmod patr_fs_driver.ko
sleep 1
echo 'remove no patr-fs driver'
exec sudo rmmod patr_fs_driver.ko
