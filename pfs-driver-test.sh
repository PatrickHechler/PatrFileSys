#!/bin/sh
# This file is part of the Patr File System Project
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
# Copyright (C) 2023  Patrick Hechler
# 
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.

set -e
if [ "$1" = "" ]
  then
	git pull
	cd ./pfs-driver
	make all
else
	cd ./pfs-driver
fi
echo 'add now Patr-FS driver'
sudo insmod patr_fs_driver.ko
sleep 1
echo 'remove now Patr-FS driver'
sudo rmmod patr_fs_driver.ko
