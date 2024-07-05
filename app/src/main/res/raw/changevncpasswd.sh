#!/bin/bash
cat $1 | vncpasswd -f > ~/.vnc/passwd
chmod 0600 -Rv ~/.vnc/passwd # 设置权限为 0600 才可以正常使用