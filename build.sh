#!/bin/sh

link=`readlink $HOME/m2`
echo $link
export M2_HOME=$HOME/$link
./bootstrap.sh
